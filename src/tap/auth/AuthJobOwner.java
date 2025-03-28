package tap.auth;
import uws.job.user.DefaultJobOwner;
import tap.TAPJob;
import uws.job.UWSJob;
import tap.parameters.TAPParameters;
import tap.metadata.TAPTable;
import adql.parser.DBChecker;
import tap.config.ConfigurableTAPFactory;
import adql.db.DBTable;
import uws.job.user.JobOwner;
import adql.parser.ADQLParser;
import adql.db.exception.UnresolvedTableException;


public class AuthJobOwner extends DefaultJobOwner {
	
	protected List<String> allowedTables; 

	/**
	 * Builds a Job Owner which has the given ID.
	 * Its pseudo will also be equal to the given ID.
	 * 
	 * @param name	ID/Pseudo of the Job Owner to create.
	 */
	public AuthJobOwner(final String name, List<String> allowedTables){
		this(name, name, allowedTables);
	}

	public AuthJobOwner(final String id, final String pseudo, List<String> allowedTables){
		super(id, pseudo);
		this.allowedTables = allowedTables;
	}

	@Override
	public hasExecutePermission(UWSJob job){
		boolean canExecute;
		if (job instanceof TAPJob){
			return super.hasExecutePermission(job) && checkTAPJobAllowed((TAPJob) job); 
		} else{
			return super.hasExecutePermission(job); 
		}
	}

	private boolean checkTAPJobAllowed(TAPJob job){
		TAPParameters tapparams = job.getTapParams();
		// If TAPJob is allowed
		if (tapParams.getRequest().equals(TAPJob.REQUEST_DO_QUERY)){
	        DBChecker allowedTableChecker = new DBChecker(this.allowedTables);
	        // Build ADQL Parser with new checker
	        ADQLParser adqlParse = new ADQLParser(allowedTableChecker);
	        // Parse the query from the request   
	        String queryString = tapParams.getQuery();
	        // Parse the query for the sake of getting checked by allowedTableChecker. 
	        // No need to store the result as ADQLExecutor will do that later
	        try{
	        	adqlParse.parseQuery(queryString);
	        } catch(UnresolvedTableException ute){
	        	// If a parse runs into this exception, then it is not on the list of allowed tables. Return false
	        	return false;

	        }   
	        // parsed without any issues, continue on to return true
	    }
	    return true;
	}

	private List<String> getAllowedTables(){
		return allowedTables;
	}

	
}