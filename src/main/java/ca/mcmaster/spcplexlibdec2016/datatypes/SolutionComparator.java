package  ca.mcmaster.spcplexlibdec2016.datatypes; 
  
import static ca.mcmaster.spcplexlibdec2016.Constants.*;
import static ca.mcmaster.spcplexlibdec2016.Parameters.*;
import java.io.Serializable;
import java.util.*;

 
  
public class SolutionComparator implements Comparator<  Solution > , Serializable {
 
    /**
     * 
     */
    private static final long serialVersionUID = 6312143274435503803L;

    //return 1 if soln2 is better, else return 0
    //
    //if soln2 is unbounded or in error, we return 1. 
    //Caller should check status and abort computation, in case of error/unbounded.
    //
    public int compare(Solution incumbentSolution, Solution challenger) {

        int retval = ZERO;
        
        if (incumbentSolution.isError() || incumbentSolution.isUnbounded()) {
            //incumbent wins, since we want to halt computation in this case
             retval=ZERO;
        }  else         if (challenger.isError() || challenger.isUnbounded())    {
             retval=ONE;
        } else if (challenger.isFeasibleOrOptimal())    {
            if (incumbentSolution.isFeasibleOrOptimal()) {
                if (  IS_MAXIMIZATION &&  incumbentSolution.getObjectiveValue() < challenger.getObjectiveValue()  ) retval = ONE;
                if (!  IS_MAXIMIZATION &&  incumbentSolution.getObjectiveValue() > challenger.getObjectiveValue()  ) retval = ONE;
                //for the same objective value, prefer optimal solution over feasible
                if (incumbentSolution.getObjectiveValue() == challenger.getObjectiveValue() && challenger.isOptimal() && !incumbentSolution.isOptimal() )  retval = ONE;
            }else{
                retval=ONE;
            }             
        } 
        
        return retval;
        
    }
  
}
