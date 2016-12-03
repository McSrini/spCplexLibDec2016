/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.solver;

import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Parameters.*;
import ca.mcmaster.spcplexlibdec2016.callbacks.*;
import ca.mcmaster.spcplexlibdec2016.datatypes.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class Solver {
    
    //this is the CPLEX object we are attached to  
    private IloCplex cplex   ;
   
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    //and the node handler
    private NodeHandler nodeHandler ;
         
    public Solver (IloCplex cplex , ActiveSubtreeMetaData metaData ) throws Exception{
            
        this.cplex=cplex;
        
        branchHandler = new BranchHandler(   metaData       );
        nodeHandler = new  NodeHandler ( metaData  ) ;
        
        this.cplex.use(branchHandler);
        this.cplex.use(nodeHandler);   
        
        setSolverParams();  
    
    }
    
    public IloCplex.Status solve(double timeSliceInSeconds,     double bestKnownGlobalOptimum ,SolutionPhase solutionPhase   ) 
            throws  Exception{
                
        branchHandler.refresh(bestKnownGlobalOptimum,   solutionPhase );  
        this.nodeHandler.refresh(solutionPhase);
         
        cplex.setParam(IloCplex.Param.TimeLimit, timeSliceInSeconds); 
        //set cutoff
        if (IS_MAXIMIZATION) {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.LowerCutoff, bestKnownGlobalOptimum);
        }else {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff, bestKnownGlobalOptimum);
        }
        cplex.solve();
        
        //if (  isLoggingInitialized) logger.info("Result of solving is" + cplex.getStatus());
        return cplex.getStatus();
    }
        
    private void setSolverParams() throws IloException {
        //depth first?
        cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect,  USER_SELECTED_SEARCH_STRATEGY); 
        
        //near strict best first
        cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  BacktrackSelection); 
        
        /** strong branching     cplex.setParam(IloCplex.Param.MIP.Strategy.VariableSelect,  3); */
    
        //SET DO NOT USE DISK 
         cplex.setParam( IloCplex.Param.MIP.Strategy.File , ZERO);
        
        //MIP gap
        if ( RELATIVE_MIP_GAP>ZERO) cplex.setParam( IloCplex.Param.MIP.Tolerances.MIPGap, RELATIVE_MIP_GAP);

        //others
    }
    
    
}
