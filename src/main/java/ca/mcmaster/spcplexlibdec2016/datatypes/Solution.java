package ca.mcmaster.spcplexlibdec2016.datatypes;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Parameters.*;
 
//objective value of any subtree, its corresponding variables, whether it is feasible or not etc.

public class Solution implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 7879596549786859622L;

    /**
     * 
     */
    
    //this object does not use any cplex status data structures directly, to ensure serializability

    private double objectiveValue;    
    
    private boolean isFeasible;
    private boolean isUnFeasible; 
    private boolean isOptimal;
    private boolean isUnbounded; 
    private boolean isError; 
    
    //a map of variable names, and their values
    private  Map< String, Double> variableMap ;

    public Solution( ) {
        //default solution  will be inferior to 
        //any other feasible or optimal solution
        isFeasible = false;
        isOptimal=false;
        isError=false;
        isUnbounded=false;
        isUnFeasible=true;              
        variableMap =  new Hashtable<String, Double>();
       
        objectiveValue = IS_MAXIMIZATION? MINUS_INFINITY: PLUS_INFINITY;
    }
    
    //other methods, get set etc.
    
    //set value for variable
    public void setVariableValue(String name, double val){
        variableMap.put(name, val);
    }
    
    public double getVariableValue(String name){
        return variableMap.get(name);
    }
    
    public Map< String, Double> getVariableMap(){
        return variableMap;
    }
    
    public boolean isError(){
        return this.isError;
    }
    
    public void setError(boolean err){
        isError=err;
    }
    
    public boolean isOptimal(){
        return isOptimal;
    }
    
    public void setOptimal(boolean opt){
        this.isOptimal=opt;
    }
    
    public boolean isFeasible(){
        return isFeasible;
    }
    
    public boolean isUnFeasible(){
        return isUnFeasible;
    }
    
    public boolean isFeasibleOrOptimal(){
        return isFeasible || isOptimal;
    }
    
    public void setFeasible(boolean feasible){
        isFeasible=feasible;
    }
    
    public void setUnFeasible(boolean unfes){
        isUnFeasible=unfes;
    }
        
    public boolean isUnbounded(){
        return this.isUnbounded;
    }
    
    public void setUnbounded(boolean unbounded){
        this.isUnbounded=unbounded;
    }
    public void setOptimumValue(double optimumValue){
        this.objectiveValue= optimumValue;
    }
    
    public double getObjectiveValue(){
        return objectiveValue;
    }

    public String toString (){
        String result=NEWLINE; 
        if (this.isFeasibleOrOptimal()){
            result += this.isOptimal? OPTIMAL_SOLUTION : FEASIBLE_SOLUTION ;
            result+= BLANKSPACE;
            result +=    this.getObjectiveValue()  +NEWLINE  ;
            
            for (Map.Entry<String, Double >  entry :variableMap.entrySet()) {
                result += entry.getKey() + BLANKSPACE + entry.getValue() +NEWLINE;
            }
        } else if (this.isError) {
            result = ERROR_SOLUTION;
        } else if (this.isUnFeasible){
            result = INFEASIBLE_SOLUTION;
        }else if (this.isUnbounded){
            result = UNBOUNDED_SOLUTION;
        }

        return result;
    }
}
