/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.datatypes;

import ca.mcmaster.spcplexlibdec2016.solver.Solver;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.*;
import org.apache.log4j.Logger;


import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Parameters.*;
import ca.mcmaster.spcplexlibdec2016.solver.RampUpSolver;
import ca.mcmaster.spcplexlibdec2016.utilities.UtilityLibrary;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 *
 * @author tamvadss
 */
public class ActiveSubtree {
    
    private static Logger logger=Logger.getLogger(ActiveSubtree.class);
  
    //the CPLEX object representing this partially solved tree 
    private  IloCplex cplex ;
            
    //a solver object that is used to solve this tree few seconds at a time 
    private Solver solver ;   
    //also get a ramp up solver
    private RampUpSolver rampUpSolver ;
    
    //a flag to indicate if end() has been called on this sub tree
    //Use this method to deallocate memory once this subtree is no longer needed
    private boolean ended = false;

    //meta data about the IloCplex object
    public ActiveSubtreeMetaData subtreeMetaData  ; 
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtree.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
          
    }
        
    //Constructor
    public ActiveSubtree (  NodeAttachment attachment ) throws  Exception  {
        
        //initialize the CPLEX object
        cplex= new IloCplex();   
        cplex.importModel(SAV_FILENAME);
        
        //attachment.setDistanceFromSubtreeRoot(ZERO); //will be subtree root       -- not needed done in callbacks 
        
        UtilityLibrary.merge(cplex, attachment); 
        
        IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();        
        subtreeMetaData = new ActiveSubtreeMetaData(   attachment, lp.getNumVars());
        
        //get ourselves a solver
        solver = new Solver( cplex   , subtreeMetaData);
        
    }
    
   
    //solve for some time    
    public IloCplex.Status solve ( double timeSliceInSeconds,         
            double bestKnownLocalOptimum   , SolutionPhase solutionPhase  )
            throws  Exception {
        
        Instant startTime = Instant.now();
       
        if (solutionPhase.equals( SolutionPhase.NORMAL__SOLVE) ) 
            this.subtreeMetaData.numLeafsBeforeSolve_History.add(ZERO, this.subtreeMetaData.numActiveLeafs) ;
                 
        //solve for some time
        IloCplex.Status  status = solver.solve( timeSliceInSeconds, bestKnownLocalOptimum   , 
                solutionPhase);
        
        if (solutionPhase.equals( SolutionPhase.NORMAL__SOLVE) ) {
            double timeSlice = Math.abs(Duration.between(startTime, Instant.now() ).toMillis()/THOUSAND);
            //this.subtreeMetaData.timeSpentSolvingThisSubtreeInSeconds+=timeSlice; //timeSlice should be = timeSliceInSeconds

            this.subtreeMetaData.numLeafsAfterSolve_History.add( ZERO,this.subtreeMetaData.numActiveLeafs);
            this.subtreeMetaData.numLeafsCreated_History.add( ZERO,this.subtreeMetaData.numNodesBranchedUpon);
            double zeroDbl = ZERO;
            if (timeSlice>ZERO){
                this.subtreeMetaData.numNodesSolvedPerSecond_History.add(ZERO, zeroDbl+ (this.subtreeMetaData.numNodesBranchedUpon+
                                                                this.subtreeMetaData.numLeafsBeforeSolve_History.get(ZERO)-
                                                                this.subtreeMetaData.numActiveLeafs)/timeSlice);
            }else this.subtreeMetaData.numNodesSolvedPerSecond_History.add(ZERO,zeroDbl+ONE);//arbitrary estimate, should not happen
            
            this.estimateTimeToCompletionInSeconds();
        }
        
        return status;
        
    }
    
    public double getEstimatedTimeToCompletionInSeconds(){
        return this.subtreeMetaData.estimatedTimeToCompletionInSeconds;
    }
    
    //TODO - when farming, find estimated time to completion without this node , and include it in the 
    //nodes sent to load balancer only if plucking it will not leave the home partition starving
                    
    //do ramp up
    public IloCplex.Status rampUp (   )
            throws  Exception {
         
        //Instant startTime = Instant.now();
        
        //get ourselves a ramp up solver , note that this installs the approriate callbacks
        rampUpSolver = new RampUpSolver (  cplex ,   subtreeMetaData );
         
        //solve for some time
        IloCplex.Status  status = this.rampUpSolver.solve( );
        
        //this.subtreeMetaData.timeSpentSolvingThisSubtreeInSeconds+=Duration.between(startTime, Instant.now() ).toMillis()/THOUSAND;
        //not recording the ramp up time as time spent for solution of this subtree - we will start keeping time in the solution phase
        
        //now we must revert to the regular callbacks
        solver = new Solver( cplex   , subtreeMetaData);
        
        return status;
        
    }
    
    public int numFarmednodesAvailable() {
        return this.subtreeMetaData.farmedNodesMap.size();
    }
        
    public List<NodeAttachment> getFarmedNodesAfterRampUp () {
        List<NodeAttachment> nodes = new ArrayList<NodeAttachment>();
        while (this.subtreeMetaData.farmedNodesMap.size()>ZERO) {
            NodeAttachment node = (NodeAttachment) this.subtreeMetaData.farmedNodesMap.values().toArray()[ZERO];
            nodes.add(this.subtreeMetaData.farmedNodesMap.remove(node.getNodeid()));
        }
        return nodes;
    }
      
    //use this method to get meta data of farmed node, which is used to 
    //decide whether to pluck it out or not (maybe some other partition has a better migration candidate)
    //
    public NodeAttachmentMetadata inspectFarmedNode (  ) {
        NodeAttachment node = (NodeAttachment) this.subtreeMetaData.farmedNodesMap.values().toArray()[ZERO];
        return  node.getMetadataCopy();
        
    }
      
    public  NodeAttachment  pluckFarmedNode  () {
        //mark the node as plucked out
        NodeAttachment node = (NodeAttachment) this.subtreeMetaData.farmedNodesMap.values().toArray()[ZERO];
        this.subtreeMetaData.nodeIDsSelectedForMigration.add( node.getNodeid());
        this.subtreeMetaData.farmedNodesMap.remove(node.getNodeid());
        return  node ;
    }
    
    //call this method on trees where you do not take the candidate node
    public void clearFarmedNode() {
        this.subtreeMetaData.farmedNodesMap.clear();
    }
     
    
    //a bunch of methods follow, related to subtree solution value and status
           
    public boolean isSolvedToCompletion() throws Exception {
        return   this.isOptimal()||this.isInError()   ||this.isUnFeasible()||this.isUnbounded();
        
    }
    
    public boolean isInferior (double cutoff) throws IloException {
        return (getBestObjValue() <= cutoff && IS_MAXIMIZATION  ) || (getBestObjValue() >= cutoff && !IS_MAXIMIZATION  );
    }
        
    public String toString(){
        String details =this.subtreeMetaData.guid +NEWLINE;
        details += this.subtreeMetaData.rootNodeAttachment.toString();
        return details;
        
    }
    
    public double getBestObjValue () throws IloException {
        return this.cplex.getBestObjValue();
    }
    
    public Solution getSolution () throws IloException {
        Solution soln = new Solution () ;
        
        soln.setError(isInError());
        soln.setOptimal(isOptimal());
        soln.setFeasible(isFeasible() );
        soln.setUnbounded(isUnbounded());
        soln.setUnFeasible(isUnFeasible());
        
        soln.setOptimumValue(getObjectiveValue());
        
        if (isOptimalOrFeasible()) UtilityLibrary.addVariablevaluesToSolution(cplex, soln);
        
        return soln;
    }
    
        
    public boolean isFeasible () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Feasible) ;
    }
    
    public boolean isUnFeasible () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Infeasible) ;
    }
    
    public boolean isOptimal() throws IloException {
        
        return cplex.getStatus().equals(IloCplex.Status.Optimal) ;
    }
    public boolean isOptimalOrFeasible() throws IloException {
        return isOptimal()|| isFeasible();
    }
    public boolean isUnbounded() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Unbounded) ;
    }
    
    public boolean isInError() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Error) ;
    }
  
    public double getObjectiveValue() throws IloException {
        double inferiorObjective = IS_MAXIMIZATION?  MINUS_INFINITY:PLUS_INFINITY;
        return isFeasible() || isOptimal() ? cplex.getObjValue():inferiorObjective;
    }
        
    public String getStatusString () throws Exception{
        String status = "Unknown";
        if (isUnFeasible())   status =      "Infeasible";
        if (isFeasible()) status = "Feasible";
        if (isOptimal()) status = "optimal.";            
        if (isInError()) status = "error.";       
        if (isUnbounded()) status = "unbounded.";  
        if (this.isDiscardable()) status += " and also discardable.";  
        return status;
    }    
    
    public void end(){
        if (!ended) cplex.end();
        ended=true;
    }
    
    public boolean hasEnded(){
        return         ended ;
    }
    
    public ActiveSubtreeMetaData getActiveSubtreeMetaData(){
        return this.subtreeMetaData;
    }
    
    //methods for getting meta data information
        
    public boolean isDiscardable() {
        return this.subtreeMetaData.canDiscardEntireSubTree;
    }
    public void setDiscardable(boolean val) {
        this.subtreeMetaData.canDiscardEntireSubTree= val;
    }
    
    public IloNumVar[] getNumericVariables () {
        return this.subtreeMetaData.numericVariables;
    }

    public NodeAttachment getRootNodeAttachment(){
        return this.subtreeMetaData.rootNodeAttachment;
    }
    
    public String getGuid () {
        return this.subtreeMetaData.guid;
    } 
        
    private double forecastCreationNumber(   List<Long> numLeafsCreated_History ){
        double result = ZERO;
        if(numLeafsCreated_History.size()==ONE){
            //return what we have
            result = numLeafsCreated_History.get(ZERO);
        }else if(numLeafsCreated_History.size()>ONE){
            //arithmetic progression for 2 periods
            result = TWO*numLeafsCreated_History.get(ZERO)-numLeafsCreated_History.get(ONE);
        }
        return result<=ZERO?ZERO: result;
    }
    
    private double forescastSolutionrate( List<Double> numNodesSolvedPerSecond_History){
        double result = ZERO;
        
        if (numNodesSolvedPerSecond_History.size()==ONE){
            result = numNodesSolvedPerSecond_History.get(ZERO);
        }else if (numNodesSolvedPerSecond_History.size()>ONE) {
            //2 period geometric progression
            result = numNodesSolvedPerSecond_History.get(ZERO)*(numNodesSolvedPerSecond_History.get(ZERO)/numNodesSolvedPerSecond_History.get(ONE));
        }
         
        return result<=ZERO?ZERO: result;
    }
    
    //call this method only after a solve MAP cycle
    private void estimateTimeToCompletionInSeconds  (  ) {
        
        //#of nodes existing + #likely to be created, divided by estimated solution rate
        
        //creation rate and solution rate in the next cycle are calculated as geometric progressions
        long numNodesExisting = this.subtreeMetaData.numLeafsAfterSolve_History.get(ZERO);
        double numNodesLikelyToBeCreated = forecastCreationNumber(this.subtreeMetaData.numLeafsCreated_History) ;
        double forecastedSolutionrate = forescastSolutionrate(this.subtreeMetaData.numNodesSolvedPerSecond_History);
        
        this.subtreeMetaData.estimatedTimeToCompletionInSeconds =(numNodesLikelyToBeCreated+numNodesExisting)/forecastedSolutionrate;
        
    }
}
