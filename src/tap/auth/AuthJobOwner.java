package tap.auth;
/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.List;

import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.db.DBTable;
import adql.db.DBChecker;
import adql.db.exception.UnresolvedTableException;

import tap.TAPJob;
import tap.parameters.TAPParameters;
import tap.metadata.TAPTable;
import tap.metadata.TAPSchema;
import tap.config.ConfigurableTAPFactory;

import uws.job.user.JobOwner;
import uws.job.UWSJob;
import uws.job.JobList;
import uws.job.user.DefaultJobOwner;

/**
 * <p>A Job Owner who represents an authenticated user and has restricted access to specific components/tables of the TAP service. To be constructed by {@link tap.auth.ConfigurableAuthUserIdentifier}</p>
 *
 * <p>For now the main thing restricted between users is private datasets. This class will store the information regarding what tables are allowed them.</p>
 *
 * <p>Inherits many methods from {@link DefaultJobOwner}, such as the various getters and checks on if a user can read/write a given job, as the requirements for
 * those remain the same. </p>
 * 
 * @author Anthony Heng (AAO)
 * @version 04/2025
 * 
 * @see uws.service.UserIdentifier
 * @see uws.job.user.JobOwner
 * @see uws.job.user.DefaultJobOwner;
 */
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
	 * @see uws.job.user.JobOwner#hasReadPermission(uws.job.JobList)
	 */
	@Override
	public boolean hasReadPermission(JobList jl){
		//TODO: Any way to restrict job lists to one per user?
		return jl.getNbJobs(this)>0;
	}

	/**
	 * Authenticated Job Owners only have write permissions to a job list if they own a job in the list
	 * @see uws.job.user.JobOwner#hasWritePermission(uws.job.JobList)
	 */
	@Override
	public boolean hasWritePermission(JobList jl){
		// TODO: Any way to restrict job lists to one per user?
		return jl.getNbJobs(this)>0;
	}

	/**
	 * Tells whether this user has the right to execute and to abort the given job. 
	 * 
	 * For authenticated job owners, a job can be executed if the owner owns the job, and the resources (e.g. tables, schemas) they 
	 * are trying to access are allowed by them. 
	 * 
	 */
	@Override
	public boolean hasExecutePermission(UWSJob job) {
		boolean nullCheck =  (job == null) || (job.getOwner() == null);
		boolean isOwner = job.getOwner().equals(this);

		if (job instanceof TAPJob){
			try {
				boolean tapJobAllowed = checkTAPJobAllowed((TAPJob) job);
				return (nullCheck||isOwner) && tapJobAllowed; 
			} catch (ParseException e){
				// Cannot run this job due to malformed query, return false
				// TODO: I would like a way to report the reason why it failed and return false...
				// return false;
				return true; // Let it run. It'll get caught further down the line during execution and produce an informative exception
							 // Can't do it here as JobOwner.hasExecutePermission() does not normally throw checked exceptions
			}
		} else{
			return (nullCheck||isOwner);  
		}
	}

<<<<<<< HEAD
	/**
	 * Checks if a given TAPJob is allowed to be run by the owner
	 * @param  job TAPJob to check against
	 * @return     <code>true</code> if the this JobOwner meets all requirements for running the job <code>false</code> otherwise.
	 */
	private boolean checkTAPJobAllowed(TAPJob job){
=======
	private boolean checkTAPJobAllowed(TAPJob job) throws ParseException {
>>>>>>> b3b213b (Fixing up exception handling)
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

	        }
	        // parsed without any issues from the DBChecker, continue on to return true
	    }
	    return true;
	}

	/**
	 * Returns the list of tables the user is allowed to query
	 * @return List of TAPTables representing which tables the user is allowed to access
	 */
	public List<TAPTable> getAllowedTables(){
		return allowedTables;
	}


	/**
	 * Checks if table <code>t</code> is accessible by the user
	 * @param  t  table to check
	 * 
	 * @return true or false if the user has access to table t
	 */
	public boolean canAccessTable(TAPTable t){
		for(TAPTable userTable : allowedTables){
			if (userTable.getFullName().equals(t.getFullName())) 
				return true;
			
		}
		// Table is not found
		return false;		
	}

	/**
	 * Checks if Schema <code>s</code> is accessible by the user
	 * @param  s  schema to check
	 * 
	 * @return true or false if the user has access to the schema
	 */
	public boolean canAccessSchema(TAPSchema s){
		boolean foundSchema = false;
		for(TAPTable userTable : allowedTables){
			if (s.equals(userTable.getSchema()))
				return true;
		}
		// Schema has not been found
		return false;	
	}

	
}