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
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import tap.TAPException;
import tap.communication.APIClient;
import tap.communication.JSONAPIClient;
import tap.metadata.TAPTable;

import uws.service.UserIdentifier;
import uws.service.UWSUrl;
import uws.job.user.JobOwner;
import uws.UWSException;

/**
 * <p>A {@link UserIdentifier} implementation for handling authenticated users. Needs to be setup in the tap.properties file the expected request header and response field names attached to their purpose.</p>
 *
 * <p>Authenticated users at the moment are only identified using a authentication header in the request. It is expected there to be another endpoint to provide these sessions and a custom TAP servlet 
 * to add the authentication header using the developer's own choice of method</p>
 *
 * <p>The session header will be sent to a given authentication URL, and the response is expected to send back all relevant details needed to build a {@link AuthJobOwner} object. 
 * The keys used to store the session in the request, the authentication API details and the keys storing the user details in the response, all will be set within the tap.properties file </p>
 *
 * <p>The following properties need to be set to use this class
 *  <ul>
 *   <li>sessionid_header_field</li>
 *   <li>session_authentication_url</li>
 *   <li>response_id_field</li>
 *   <li>response_pseudo_field</li>
 *   <li>response_tables_field</li>
 * </ul>
 * </p>
 *
 * <p>With all required properties set, this class should either be initialised in the tap servlet or set as the <i>user_identifier</i> in the tap.properties file i.e. <code>user_identifier = tap.auth.ConfigurableUserIdentifier</code></p>
 * 
 * @author Anthony Heng (AAO)
 * @version 04/2025
 *
 * @see tap.communication.APIClient
 * @see tap.auth.user.AuthJobOwner;
 */
public class ConfigurableAuthUserIdentifier implements UserIdentifier {

	/* API SETUP KEYS */
	/* Property name used to set the request header field name of the session */
	public final static String KEY_SESSIONID_HEADER_FIELD = "sessionid_header_field";
	/* Property name used to set the url used for authentication */
	public final static String KEY_AUTH_URL_FIELD = "session_authentication_url";

	/* Property name used to set the name of the key in the authentication URL response, which stores the user's ID*/
	public final static String KEY_RESP_SESSIONID_FIELD = "response_id_field";
	/* Property name used to set the name of the key in the authentication URL response, which stores the username*/
	public final static String KEY_RESP_PSEUDO_FIELD = "response_pseudo_field";
	/* Property name used to set the name of the key in the authentication URL response, which stores the list of allowed tables by the user*/
	public final static String KEY_RESP_ALLOWED_TABLES_FIELD = "response_tables_field";


	/* URL to send authentication requests to verify cookie. Changed in tap.properties under sessionid_header_field */
	private String authURL; 

	/* Field in the request header that contains the session ID, sent to authURL for verification. Changed in tap.properties under session_authentication_url*/
	private String sessionIDHeaderField;

	/* APIClient used for communication with the authentication API which we will send authentication tokens to*/
	private APIClient api;

	/* From the API response the field name of the User ID. Can be changed in tap.properties under response_id_field */
	private String responseUserIDField; 

	/* From the API response the field name of the username. Can be changed in tap.properties under response_pseudo_field */
	private String responsedPseudoField; 

	/* From the API response the field name of the list of allowed tables the user can access. Can be changed in tap.properties under response_tables_field */
	private String responseAllowedTablesField;


	/**
	 * <p>Builds the authenticated API thanks to a given TAP configuration file. The configuration file is used to configure the expected headers for sessions and the authentication API</p>
	 * 
	 * <p>This method should either be called during the custom servlet initialisation or if using {@link ConfigurableTapServlet}, set as the <i>user_identifier</i> in the tap.properties file i.e. <code>user_identifier = tap.auth.ConfigurableUserIdentifier</code></p>
	 * 
	 * @param tapConfig	The content of the TAP configuration file.
	 * 
	 * @throws TAPException		If any required fields are missing in the tap.properties file.
	 * 
	 */
	public ConfigurableAuthUserIdentifier(final Properties tapConfig) throws TAPException {
		this.sessionIDHeaderField = tapConfig.getProperty(KEY_SESSIONID_HEADER_FIELD); 
		this.authURL = tapConfig.getProperty(KEY_AUTH_URL_FIELD);
		this.responseUserIDField = tapConfig.getProperty(KEY_RESP_SESSIONID_FIELD);
		this.responsedPseudoField = tapConfig.getProperty(KEY_RESP_PSEUDO_FIELD);
		this.responseAllowedTablesField = tapConfig.getProperty(KEY_RESP_ALLOWED_TABLES_FIELD);
		// if any of the required fields are missing, throw IllegalArgumentException
		if (this.sessionIDHeaderField == null || this.authURL == null || this.responseUserIDField == null || 
			this.responsedPseudoField == null || this.responseAllowedTablesField == null){ 
			throw new TAPException(" " + KEY_UDFS + ": " + (udfOffset + matcher.start(GROUP_SIGNATURE)) + "-" + (udfOffset + matcher.end(GROUP_SIGNATURE)) + ")");

		}
		
		this.api = new JSONAPIClient(this.authURL, "POST");
	}

	/**
	 * {@inheritDoc}
	 *
	 * The authentication headers will be extracted from the request, which will be up to the servlet or frontend to append using any given method (e.g. cookies)
	 *
	 * @param urlInterpreter	The interpreter of the request URL.
	 * @param request			The request.
	 * 
	 * @return					The owner/user of a given session ID
	 * 
	 * @throws UWSException		If any error occurs while extracting the user ID from the given parameters.
	 * 
	 * @see UWSService#executeRequest(HttpServletRequest, HttpServletResponse)
	 */
	@Override
	public JobOwner extractUserId(UWSUrl urlInterpreter, HttpServletRequest request) throws UWSException {
        JSONObject jsonResponse;
        try{
        	String sessionToken = request.getHeader(this.sessionIDHeaderField);
        	if (sessionToken == null){
        		// throw exception appropriate: "Field "+sessionIDHeaderField +" not in request header"
        	} 

        	HashMap<String, String> authHeaders = new HashMap<String, String>();
        	authHeaders.put(this.sessionIDHeaderField, sessionToken);

        	jsonResponse = (JSONObject) this.api.sendRequest(authHeaders);
        } catch (TAPException TAPe) {
        	// TODO: properly handle this exception
        	TAPe.getHttpErrorCode();
        }
        HashMap<String, Object> permissions = new HashMap<>();
        // Add allowed tables
        ArrayList<TAPTable> allowedTablesFromAPI = new ArrayList<TAPTable>();
        
        JSONArray tablesjson = jsonResponse.getJSONArray(this.responseAllowedTablesField);
        // Loop over json array of tables. Extract Object and convert to string to build a new TAPTable
        for (int i = 0; i<tablesjson.length(); i++){
            allowedTablesFromAPI.add(new TAPTable(tablesjson.getString(i)));
        }
        permissions.put("allowedTables", allowedTablesFromAPI);

        return restoreUser(jsonResponse.getString(this.responseUserIDField), 
        	jsonResponse.getString(this.responsedPseudoField), permissions);
	}

	@Override
	public JobOwner restoreUser(String id, String pseudo, Map<String, Object> otherData) {
		return new AuthJobOwner(id, pseudo, (List<TAPTable>) otherData.get("allowedTables"));
	}
	
}