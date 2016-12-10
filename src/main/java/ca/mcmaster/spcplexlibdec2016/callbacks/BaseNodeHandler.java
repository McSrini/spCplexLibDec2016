/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.callbacks;

import static ca.mcmaster.spcplexlibdec2016.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.spcplexlibdec2016.Constants.MINUS_INFINITY;
import static ca.mcmaster.spcplexlibdec2016.Constants.ONE;
import static ca.mcmaster.spcplexlibdec2016.Constants.PLUS_INFINITY;
import static ca.mcmaster.spcplexlibdec2016.Constants.ZERO;
import static ca.mcmaster.spcplexlibdec2016.Parameters.IS_MAXIMIZATION;
import static ca.mcmaster.spcplexlibdec2016.Parameters.LOG_FOLDER;
import static ca.mcmaster.spcplexlibdec2016.Parameters.PARTITION_ID;
import ca.mcmaster.spcplexlibdec2016.datatypes.ActiveSubtreeMetaData;
import ca.mcmaster.spcplexlibdec2016.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public abstract class BaseNodeHandler extends IloCplex.NodeCallback{
    
    private static Logger logger=Logger.getLogger(BaseNodeHandler.class);
    
    protected ActiveSubtreeMetaData subTreeMetaData ;
            
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+
                    BaseNodeHandler.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
             
        } catch (IOException ex) {
            ///
        }
          
    }
       
    //this method chooses the node for farming -- change this method if you have better heuristics for choosing migration candidates
    //
    //current logic is highest LP  relax node for maximization, or lowest LP relax node for minimization having 
    //above avg estimate of objective,  and above avg var bounds
    //
    protected long getIndexOfFarmingCandidate() throws IloException{
          
        long selectedNodeIndex= ZERO;//default
        double bestKnownLPRelax = getObjValue(ZERO) ;
        
        NodeAttachment nodeData = (NodeAttachment) getNodeData(ZERO );
        double selectedNodesVarBoundTighenings = nodeData.getSumofVarboundTightenings();
        double selectedNodesEstimatedObjectiveValue =  getEstimatedObjValue(ZERO );
        
        NodeMetrics metrics = getAverageMetrics();
        
        for(long index = ONE; index <getNremainingNodes64(); index ++ ){
            nodeData = (NodeAttachment) getNodeData(index );
            if (nodeData.getSumofVarboundTightenings()>= metrics.avgVarBoundTightenings  ){
                
                if ( IS_MAXIMIZATION && bestKnownLPRelax< getObjValue(index) &&  metrics.avgEstimatedObjValue <= getEstimatedObjValue(index )) {
                    bestKnownLPRelax =getObjValue(index);
                    selectedNodeIndex = index;
                    
                    selectedNodesVarBoundTighenings=nodeData.getSumofVarboundTightenings();
                    selectedNodesEstimatedObjectiveValue= getEstimatedObjValue(index );
                    
                } else if (! IS_MAXIMIZATION && bestKnownLPRelax> getObjValue(index) &&  metrics.avgEstimatedObjValue >= getEstimatedObjValue(index )) {
                    bestKnownLPRelax =getObjValue(index);
                    selectedNodeIndex = index;
                    
                    selectedNodesVarBoundTighenings=nodeData.getSumofVarboundTightenings();
                    selectedNodesEstimatedObjectiveValue= getEstimatedObjValue(index );
                
                }                
                
            }
        }
        
        logger.info("LP relax of farming candidate " + bestKnownLPRelax + " Var bound tighetening " + selectedNodesVarBoundTighenings + 
                " Estimated Obj value " + selectedNodesEstimatedObjectiveValue); 
        return selectedNodeIndex;
    }
     
    /*protected void initializeNodeWithMetricsUsedInMigrationDecisions(long selectedNodeIndex) throws IloException {
         NodeAttachment nodeData = (NodeAttachment) getNodeData(selectedNodeIndex );
         nodeData.setNumberOfIntInfeasibilities(getNinfeasibilities(selectedNodeIndex) );
         nodeData.setSumOfIntInfeasibilities( getInfeasibilitySum(selectedNodeIndex));
         setNodeData(selectedNodeIndex ,nodeData) ;
    }*/
    
    protected NodeMetrics getAverageMetrics () throws IloException{
                
        NodeMetrics metrics = new NodeMetrics();
        long count = ZERO;
        
        for(long index = ZERO; index <getNremainingNodes64(); index ++ ){
            NodeAttachment nodeData = (NodeAttachment) getNodeData(index );
            metrics.avgVarBoundTightenings += nodeData.getSumofVarboundTightenings();
            metrics.avgEstimatedObjValue += getEstimatedObjValue(index );
            metrics.avgLPRelax +=       getObjValue(index) ;
            count ++;
        }
        if(count!=ZERO){
            metrics.avgVarBoundTightenings = metrics.avgVarBoundTightenings/count;
            metrics.avgEstimatedObjValue=metrics.avgEstimatedObjValue/count;
            metrics.avgLPRelax=metrics.avgLPRelax/count;
        }
        
        return metrics; 
    }
       
    
    
}
