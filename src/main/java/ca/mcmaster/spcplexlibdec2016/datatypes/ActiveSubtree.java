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
    public ActiveSubtree (  NodeAttachment attachment) throws  Exception  {
        
        //initialize the CPLEX object
        cplex= new IloCplex();   
        cplex.importModel(SAV_FILENAME);
        
        //attachment.setDistanceFromSubtreeRoot(ZERO); //will be subtree root       -- not needed done in callbacks 
        
        UtilityLibrary.merge(cplex, attachment); 
        
        IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();        
        subtreeMetaData = new ActiveSubtreeMetaData(   attachment, lp.getNumVars());
        
        //get ourselves a solver
        solver = new Solver( cplex   , subtreeMetaData);
        rampUpSolver = new RampUpSolver (  cplex ,   subtreeMetaData );
    }
    
    //solve for some time    
    public IloCplex.Status solve ( double timeSliceInSeconds,         
            double bestKnownOptimum   , SolutionPhase solutionPhase  )
            throws  Exception {
         
        //solve for some time
        IloCplex.Status  status = solver.solve( timeSliceInSeconds, bestKnownOptimum   , 
                solutionPhase);
        
        return status;
        
    }
            
    //do ramp up
    public IloCplex.Status rampUp (   )
            throws  Exception {
         
        //solve for some time
        IloCplex.Status  status = this.rampUpSolver.solve( );
        
        return status;
        
    }
    
    public int numFarmednodesAvailable() {
        return this.subtreeMetaData.farmedNodesMap.size();
    }
        
    public List<NodeAttachment> getFarmedNodesAfterRampUp () {
        return new ArrayList ( this.subtreeMetaData.farmedNodesMap.values());
    }
      
    //use this method to get meta data of farmed node, which is used to 
    //decide whether to pluck it out or not (maybe some other partition has a better migration candidate)
    //
    //returns node ID of available node, and available node is populated into nodeAttachmentMetadata
    public String inspectFarmedNode (NodeAttachmentMetadata nodeAttachmentMetadata) {
        nodeAttachmentMetadata= (NodeAttachmentMetadata) this.subtreeMetaData.farmedNodesMap.values().toArray()[ZERO];
        return (String) this.subtreeMetaData.farmedNodesMap.keySet().toArray()[ZERO];
    }
      
    public  NodeAttachment  getFarmedNode  () {
        //mark the node as plucked out
        this.subtreeMetaData.nodeIDsSelectedForMigration.add( (String) this.subtreeMetaData.farmedNodesMap.keySet().toArray()[ZERO]);
        return   (NodeAttachment) this.subtreeMetaData.farmedNodesMap.values().toArray()[ZERO];
    }
    
    //sometimes, after farming we find that the farmed node is below the global cutoff
    public void discardInferiorFarmedNode (double globalCutoff){
        NodeAttachmentMetadata nodeAttachmentMetadata = new NodeAttachmentMetadata ();
        inspectFarmedNode (nodeAttachmentMetadata);
        if ((nodeAttachmentMetadata.lpRelaxValue    >= globalCutoff && !IS_MAXIMIZATION  ) || 
            (nodeAttachmentMetadata.lpRelaxValue    <= globalCutoff &&  IS_MAXIMIZATION  )){
            //mark it as selected for migration, equivalently get the farmed node but discard it
             getFarmedNode  ();
        }
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
}
