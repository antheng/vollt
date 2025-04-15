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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import tap.TAPException;
import tap.communication.APIClient;
import tap.communication.JSONAPIClient;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.auth.AuthJobOwner;

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
	public final static String KEY_SESSIONID_HEADER_FIELD = "auth_header_field";
	/* Property name used to set the url used for authentication */
	public final static String KEY_AUTH_URL_FIELD = "session_authentication_url";

	/* Property name used to set the name of the key in the authentication URL response, which stores the user's ID*/
	public final static String KEY_RESP_SESSIONID_FIELD = "response_id_field";
	/* Property name used to set the name of the key in the authentication URL response, which stores the username*/
	public final static String KEY_RESP_PSEUDO_FIELD = "response_pseudo_field";
	/* Property name used to set the name of the key in the authentication URL response, which stores the list of allowed schemas and tables by the user*/
	public final static String KEY_RESP_ALLOWED_ACCESS_FIELD = "response_allowed_access_field";


	/* URL to send authentication requests to verify token. Changed in tap.properties under sessionid_header_field */
	private String authURL; 

	/* Field in the request header that contains the session ID, sent to authURL for verification. Changed in tap.properties under 
	 * session_authentication_url*/
	private String sessionIDHeaderField;

	/* APIClient used for communication with the authentication API which we will send authentication tokens to*/
	private APIClient api;

	/* From the API response the field name of the User ID. Can be changed in tap.properties under response_id_field */
	private String responseUserIDField; 

	/* From the API response the field name of the username. Can be changed in tap.properties under response_pseudo_field */
	private String responsedPseudoField; 

	/* From the API response the field name of the list of allowed schemas and tables the user can access. Can be changed in tap.properties 
	 * under response_tables_field */
	private String responseAllowedDataField;


	/**
	 * <p>Builds the authenticated API thanks to a given TAP configuration file. The configuration file is used to configure the 
	 * expected headers for sessions and the authentication API</p>
	 * 
	 * <p>This method should either be called during the custom servlet initialisation or if using {@link ConfigurableTapServlet}, 
	 * set as the <i>user_identifier</i> in the tap.properties file i.e. 
	 * <code>user_identifier = tap.auth.ConfigurableUserIdentifier</code></p>
	 * 
	 * @param tapConfig	The content of the TAP configuration file.
	 * 
	 * @throws UWSException		If any required fields are missing in the tap.properties file, or any errors occur trying to initialize the API client
	 * 
	 */
	public ConfigurableAuthUserIdentifier(final Properties tapConfig) throws UWSException{
		this.sessionIDHeaderField = tapConfig.getProperty(KEY_SESSIONID_HEADER_FIELD); 
		this.authURL = tapConfig.getProperty(KEY_AUTH_URL_FIELD);
		this.responseUserIDField = tapConfig.getProperty(KEY_RESP_SESSIONID_FIELD);
		this.responsedPseudoField = tapConfig.getProperty(KEY_RESP_PSEUDO_FIELD);
		this.responseAllowedDataField = tapConfig.getProperty(KEY_RESP_ALLOWED_ACCESS_FIELD);
		// if any of the required fields are missing, throw IllegalArgumentException
		if (this.sessionIDHeaderField == null || this.authURL == null || this.responseUserIDField == null || 
			this.responsedPseudoField == null || this.responseAllowedDataField == null){ 
			// TODO: need a different exception I think
			throw new UWSException("Missing parameters "+ 
				String.join(", ", KEY_SESSIONID_HEADER_FIELD, KEY_AUTH_URL_FIELD, KEY_RESP_SESSIONID_FIELD, KEY_RESP_PSEUDO_FIELD,KEY_RESP_ALLOWED_ACCESS_FIELD)+
				" to setup auth in tap.properties");
		}
		try{
			this.api = new JSONAPIClient(this.authURL, "POST");
		} catch (Exception e){
			throw new UWSException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * The authentication headers will be extracted from the request, which will be up to the servlet or frontend to append using any given method (e.g. cookies)
	 *
	 * The response body is expected to be a json object with a "allowedAccess"
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
	public AuthJobOwner extractUserId(UWSUrl urlInterpreter, HttpServletRequest request) throws UWSException {
        JSONObject jsonResponse;
        try{
        	String sessionToken = request.getHeader(this.sessionIDHeaderField);
        	if (sessionToken == null){
        		throw new ServletException("Authentication header missing from request");
        	} 
        	HashMap<String, String> authHeaders = new HashMap<String, String>();
        	authHeaders.put(this.sessionIDHeaderField, sessionToken);
        	jsonResponse = (JSONObject) this.api.sendRequest(authHeaders);
        } catch (Exception e) {
			throw new UWSException(e);
		}
        
        HashMap<String, Object> permissions = new HashMap<>();
        // Add allowed access information
        ArrayList<TAPSchema> allowedDataFromAPI = new ArrayList<TAPSchema>();
        
        // Assumed: array of schema json objects
        JSONObject accessjson = jsonResponse.getJSONObject(this.responseAllowedDataField);
        for (String schemaName : accessjson.keySet()){ // Contained object is a struct of schemas, which are keys to table lists. See doc for an example.
        	TAPSchema schemaToAdd = new TAPSchema(schemaName);
        	JSONArray tableNamesArr = accessjson.getJSONArray(schemaName);
        	for (int i = 0; i<tableNamesArr.length(); i++)
	            schemaToAdd.addTable(tableNamesArr.getString(i));
	        allowedDataFromAPI.add(schemaToAdd);

        }
        
        // Loop over json array of tables. Extract Object and convert to string to build a new TAPSchema
        permissions.put("allowedData", allowedDataFromAPI);

        return restoreUser(jsonResponse.getString(this.responseUserIDField), 
        	jsonResponse.getString(this.responsedPseudoField), permissions);
	}

	@Override
	public AuthJobOwner restoreUser(String id, String pseudo, Map<String, Object> otherData) {
		return new AuthJobOwner(id, pseudo, (List<TAPSchema>) otherData.get("allowedData"));
	}
	
}