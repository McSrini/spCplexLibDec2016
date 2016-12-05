/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibdec2016.solver;

import ca.mcmaster.spcplexlibdec2016.Constants;
import static ca.mcmaster.spcplexlibdec2016.Constants.ZERO;
import static ca.mcmaster.spcplexlibdec2016.Parameters.BacktrackSelection;
import static ca.mcmaster.spcplexlibdec2016.Parameters.IS_MAXIMIZATION;
import static ca.mcmaster.spcplexlibdec2016.Parameters.RELATIVE_MIP_GAP;
import static ca.mcmaster.spcplexlibdec2016.Parameters.USER_SELECTED_SEARCH_STRATEGY;
import ca.mcmaster.spcplexlibdec2016.callbacks.BranchHandler;
import ca.mcmaster.spcplexlibdec2016.callbacks.NodeHandler;
import ca.mcmaster.spcplexlibdec2016.callbacks.*;
import ca.mcmaster.spcplexlibdec2016.datatypes.ActiveSubtreeMetaData;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author tamvadss
 */
public class RampUpSolver {
    
    //this is the CPLEX object we are attached to  
    private IloCplex cplex   ;
   
    //this is the branch handler for the CPLEX object
    private RampUpBranchHandler rampUpBranchHandler;
    //and the node handler
    private RampUpNodeHandler rampUpNodeHandler ;
    
    public RampUpSolver (IloCplex cplex , ActiveSubtreeMetaData metaData ) throws Exception{
            
        this.cplex=cplex;
        
        rampUpBranchHandler = new RampUpBranchHandler(   metaData       );
        rampUpNodeHandler = new  RampUpNodeHandler ( metaData  ) ;
        
        this.cplex.use(rampUpBranchHandler);
        this.cplex.use(rampUpNodeHandler);   
        
        setSolverParams();  
    
    }
        
    public IloCplex.Status solve()            throws  Exception{
                  
        cplex.solve();
        
        //if (  isLoggingInitialized) logger.info("Result of solving is" + cplex.getStatus());
        return cplex.getStatus();
    }
        
    private void setSolverParams() throws IloException {
        //depth first?
        //cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect,  USER_SELECTED_SEARCH_STRATEGY); 
        
        //near strict best first
        //cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  BacktrackSelection); 
        
        /** strong branching     cplex.setParam(IloCplex.Param.MIP.Strategy.VariableSelect,  3); */
    
        //SET DO NOT USE DISK 
         cplex.setParam( IloCplex.Param.MIP.Strategy.File , ZERO);
        
        //MIP gap
        //if ( RELATIVE_MIP_GAP>ZERO) cplex.setParam( IloCplex.Param.MIP.Tolerances.MIPGap, RELATIVE_MIP_GAP);

        //others
    }
    
    
}
