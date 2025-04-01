package tap.auth;
import uws.job.user.JobOwner;
import tap.TAPJob;
import uws.job.UWSJob;
import uws.job.JobList;
import tap.parameters.TAPParameters;
import tap.metadata.TAPTable;
import adql.db.DBChecker;
import tap.config.ConfigurableTAPFactory;
import adql.db.DBTable;
import uws.job.user.DefaultJobOwner;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.db.exception.UnresolvedTableException;
import java.util.List;


public class AuthJobOwner extends DefaultJobOwner {
	
	protected List<TAPTable> allowedTables; 

	/**
	 * Builds a Job Owner which has the given ID.
	 * Its pseudo will also be equal to the given ID.
	 * 
	 * @param name	ID/Pseudo of the Job Owner to create.
	 */
	public AuthJobOwner(final String name, List<TAPTable> allowedTables){
		this(name, name, allowedTables);
	}

	public AuthJobOwner(final String id, final String pseudo, List<TAPTable> allowedTables){
		super(id, pseudo);
		this.allowedTables = allowedTables;
	}

	/**
	 * Only have read permissions if this AuthJobOwner has a job in the list
	 * TODO: Any way to restrict job lists to one per user?
	 */
	@Override
	public boolean hasReadPermission(JobList jl){
		return jl.getNbJobs(this)>0;
	}

	/**
	 * Only have write permissions to a job list if this AuthJobOwner has a job in the list
	 * TODO: Any way to restrict job lists to one per user?
	 */
	@Override
	public boolean hasWritePermission(JobList jl){
		return jl.getNbJobs(this)>0;
	}

	@Override
	public boolean hasExecutePermission(UWSJob job){
		boolean nullCheck =  (job == null) || (job.getOwner() == null);
		boolean isOwner = job.getOwner().equals(this);

		if (job instanceof TAPJob){
			return (nullCheck||isOwner) && checkTAPJobAllowed((TAPJob) job); 
		} else{
			return (nullCheck||isOwner);  
		}
	}

	private boolean checkTAPJobAllowed(TAPJob job){
		TAPParameters tapParams = job.getTapParams();
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

	        } catch(ParseException pe){
	        	// If a parse runs into this exception, then thequery is malformed. Rethrow.
	        	throw pe;

	        }  
	        // parsed without any issues, continue on to return true
	    }
	    return true;
	}

	public List<TAPTable> getAllowedTables(){
		return allowedTables;
	}

	public boolean canAccessTable(TAPTable table){
		for(TAPTable userTable : allowedTables){
			if (userTable.getFullName().equals(table.getFullName())) {
				return true;
			}
		}
		// Table is not found
		return false;

		
	}

	
}