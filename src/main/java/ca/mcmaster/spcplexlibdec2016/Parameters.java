/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016;

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import java.io.Serializable;

/**
 *
 * @author tamvadss
 */
public class Parameters  implements Serializable{
    
    //should move to properties file
    
    //this is the file we are solving
    public static final String SAV_FILENAME="F:\\temporary files here\\a1c1s1.mps";   
    public  static final  boolean IS_MAXIMIZATION = false;
    
    
    public static final String HALT_FILE = "F:\\temporary files here\\haltfile.txt";
    
    public static final int NUM_PARTITIONS =   ONE*(TWO -ONE) + THREE *TWO;
    
    public static final int  LEAF_NODES_AT_RAMP_UP_HALT  =  Math.min(TEN*FIVE +NUM_PARTITIONS , FIVE*NUM_PARTITIONS); 
    
    public static   int NUM_NODES_TO_MIGRATE = ONE; //increases later
    
    // map cycle time
    public  static    int SOLUTION_TIME_SLICE_PER_PARTITION_IN_SECONDS  =     FIVE*SIX*TEN; //five minutes
            
    //limit the number of Iloclex objects in any partition  . 
    //This is required so we do not run out of memory ; better to have bigger IloCplex objects than many small ones.
    //
    //If we have more than 1 Iloclex on a partition, then they are equally time sliced during the map cycle
    //
    public  static final  int MAX_ACTIVE_SUBTREES_PER_PARTITION  =     FOUR; 
    
    //search strategy
    public static final int DepthFirst= ZERO;
    public static final int BestFirst= ONE;
    public static final int BestEstimeateFirst= TWO;
    //user can set default strategy
    public static int  USER_SELECTED_SEARCH_STRATEGY = BestFirst  ; 
    public static double BacktrackSelection = ZERO;
    
    //The partition on which this library (i.e. the ActiveSubtree and supporting objects) live
    public static int  PARTITION_ID = ONE;
    //This is used for logging
    public static String LOG_FOLDER="F:\\temporary files here\\";
        
    public static double  RELATIVE_MIP_GAP = ZERO; 
}
