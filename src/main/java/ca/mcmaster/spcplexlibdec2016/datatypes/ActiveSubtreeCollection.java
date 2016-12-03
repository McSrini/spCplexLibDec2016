/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.datatypes;
 
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*; 

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Constants.SolutionPhase.DO_FARMING;
import static ca.mcmaster.spcplexlibdec2016.Constants.SolutionPhase.NORMAL__SOLVE;
import static ca.mcmaster.spcplexlibdec2016.Parameters.*;


/**
 *
 * @author tamvadss
 */
public class ActiveSubtreeCollection {
    
    private static Logger logger=Logger.getLogger(ActiveSubtreeCollection.class);
     
    
    //list of subtrees
    private List <ActiveSubtree> activeSubtreeList = new ArrayList<ActiveSubtree> ();
    //list of node attachments not yet converted into active sub-trees
    private List <NodeAttachment> rawNodeList = new ArrayList<NodeAttachment> ();
    
    //here is the cursor to the tree being solved right now, in the activeSubtreeList
    private int cursorToTreeBeingSolved = -ONE;
    
    //best known LocalSolution, i.e best on this partition
    private Solution bestKnownLocalSolution  = new Solution () ;
    private double bestKnownLocalOptimum =bestKnownLocalSolution.getObjectiveValue();
        
