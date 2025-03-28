
public class DefaultAPIClient extends APIClient<String> {

	public DefaultAPIClient(String urlString, String requestMethod, String payloadEncoding){
		super(urlString, requestMethod, payloadEncoding);
	}

	public DefaultAPIClient(String urlString, String requestMethod){
		super(urlString, requestMethod);
	}

	@Override
	protected String convertToString(String payload){
		return payload;
	}

	@Override
	protected String convertFromString(String data){
		return data;
	}
}

