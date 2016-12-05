/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.datatypes;

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import ilog.concert.IloNumVar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author tamvadss
 * 
 * This object holds all the meta data associated with an ActiveSubtree.
 *
 */
public class ActiveSubtreeMetaData {
        
    //keep note of the root Node Attachment used to create this subtree
    public final NodeAttachment rootNodeAttachment ;
    
    //GUID used to identify the ActiveSubtree
    public  final String guid ; 
                    
    //keep note of all the variables in the model. We need INT and Bool variables to find bound tightenings
    public final IloNumVar[] numericVariables ; 
                
    //sometimes we find that the entire subtree can be discarded, because it cannot beat the incumbent 
    public boolean canDiscardEntireSubTree  = false;
    
    //Farming related meta data (which follows) is used by the callbacks
    
    //here are the farmed nodes which can be solved somewhere else , key is NodeID
    public Map< String,  NodeAttachment> farmedNodesMap = new LinkedHashMap< String, NodeAttachment>();
    
    //id of nodes that are actually chosen for migration. If not selected for migration, do not prune the node .
    public List<String> nodeIDsSelectedForMigration =  new ArrayList<String>();
    //Note that RAMP UP is special - several nodes are farmed, all of them  are pruned in advance, and all are chosen for migration 
    //Note also that we only allow 1 node per ActiveSubtree to be farmed, although the above data structures allow multiple nodes to be farmed
    
    //these stats are required to estimate time to completion of the subtree
    public double timeSpentSolvingThisSubtreeInSeconds = ZERO;
    public double lpRelaxValueAtBirth = ZERO;
    public double lpRelaxValueNow = ZERO;
    public double estimatedObjectiveValueAtCompletion = ZERO;
    
    //for performance statistics
    public long numActiveLeafs = ONE; //starts with root
    public int numNodesSelectedForMigrationSoFar = ZERO;
    public long numNodesBranchedUpon = ZERO;
    
    public ActiveSubtreeMetaData( NodeAttachment attachment, IloNumVar[] vars){
        guid = UUID.randomUUID().toString();
        rootNodeAttachment=attachment;
        this.numericVariables= vars;
    }
}
