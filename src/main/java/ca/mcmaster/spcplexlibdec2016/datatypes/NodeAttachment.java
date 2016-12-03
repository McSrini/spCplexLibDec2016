/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.datatypes;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tamvadss
 * 
 *  
 * This object is attached to every tree node, and can be used to reconstruct migrated leaf nodes.
 * 
 * It contains branching variable bounds, and other useful information.
 * 
 */
public class NodeAttachment  implements Serializable{
    
    //every time there is a branching on a variable, we update on of these lists with the
    //new bound corresponding to the branching condition
    //
    //the key is the Variable name
    //
    //
    private Map< String, Double > branchingVariableUpperBounds  = new HashMap< String, Double >();
    private Map< String, Double > branchingVariableLowerBounds = new HashMap< String, Double >();
    
    //meta data contains some metric such as LP relax value of this node and other information
    private NodeAttachmentMetadata nodeMetadata = new NodeAttachmentMetadata();
    
    //constructors    
    public NodeAttachment (){
        
    }
        
    public NodeAttachment (    Map< String, Double > upperBounds, 
            Map< String, Double > lowerBounds,  int distanceFromOriginalRoot, int distanceFromSubtreeRoot ) {
         
        for (Map.Entry <String, Double> entry : upperBounds.entrySet()){
            this.branchingVariableUpperBounds.put(entry.getKey(), entry.getValue()  );
        }
        for (Map.Entry <String, Double> entry : lowerBounds.entrySet()){
            this.branchingVariableLowerBounds.put(entry.getKey(), entry.getValue()  );
        }
        
        nodeMetadata.distanceFromOriginalRoot=distanceFromOriginalRoot;
        nodeMetadata.distanceFromSubtreeRoot=distanceFromSubtreeRoot ;
         
    }
    
    
    public void setDistanceFromOriginalRoot(int val) {
        this.nodeMetadata.distanceFromOriginalRoot= val;
    }
        
    public int getDistanceFromOriginalRoot( ) {
        return nodeMetadata.distanceFromOriginalRoot;
    }
    
    public void setDistanceFromSubtreeRoot(int val) {
        nodeMetadata.distanceFromSubtreeRoot= val;
    }
        
    public int getDistanceFromSubtreeRoot( ) {
        return nodeMetadata.distanceFromSubtreeRoot;
    }
    
    public void setNumberOfIntInfeasibilities(int val) {
       nodeMetadata. numberOfIntegerInfeasibilities= val;
    }
        
    public int getNumberOfIntInfeasibilities( ) {
        return nodeMetadata.numberOfIntegerInfeasibilities;
    }
    
    public void setSumOfIntInfeasibilities(double val) {
       nodeMetadata.sumOfIntegerInfeasibilities= val;
    }
        
    public double getSumOfIntInfeasibilities( ) {
        return nodeMetadata.sumOfIntegerInfeasibilities;
    }
    
    public void setNodeid(String val) {
        nodeMetadata.nodeID= val;
    }
        
    public String getNodeid( ) {
        return nodeMetadata.nodeID;
    }
    
    public void setLPrelax(double val) {
        nodeMetadata.lpRelaxValue= val;
    }
        
    public double getLPrelax( ) {
        return nodeMetadata.lpRelaxValue;
    }
    
    public void setSumofVarboundTightenings(double val){
        this.nodeMetadata.sumOfVarBoundTightenings=val;
    }
    
    public double getSumofVarboundTightenings(){
        return this.nodeMetadata.sumOfVarBoundTightenings;
    }
    
    public Map< String, Double >   getBranchingVariableUpperBounds   () {
        return  Collections.unmodifiableMap(branchingVariableUpperBounds)  ;
    }

    public Map< String, Double >   getBranchingVariableLowerBounds   () {
        return   Collections.unmodifiableMap(branchingVariableLowerBounds) ;
    }
        
    public NodeAttachmentMetadata getMetadataCopy () {
        
        return this.nodeMetadata.cloneMe();
    }
    
    
}
