/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.datatypes;

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import java.io.Serializable;

/**
 *
 * @author tamvadss
 *  
 * details of node attachment which can be 
 * used to make load balancing decisions, without having the entire node attachment available
 */
public class NodeAttachmentMetadata  implements Serializable{
    
    // distance From Original problem    , never changes
    public int distanceFromOriginalRoot =ZERO;   
    //    this is  the depth in the current subtree   , may change to 0 if node is migrated
    public int distanceFromSubtreeRoot=ZERO; 
    
    //unique node ID assigned by CPLEX within an Ilocplex object
    public String nodeID;
    
    //a few metrics which can be used to decide which nodes to pluck out
    public int numberOfIntegerInfeasibilities = ZERO;
    public double  sumOfIntegerInfeasibilities = ZERO;
    public double lpRelaxValue =ZERO;
    public double sumOfVarBoundTightenings = ZERO;
            
    //other warm start information such as basis info  can be included here
    
    public NodeAttachmentMetadata cloneMe () {
        NodeAttachmentMetadata cloned = new NodeAttachmentMetadata();
        
        cloned.distanceFromOriginalRoot = this.distanceFromOriginalRoot;
        cloned.distanceFromSubtreeRoot= this.distanceFromSubtreeRoot;
                
        cloned.nodeID= this.nodeID;
        cloned.numberOfIntegerInfeasibilities= this.numberOfIntegerInfeasibilities;
        cloned.sumOfIntegerInfeasibilities=this.sumOfIntegerInfeasibilities;
        cloned.lpRelaxValue= this.lpRelaxValue;
        cloned.sumOfVarBoundTightenings=this.sumOfVarBoundTightenings;
        
        return cloned;
    }
    
    
}
