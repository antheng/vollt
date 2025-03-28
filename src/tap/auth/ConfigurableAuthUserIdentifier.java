package tap.auth;
import uws.service.UserIdentifier;
import java.util.Properties;
import uws.job.user.JobOwner;
import uws.UWSException;
import uws.service.UWSUrl;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import org.json.simple.JSONObject;



public class ConfigurableAuthUserIdentifier implements UserIdentifier {

	/* URL to send authentication requests to verify cookie */
	private String authURL; 

	/* Field in the request header that contains the session ID, sent to authURL for verification */
	private String sessionIDHeaderField;

	private APIClient api;

	private String responseIDField; 

	private String responsedPseudoField; 

	private String responseAllowedTablesField;



	public ConfigurableAuthUserIdentifier(final Properties tapConfig){
		// TODO: How do we handle configs without defaults? Look around in vollt
		this.sessionIDHeaderField = tapConfig.getProperty("sessionid_header_field","session-id");
		this.authURL = tapConfig.getProperty("session_authentication_url");
		this.userIDResponseField = tapConfig.getProperty("response_id_field");
		this.usernameResponseField = tapConfig.getProperty("response_pseudo_field");
		this.responseAllowedTablesField = tapConfig.getProperty("response_allowed_tables_field");
		
		this.api = JSONAPIClient(this.authURL);
	}

	@Override
	public JobOwner extractUserId(UWSUrl urlInterpreter, HttpServletRequest request) throws UWSException {
        JSONObject jsonResponse;
        try{
        	String sessionToken = request.getHeader(this.sessionIDHeaderField);
        	if (sessionToken == null){
        		// throw exception appropriate: "Field "+sessionIDHeaderField +" not in request header"
        		// Or should it just return the anonymous user?
        	} 

        	if (this.supportsAnonymous && sessionToken.equals(this.ANONYMOUS_ID)){
        		return anonymous;
        	}

        	HashMap<String, String> authHeaders = new HashMap<String, String>();
        	authHeaders.put(this.sessionIDHeaderField, sessionToken);

        	jsonResponse = this.api.sendRequest(authHeaders);
        } catch (UWSException e) {
        	
        }
        HashMap<String, Object> permissions = new HashMap<>();
        // Add allowed tables
        ArrayList<String> allowedTablesFromAPI = new ArrayList<String>();

        for (JsonString elm: jsonResponse.getJsonArray(this.allowedTablesResponseField).getValuesAs(JsonString.class)){
            allowedTablesFromAPI.add(elm.getString());
        }
        permissions.put("allowedTables", allowedTablesFromAPI);

        return restoreUser(jsonResponse.getString(this.userIDResponseField), jsonResponse.getString(usernameResponseField), permissions);
	}

	@Override
	public JobOwner restoreUser(String id, String pseudo, Map<String, Object> otherData) {
		return AuthJobOwner(id, pseudo, (List<String>) otherData.get("allowedTables"));
	}
	
}