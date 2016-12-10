/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.callbacks;

import static ca.mcmaster.spcplexlibdec2016.Constants.ONE;
import static ca.mcmaster.spcplexlibdec2016.Constants.ZERO;
import static ca.mcmaster.spcplexlibdec2016.Parameters.LEAF_NODES_AT_RAMP_UP_HALT;
import static ca.mcmaster.spcplexlibdec2016.Parameters.NUM_PARTITIONS;
import ca.mcmaster.spcplexlibdec2016.datatypes.ActiveSubtreeMetaData;
import ca.mcmaster.spcplexlibdec2016.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 *   
 * 
 */
public class RampUpBranchHandler  extends BaseBranchhandler {
          
    public RampUpBranchHandler (ActiveSubtreeMetaData metaData ) {
        this.  subtreeMetaData= metaData;    
         
    }

    @Override
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
            
            //if threshold reached, start farming
            if (getNremainingNodes64()>=LEAF_NODES_AT_RAMP_UP_HALT){
                
                //reduce threshold by 1
                LEAF_NODES_AT_RAMP_UP_HALT --;
                
                //farm the node
                this.subtreeMetaData.farmedNodesMap.put(nodeData .getNodeid(),nodeData  );
                prune();
                
                //check if ramp up complete
                if (subtreeMetaData.farmedNodesMap.size()==(NUM_PARTITIONS-ONE)) abort();
                
            } else {
                //simply create the 2 child nodes
                createTwoChildNodes(  nodeData);
            }
            
            
            this.subtreeMetaData.numActiveLeafs= getNremainingNodes64();
            
        } //end if getNbranches()> ZERO
    } //end main
    
}//end class
