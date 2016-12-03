package ca.mcmaster.spcplexlibdec2016.utilities;

 
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import java.util.*;
import java.util.Map.Entry; 

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Parameters.*;
import ca.mcmaster.spcplexlibdec2016.datatypes.*;

public class UtilityLibrary {
    
    /**
     * read the CPLEX object and update solution object with variables and their values
     * 
     * Assumes that the CPLEX object is solved to optimality or feasibility
   
     */
    public static void addVariablevaluesToSolution    (IloCplex cplex, Solution soln) throws  IloException {
        
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();

        //WARNING: we assume that every variable appears in at least 1 constraint or variable bound
        //Otherwise, this method of getting all the variables from the matrix may not yield all the variables
        
        IloNumVar[] variables = lpMatrix.getNumVars();
        double[] variableValues = cplex.getValues(variables);                 

        for ( int index = ZERO; index < variableValues.length; index ++){

            String varName = variables[index].getName();
            double varValue = variableValues[index];
            soln.setVariableValue (varName,  varValue);

        }
    }
    
    
    public static NodeAttachment createChildNode (NodeAttachment parentNode, BranchDirection[ ] directionArray, 
            double[ ] boundArray, IloNumVar[] varArray  ) {

        //depth of child is 1 more than parent
         
        NodeAttachment child =new NodeAttachment (    parentNode.getBranchingVariableUpperBounds(), 
                parentNode.getBranchingVariableLowerBounds() ,  ONE+parentNode.getDistanceFromOriginalRoot(),
                ONE+parentNode.getDistanceFromSubtreeRoot()  ) ;            
                
        //now apply the new bounds to the existing bounds
        for (int index = 0 ; index < varArray.length; index ++) {                           
            mergeBound(child, varArray[index].getName(), boundArray[index] , 
                    directionArray[index].equals(BranchDirection.Down));
        }

        return child;
    }
    
    public static boolean mergeBound(NodeAttachment node, String varName, double value, boolean isUpperBound) {
        boolean isMerged = false;

        if (isUpperBound){
            Map< String, Double >  upperBounds = node.getBranchingVariableUpperBounds() ;
            if (upperBounds.containsKey(varName)) {
                if (value < upperBounds.get(varName)){
                    //update the more restrictive upper bound
                    upperBounds.put(varName, value);
                    isMerged = true;
                }
            }else {
                //add the bound
                upperBounds.put(varName, value);
                isMerged = true;
            }
        } else {
            //it is a lower bound
            Map< String, Double >  lowerBounds = node.getBranchingVariableLowerBounds() ;
            if (lowerBounds.containsKey(varName)) {
                if (value > lowerBounds.get(varName)){
                    //update the more restrictive lower bound
                    lowerBounds.put(varName, value);
                    isMerged = true;
                }               
            }else {
                //add the bound
                lowerBounds.put(varName, value);
                isMerged = true;
            }
        }

        return isMerged;
    }
    
    /**
     * 
     * To the CPLEX object ,  apply all the bounds mentioned in attachment
     */
    public static void  merge ( IloCplex cplex, NodeAttachment attachment   ) throws IloException {

        IloLPMatrix lpMatrix = (IloLPMatrix) cplex .LPMatrixIterator().next();

        //WARNING : we assume that every variable appears in at least 1 constraint or variable bound
        IloNumVar[] variables = lpMatrix.getNumVars();

        for (int index = ZERO ; index <variables.length; index ++ ){

            IloNumVar thisVar = variables[index];
            updateVariableBounds(thisVar,attachment.getBranchingVariableLowerBounds(), false );
            updateVariableBounds(thisVar,attachment.getBranchingVariableUpperBounds(), true );

        }       
    }
    
    /**
     * 
     *  Update variable bounds as specified    
     */
    public static   void updateVariableBounds(IloNumVar var, Map< String,Double > newBounds, boolean isUpperBound   ) 
            throws IloException{

        String varName = var.getName();
        boolean isPresentInNewBounds = newBounds.containsKey(varName);

        if (isPresentInNewBounds) {
            double newBound =   newBounds.get(varName)  ;
            if (isUpperBound){
                if ( var.getUB() > newBound ){
                    //update the more restrictive upper bound
                    var.setUB( newBound );
                }
            }else{
                if ( var.getLB() < newBound){
                    //update the more restrictive lower bound
                    var.setLB(newBound);
                }
            }               
        }

    }  

}
