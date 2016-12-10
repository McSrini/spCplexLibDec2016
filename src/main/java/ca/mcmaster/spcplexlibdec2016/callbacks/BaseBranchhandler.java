/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.callbacks;

import static ca.mcmaster.spcplexlibdec2016.Constants.TWO;
import static ca.mcmaster.spcplexlibdec2016.Constants.ZERO;
import ca.mcmaster.spcplexlibdec2016.datatypes.ActiveSubtreeMetaData;
import ca.mcmaster.spcplexlibdec2016.datatypes.NodeAttachment;
import ca.mcmaster.spcplexlibdec2016.utilities.UtilityLibrary;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public abstract class BaseBranchhandler  extends IloCplex.BranchCallback{
    
    //meta data of the subtree which we are monitoring
    protected ActiveSubtreeMetaData subtreeMetaData;
    
    protected void createTwoChildNodes(NodeAttachment parentNodeData) throws IloException{
        
        //First, append the 
        //branching conditions so we can pass them on to the kids

        //get the branches about to be created
        IloNumVar[][] vars = new IloNumVar[TWO][] ;
        double[ ][] bounds = new double[TWO ][];
        IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
        getBranches(  vars, bounds, dirs);
 
        //now allow  both kids to spawn
        for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {    
            //apply the bound changes specific to this child
            NodeAttachment thisChild  = UtilityLibrary.createChildNode( parentNodeData,
                    dirs[childNum], bounds[childNum], vars[childNum]   ); 
         
            //   create the  kid,  and attach node data  to the kid
            IloCplex.NodeId nodeID = makeBranch(childNum,thisChild );

            thisChild.setNodeid(nodeID.toString()); 
            
            //set parents sum of bound tighening values into kid
            thisChild.setSumofVarboundTightenings( getSumofVarboundTighteningsForIntegerVars());

            //IMPORTANT - HERE WE CAN ALSO INCLUDE BASIS INFO in child_node data, IF AVAILABLE

        }//end for 2 kids
    }
        
    protected double getSumofVarboundTighteningsForIntegerVars() throws IloException{
        IloNumVar[] allVars = this.subtreeMetaData.numericVariables;
        
        double result = ZERO;
        
        for(IloNumVar var:allVars){
            if (var.getType().equals( IloNumVarType.Bool)||var.getType().equals( IloNumVarType.Int)) {
                result += Math.abs(getUB(var)-getLB(var));
                
            }                
        }
        return result;
    }
    
}
