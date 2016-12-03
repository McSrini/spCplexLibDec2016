/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.callbacks;
 
import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Constants.SolutionPhase.*;
import static ca.mcmaster.spcplexlibdec2016.Parameters.IS_MAXIMIZATION;
import   ca.mcmaster.spcplexlibdec2016.datatypes.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class NodeHandler extends BaseNodeHandler{
    
    private ActiveSubtreeMetaData subTreeMetaData ;
        
    //solution phase indicates whether we are farming,  or just solving
    private SolutionPhase solutionPhase ;
    
    public NodeHandler (ActiveSubtreeMetaData meta) {
        this.subTreeMetaData= meta;
    }

    @Override
    protected void main() throws IloException {
        
        long selectedNodeIndex= ZERO;//default
        
        if(getNremainingNodes64()== ONE){
            
            // get the node attachment for the node about to be processed
            
            NodeAttachment nodeData = (NodeAttachment) getNodeData(selectedNodeIndex );
            if (nodeData==null ) { //it will be null for subtree root
                 
                nodeData=new NodeAttachment (  
                        subTreeMetaData.rootNodeAttachment.getBranchingVariableUpperBounds(),
                        subTreeMetaData.rootNodeAttachment.getBranchingVariableLowerBounds(),  
                        subTreeMetaData.rootNodeAttachment.getDistanceFromOriginalRoot(),
                        ZERO );  
                
                setNodeData(selectedNodeIndex ,nodeData) ;
                
            } 
            
        }   
                        
        //if in farming phase, we choose node to be processed. Else we dont interfere with node selection
        if(DO_FARMING==solutionPhase && getNremainingNodes64()>ONE)     /*cannot farm root node*/     {
                                  
            selectedNodeIndex=getIndexOfFarmingCandidate();

            //if no farming candidate available, abort
            if(selectedNodeIndex < ZERO) abort();
                       
            //set useful metrics inside this node which are only available in the node handler
            initializeNodeWithMetricsUsedInMigrationDecisions(selectedNodeIndex);
                        
            selectNode(selectedNodeIndex);

        }
        
        if (DO_FARMING==solutionPhase && getNremainingNodes64()<=ONE) {
            abort();
        }
      
    }
        
    public void refresh(  SolutionPhase solutionPhase) {
        
        this.solutionPhase = solutionPhase;
    } 
    

 
}
