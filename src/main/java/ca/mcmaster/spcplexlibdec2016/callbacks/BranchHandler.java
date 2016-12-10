/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.callbacks;

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Constants.SolutionPhase.DO_FARMING;
import static ca.mcmaster.spcplexlibdec2016.Parameters.*;
import ca.mcmaster.spcplexlibdec2016.datatypes.*;
import ca.mcmaster.spcplexlibdec2016.utilities.UtilityLibrary;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * handle branching during regular solve - in this case prune inferior parts of tree
 * Also used to farm migration candidates
 * 
 * updates number of leaf nodes left in tree for statistics
 * 
 * 
 */
public class BranchHandler extends BaseBranchhandler {
    
    private static Logger logger=Logger.getLogger(BranchHandler.class);
        
    //best known optimum is used to prune nodes
    private double bestKnownOptimum;
    
    //solution phase indicates whether we are farming,  or just solving
    private SolutionPhase solutionPhase ;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+
                    BranchHandler.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
             
        } catch (IOException ex) {
            ///
        }
          
    }
        
    public BranchHandler (ActiveSubtreeMetaData metaData ) {
        this.  subtreeMetaData= metaData;    
         
    }
    
    public void refresh( double bestKnownOptimum, SolutionPhase solutionPhase) {
        this.bestKnownOptimum=bestKnownOptimum;
        this.solutionPhase = solutionPhase;
    } 

    /**
     * discard inferior nodes, or entire trees
     * Otherwise branch the 2 kids and accumulate branching variable bound information
     *   
     */
    protected void main() throws IloException {
       
        if ( getNbranches()> ZERO ){  
            
            //tree is branching
            this.subtreeMetaData.numNodesBranchedUpon ++;
            
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
                NodeAttachment subTreeRoot = subtreeMetaData.rootNodeAttachment;
                nodeData=new NodeAttachment (  
                        subTreeRoot.getBranchingVariableUpperBounds(),
                        subTreeRoot.getBranchingVariableLowerBounds(),  
                        subTreeRoot.getMetadataCopy().distanceFromOriginalRoot,
                        ZERO );  
                setNodeData(nodeData);
            } 
            nodeData.setLPrelax(getObjValue());
            
            //first check if entire tree can be discarded
            if (canTreeBeDiscarded()    ){
                
                //no point solving this tree any longer 
                //if (  isLoggingInitialized) logger.info( this.treeMetaData.getGUID()+         " tree is getting discarded "); 
                subtreeMetaData.canDiscardEntireSubTree = true;
                abort();
                //logger.debug("tree disarded " +this.treeMetaData.getGUID());
                
            } else  /*check if this node can be discarded*/ if (canNodeBeDiscarded( getObjValue())  && !isSubtreeRoot() ) {               
                // this node is useless
                //if (  isLoggingInitialized) logger.info( this.treeMetaData.getGUID()+         " tree is pruning inferior node "+         ((NodeAttachment)getNodeData()).nodeMetadata.nodeID); 
                prune();  
                  
                //logger.debug("node discarded from tree "+this.treeMetaData.getGUID());
                
            //we will now have several cases:
            //
            //     CASES:       
            //1) when we hit a migrated node
            //2) regular solve  
            //3) farm a node
                
            } else   if (                  subtreeMetaData.nodeIDsSelectedForMigration.size()>ZERO &&
                                         subtreeMetaData.nodeIDsSelectedForMigration.contains( nodeData.getMetadataCopy().nodeID)
                      ) {
                             
                               
                //prune  node which has been migrated away
                prune();    
                subtreeMetaData.nodeIDsSelectedForMigration.remove(nodeData.getNodeid());
                                
            }  else   if (   DO_FARMING.equals(solutionPhase ) && !isSubtreeRoot()  ) { //never farm subtree root
                
                //farm out node  , do not prune this node from tree just yet               
                this.subtreeMetaData.farmedNodesMap.put(nodeData .getNodeid(),nodeData  );
                                
                //abort right after setting the migration candidate
                abort();
                
            } else    {
                
                // we  create  2 kids - this is branching during regular solve
                createTwoChildNodes(  nodeData);
                                  
                                
            } //and if else
            
            this.subtreeMetaData.numActiveLeafs= getNremainingNodes64();
            
        }//end getNbranches()> ZERO
    } //end main
    
    private boolean canNodeBeDiscarded (double lpRelaxVal) throws IloException {
        boolean result = false;
        
        result = IS_MAXIMIZATION  ? 
                    (lpRelaxVal< getCutoff()) || (lpRelaxVal <= bestKnownOptimum )  : 
                    (lpRelaxVal> getCutoff()) || (lpRelaxVal >= bestKnownOptimum );

       
        return result;
    }
    
    //can this ILOCLPEX object  be discarded ?
    private boolean canTreeBeDiscarded(  ) throws IloException{     
        
        double bestIntegerFeasibleObjectiveValue = ZERO;
        if ( IS_MAXIMIZATION)  {
            bestIntegerFeasibleObjectiveValue = Math.max(bestKnownOptimum,  getIncumbentObjValue());
        }else {
            bestIntegerFeasibleObjectiveValue = Math.min(bestKnownOptimum,  getIncumbentObjValue());
        }
         
        //|bestnode-bestinteger|/(1e-10+|bestinteger|) 
        //(bestinteger - bestobjective) / (1e-10 + |bestobjective|)
        double relativeMIPGap =  getBestObjValue() - bestIntegerFeasibleObjectiveValue ;        
        if ( IS_MAXIMIZATION)  {
            relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestIntegerFeasibleObjectiveValue  ));
        } else {
            relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(getBestObjValue() ));
        }
        

        boolean mipGapHaltCondition =  RELATIVE_MIP_GAP >= Math.abs(relativeMIPGap)  ;
        
        //also halt if we cannot do better than best Known Optimum
        boolean inferiorityHaltConditionMax = IS_MAXIMIZATION  && (bestKnownOptimum>=getBestObjValue());
        boolean inferiorityHaltConditionMin = !IS_MAXIMIZATION && (bestKnownOptimum<=getBestObjValue());
         
  
        return   mipGapHaltCondition ||  inferiorityHaltConditionMin|| inferiorityHaltConditionMax;       
      
    }
    
    private boolean isSubtreeRoot () throws IloException {
        
        boolean isRoot = true;
        
        if (getNodeData()!=null  ) {
            NodeAttachment thisNodeData =(NodeAttachment) getNodeData();
            if (thisNodeData.getMetadataCopy().distanceFromSubtreeRoot>ZERO) {
                
                isRoot = false;
                
            }
        }    
        
        return isRoot;
        
    }
    
    
}
