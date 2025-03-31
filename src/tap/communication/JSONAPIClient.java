package tap.communication;
import org.json.JSONObject;

public class JSONAPIClient extends APIClient<JSONObject> {

	public JSONAPIClient(String urlString, String requestMethod, String payloadEncoding){
		super(urlString, requestMethod, payloadEncoding);
	}

	public JSONAPIClient(String urlString, String requestMethod){
		super(urlString, requestMethod);
	}

	@Override
	protected String convertToString(JSONObject payload){
		return payload.toString();
	}

	@Override
	protected JSONObject convertFromString(String data){
		return new JSONObject(data);
	}
}

