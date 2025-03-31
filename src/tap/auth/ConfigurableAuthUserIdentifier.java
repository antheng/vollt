package tap.auth;
import uws.service.UserIdentifier;
import tap.communication.APIClient;
import tap.communication.JSONAPIClient;
import tap.metadata.TAPTable;
import java.util.Properties;
import uws.job.user.JobOwner;
import uws.UWSException;
import uws.service.UWSUrl;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import tap.TAPException;


public class ConfigurableAuthUserIdentifier implements UserIdentifier {

	public final static String KEY_SESSIONID_HEADER_FIELD = "sessionid_header_field";
	public final static String KEY_AUTH_URL_FIELD = "session_authentication_url";
	public final static String KEY_RESP_SESSIONID_FIELD = "response_id_field";
	public final static String KEY_RESP_PSEUDO_FIELD = "response_pseudo_field";
	public final static String KEY_RESP_ALLOWED_TABLES_FIELD = "response_pseudo_field";


	/* URL to send authentication requests to verify cookie */
	private String authURL; 

	/* Field in the request header that contains the session ID, sent to authURL for verification */
	private String sessionIDHeaderField;

	private APIClient api;

	private String responseUserIDField; 

	private String responsedPseudoField; 

	private String responseAllowedTablesField;



	public ConfigurableAuthUserIdentifier(final Properties tapConfig) throws TAPException{
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