    //for statistics
    public double totalTimeAllocatedForSolving = ZERO;
    public double totalTimeUsedForSolving = ZERO;
     
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtreeCollection.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
            logger.debug("ActiveSubtreeCollection Version 1.0");
        } catch (IOException ex) {
            ///
        }
          
    }
    
    //Constructor
    public ActiveSubtreeCollection (  List<NodeAttachment> attachmentList) throws  Exception  {
        for (NodeAttachment node : attachmentList){
            rawNodeList.add( node );
        }
        
    }
    
    public ActiveSubtreeCollection (   NodeAttachment  node) throws  Exception  {
         
        rawNodeList.add( node );         
        
    }
        
    public ActiveSubtreeCollection (   ) throws  Exception  {
        
    }
    
    public void add (  NodeAttachment attachment ) throws  Exception  {
         rawNodeList.add( attachment) ;
    }
    
    public void add (  List<NodeAttachment> attachmentList) throws  Exception  {
        for (NodeAttachment node : attachmentList){
            rawNodeList.add( node );
        }
        
    }
        
    public  Solution getSolution (){
        return    bestKnownLocalSolution ;
    }
    
    //before beginning a map cycle, cull inferior trees
    public void cullInferiorTrees (double globalCutoff ) throws Exception{
        List <Integer> positionsToCull = new ArrayList <Integer> ();
        for (int index = ZERO; index <activeSubtreeList.size(); index ++ ){
            if (     activeSubtreeList.get(index).isDiscardable()|| 
                     activeSubtreeList.get(index).isSolvedToCompletion() ||
                     activeSubtreeList.get(index).isInferior( globalCutoff)   ) positionsToCull.add(index);
        }
        
        //get indices in decreasing order
        Collections.reverse(  positionsToCull);
                 
        for (int index: positionsToCull){
            ActiveSubtree removedTree= activeSubtreeList.remove(index);
            removedTree.end();
              
        }        
        
    }
   
    //before beginning a map cycle, cull inferior raw nodes
    public  void  cullInferiorRawNodes (double globalCutoff) throws IloException {
         
        List <Integer> positionsToCull = new ArrayList <Integer> ();
        
        for (int index = ZERO; index <this.rawNodeList.size(); index ++ ){
            if ( rawNodeList.get(index).getLPrelax() <= globalCutoff && IS_MAXIMIZATION  ) positionsToCull.add(index);
            if ( rawNodeList.get(index).getLPrelax() >= globalCutoff && !IS_MAXIMIZATION  ) positionsToCull.add(index);
        }
        
        //get indices in decreasing order
        Collections.reverse(  positionsToCull);
        
        for (int index: positionsToCull){
           rawNodeList.remove(index);
        }
        
    }
    
    //driver needs a way to inspect each active subtree for available farmed nodes
    //here is an inelegant way of allowing that
    public List <ActiveSubtree> getActiveSubtreeList () {
        return this.activeSubtreeList;
    }
    
    public Solution rampUp  (  int mapIteratioNumber, int partitionNumber) throws Exception{
        
        logger.info(" Ramp up - iteration "+ mapIteratioNumber + " , subtree collection " + partitionNumber );
                
        //solve the only tree in this collection till enough nodes have been farmed out
        getIndexOfTreeToSolve();
        ActiveSubtree subtree = activeSubtreeList.get(cursorToTreeBeingSolved);
        subtree.rampUp();
        
        Solution subTreeSolution = subtree.getSolution() ;
        if ( ZERO != (new SolutionComparator()).compare(bestKnownLocalSolution, subTreeSolution)){
            //we have found a better solution

            //update our copies
            bestKnownLocalSolution = subTreeSolution;                
            bestKnownLocalOptimum = subTreeSolution.getObjectiveValue();
            logger.info("bestKnownLocalOptimum updated to "+bestKnownLocalOptimum );

        }
        
        logger.debug("Tree has this many active leafs after solving " +    subtree.getActiveSubtreeMetaData().numActiveLeafs) ;
        return bestKnownLocalSolution;
    }
    
    public void doFarming  (  int mapIteratioNumber, int partitionNumber) throws Exception{
        
        //order every active subtree to produce 1 candidate node for farming
        logger.info(" Doing Farming - map iteration "+ mapIteratioNumber + " , subtree collection " + partitionNumber );
        
        for (int treeIndex = ZERO;treeIndex < this.activeSubtreeList.size();  treeIndex++){
            //allow more than ten minutes to farm one node :)
            this.solveTree(activeSubtreeList.get( treeIndex), TEN*HUNDRED,  DO_FARMING);
        }
        
    }
                  
    
    //Solve an active sub-tree selected by the tree selection strategy
    public Solution solve  ( Instant endTimeOnWorkerMachine,         
            Solution bestKnownGlobalSolution , int mapIteratioNumber, int partitionNumber) throws Exception{
                
        logger.info(" iteration "+ mapIteratioNumber + " , subtree collection " + partitionNumber +
                " , solve Started at  " + Instant.now() + " will end at "+ endTimeOnWorkerMachine);
         
        double timeSliceForPartition = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
        this.totalTimeAllocatedForSolving += timeSliceForPartition;
               
        //update the local  copy of solution on this partition
        bestKnownLocalSolution=bestKnownGlobalSolution;
        this.bestKnownLocalOptimum = bestKnownGlobalSolution.getObjectiveValue();
        
        //make several passes thru the list of trees in this partition, till time runs out or all trees solved
        for (int pass = ZERO;  ! isHaltFilePresent() ;pass++) {
            
            logger.info(" starting pass  "+pass);
              
            int numTreesLeft = this.activeSubtreeList.size();
            int numRawNodesLeft = this.rawNodeList.size();
            if (numTreesLeft  + numRawNodesLeft== ZERO) break;            
            // exit in case of error or unbounded
            if (bestKnownLocalSolution.isError() ||bestKnownLocalSolution.isUnbounded())  break;
                    
            //check if time expired, or no work-items left to solve
            double wallClockTimeLeft = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
            //do not solve unless at least a few seconds left
            if ( Math.round(wallClockTimeLeft/numTreesLeft)<= ONE /*seconds*/  )  break;                

            //pick a tree to solve and solve it for MIN_SOLUTION_TIME_SLICE_IN_SECONDS   
            getIndexOfTreeToSolve();            
            
            if (this.cursorToTreeBeingSolved >=ZERO) {
                //solve it for time slice
                ActiveSubtree subtree = activeSubtreeList.get(cursorToTreeBeingSolved);
                
                logger.debug(" Selected this tree for solving "+subtree.getActiveSubtreeMetaData().guid + 
                        " tree has this many active leafs " +
                        subtree.getActiveSubtreeMetaData().numActiveLeafs);                 
                
                logger.debug("Solve this tree for   "+(int)Math.round(wallClockTimeLeft/this.activeSubtreeList.size()));
                boolean betterSolutionFound = solveTree(  subtree, (int)Math.round(wallClockTimeLeft/ numTreesLeft) , NORMAL__SOLVE );   
                
                if ( subtree.isDiscardable() || subtree.isSolvedToCompletion() || subtree.isInferior( bestKnownLocalOptimum) ) {
                    if (subtree.isDiscardable()) logger.debug(" tree discarded "+subtree.getActiveSubtreeMetaData().guid);
                    if (subtree.isSolvedToCompletion()) logger.debug(" tree is Solved To Completion "+
                            subtree.getActiveSubtreeMetaData().guid);
                    subtree.end();
                    activeSubtreeList.remove(cursorToTreeBeingSolved);
                } 
                
                //cull inferior trees and raw nodes in case better solution has been found
                if (betterSolutionFound){
                    this.cullInferiorRawNodes(bestKnownLocalOptimum );
                    this.cullInferiorTrees(bestKnownLocalOptimum); 
                }
                    
            } else {
                //nothing left to solve
                break;
            }
        }
        
        double timeWasted = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
        this.totalTimeUsedForSolving +=(timeSliceForPartition-timeWasted);
        
        logger.info(" iteration "+ mapIteratioNumber + " ,subtree collection " + partitionNumber +" ,solve Ended at  " + Instant.now());
        
        return this.bestKnownLocalSolution;
        
    }//end solve
  
    
    private boolean solveTree(ActiveSubtree subtree , int  timeSliceInSeconds,    SolutionPhase solutionPhase ) throws Exception{
        
        boolean hasBetterSolutionBeenFound = false;
        
        logger.debug(" solving "+subtree.getActiveSubtreeMetaData().guid);
         
        subtree.solve(timeSliceInSeconds , bestKnownLocalOptimum,    solutionPhase);
        
        Solution subTreeSolution = subtree.getSolution() ;
        if ( ZERO != (new SolutionComparator()).compare(bestKnownLocalSolution, subTreeSolution)){
            //we have found a better solution

            //update our copies
            bestKnownLocalSolution = subTreeSolution;                
            bestKnownLocalOptimum = subTreeSolution.getObjectiveValue();
            logger.info("bestKnownLocalOptimum updated to "+bestKnownLocalOptimum );

            hasBetterSolutionBeenFound= true;            

        }
        
        logger.debug("Tree has this many active leafs after solving " + 
                subtree.getActiveSubtreeMetaData().numActiveLeafs) ;
        
        return hasBetterSolutionBeenFound;
    }
  
    
    //pick a tree to solve
    private int getIndexOfTreeToSolve () throws  Exception{
        
        //first convert any raw nodes to trees, subjetc to tree limit
        int numRawNodes = this.rawNodeList.size();
        int numTrees = this.activeSubtreeList.size();
        while  ( ( numRawNodes>ZERO) && (numTrees < MAX_ACTIVE_SUBTREES_PER_PARTITION) ){
            //pick the first raw node in the list 
            // promote it to tree  
            activeSubtreeList.add(new ActiveSubtree(this.rawNodeList.remove(ZERO)));
            numRawNodes = this.rawNodeList.size();
            numTrees = this.activeSubtreeList.size();
        }
        
        if (numTrees>ZERO) {
            //advance the cursor to the next tree in the tree list
            this.cursorToTreeBeingSolved = (cursorToTreeBeingSolved+ONE)%MAX_ACTIVE_SUBTREES_PER_PARTITION;
        }else {
             //no work left, solve nothing
             cursorToTreeBeingSolved= -ONE;
        }
        
        return cursorToTreeBeingSolved;
    }
            
    private static boolean isHaltFilePresent (){
        File file = new File(HALT_FILE);
        return file.exists();
    }
 
}
