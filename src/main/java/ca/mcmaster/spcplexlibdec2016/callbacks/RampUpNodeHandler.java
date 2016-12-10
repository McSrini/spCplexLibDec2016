/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.callbacks;

import static ca.mcmaster.spcplexlibdec2016.Constants.ONE;
import static ca.mcmaster.spcplexlibdec2016.Constants.ZERO;
import static ca.mcmaster.spcplexlibdec2016.Parameters.IS_MAXIMIZATION; 
import static ca.mcmaster.spcplexlibdec2016.Parameters.LEAF_NODES_AT_RAMP_UP_HALT;
import static ca.mcmaster.spcplexlibdec2016.Parameters.RAMP_UP_VAR_BOUND_RANGE;
import ca.mcmaster.spcplexlibdec2016.datatypes.ActiveSubtreeMetaData;
import ca.mcmaster.spcplexlibdec2016.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 * 
 * simply redirect to best remaining LP relax node
 * 
 */
public class RampUpNodeHandler  extends BaseNodeHandler{
      
    public RampUpNodeHandler (ActiveSubtreeMetaData meta) {
        this.subTreeMetaData= meta;
    }

    @Override
    protected void main() throws IloException {
                                    
        if(getNremainingNodes64()> ZERO){
                        
            if(getNremainingNodes64()== ONE){

                // get the node attachment for the node about to be processed

                NodeAttachment nodeData = (NodeAttachment) getNodeData(ZERO );
                if (nodeData==null ) { //it will be null for subtree root

                    nodeData=new NodeAttachment (  
                            subTreeMetaData.rootNodeAttachment.getBranchingVariableUpperBounds(),
                            subTreeMetaData.rootNodeAttachment.getBranchingVariableLowerBounds(),  
                            subTreeMetaData.rootNodeAttachment.getDistanceFromOriginalRoot(),
                            ZERO );  

                    //subTreeMetaData.lpRelaxValueAtBirth =  getObjValue(ZERO);
                    setNodeData(ZERO ,nodeData) ;

                } 

            }   
            
            boolean canFarm = false;
            long indexOfFarmingCandidate=-ONE;
            
            if (getNremainingNodes64()>=LEAF_NODES_AT_RAMP_UP_HALT) {
                
                indexOfFarmingCandidate=getIndexOfFarmingCandidate();
                if (subTreeMetaData.farmedNodesMap.size()>ZERO) {
                    
                    //farming candidate var bounds must be within 10% of the first farmed node
                    NodeAttachment firstFarmednodeData=(NodeAttachment)subTreeMetaData.farmedNodesMap.values().toArray()[ZERO];
                    NodeAttachment candidateNodeData = (NodeAttachment) getNodeData(indexOfFarmingCandidate );
                    canFarm = firstFarmednodeData.getSumofVarboundTightenings() * (ONE - RAMP_UP_VAR_BOUND_RANGE ) < candidateNodeData.getSumofVarboundTightenings();
                }else {
                    canFarm = true;
                }
            }
            
            //keep selecting best first until number of leafs grows to a threshold
            if (  !canFarm){
                long selectedIndex = ZERO;
                double bestKnownLPRelax = getObjValue(ZERO);

                /* 
                for(long index = ZERO; index <getNremainingNodes64(); index ++ ){
                    if ( IS_MAXIMIZATION && bestKnownLPRelax< getObjValue(index) ){
                        selectedIndex = index;
                        bestKnownLPRelax = getObjValue(index);
                    }
                    if ( !IS_MAXIMIZATION && bestKnownLPRelax > getObjValue(index ) ){
                        selectedIndex = index;
                        bestKnownLPRelax = getObjValue(index);
                    }
                } */
                
                selectNode(selectedIndex);
                
            } else {
                //we will farm now, select good migration candidate , which may not be the highest LP relax node
                
                long selectedNodeIndex=indexOfFarmingCandidate;
 
                //set useful metrics inside this node which are only available in the node handler
                //initializeNodeWithMetricsUsedInMigrationDecisions(selectedNodeIndex);
                                
                selectNode(selectedNodeIndex);
                
            }            
                         
        }
    }
    
}
