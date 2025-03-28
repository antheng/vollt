package tap.communication;
import org.json.simple.JSONObject;

public class JSONAPIClient extends APIClient<JSONObject> {

	public JSONAPIClient(String urlString, String requestMethod, String payloadEncoding){
		super(urlString, requestMethod, payloadEncoding);
	}

	public JSONAPIClient(String urlString, String requestMethod){
		super(urlString, requestMethod);
	}

	@Override
	protected byte[] convertToBytes(JSONObject payload){
		return JSONObject.toString().getBytes(Charset.forName(this.textEncoding));
	}

	@Override
	protected JSONObject convertFromString(String data){
		return new JSONObject(data);
	}
}

