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


public class ConfigurableAuthUserIdentifier implements UserIdentifier {

	/* URL to send authentication requests to verify cookie */
	private String authURL; 

	/* Field in the request header that contains the session ID, sent to authURL for verification */
	private String sessionIDHeaderField;

	private APIClient api;

	private String responseUserIDField; 

	private String responsedPseudoField; 

	private String responseAllowedTablesField;



	public ConfigurableAuthUserIdentifier(final Properties tapConfig){
		// TODO: How do we handle configs without defaults? Look around in vollt
		this.sessionIDHeaderField = tapConfig.getProperty("sessionid_header_field","session-id");
		this.authURL = tapConfig.getProperty("session_authentication_url");
		this.responseUserIDField = tapConfig.getProperty("response_id_field");
		this.responsedPseudoField = tapConfig.getProperty("response_pseudo_field");
		this.responseAllowedTablesField = tapConfig.getProperty("response_allowed_tables_field");
		
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
        } catch (UWSException e) {
        	// TODO: properly handle this exception
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