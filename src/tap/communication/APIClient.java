package tap.communication;
/*
 * This file is part of TAPLibrary.
 *
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2015-2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.Collections;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.io.DataOutputStream;
import java.io.InputStream;
import tap.TAPException;

/**
 * <p>Provides an generic abstract class for acting as a client for an API. Inherit to handle different payload and response data formats</p>
 * 
 * <p>
 * This class serves as a base class for handling API requests and their responses.
 * This inital class defines an existing method for sending data via GET or POST request, and provides default
 * default behaviour for communicating data to and from an API (conversion to byte, assumed to get String response).
 * </p>
 *
 *
 * <p>
 * When extending this class, it is expected the subclass will have a specific data structure/class in mind, with it's own conversion
 * to bytecode and parsing method from String. Thus this class provides abstract functions convertToBytes and convertFromString to define
 * these behaviours. 
 * 
 * Existing classes are included for handling common data formats, including 
 * </p>
 * 
 * 
 * @author Anthony Heng (AAO)
 * @version TBD?
 * @since TBD?
 */
public abstract class APIClient<T> {

	// OVERRIDE IN SUBCLASSES
	/**
	 * Convert the payload to a string representation to a request
	 * @param  payload Data to send as payload
	 * @return         String representation of the payload to send
	 */
	protected abstract String convertToString(T payload);

	/**
	 * Convert the data from a string representation from a response
	 * @param  data Data to be recieved from a request
	 * @return         String representation of the payload to send
	 */
	protected abstract T convertFromString(String data);


	/** URL to communicate with the API. To be set using a config file */
	protected URL url;

	/** Alternate payload encodings can be set in constructor, but utf-8 is 
	 * common enough to be a good default */
	protected String stringEncoding = "UTF-8"; 

	/** http request type (POST or GET). To be converted from string during 
	 * the Constructor*/
	protected String requestMethod;


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds the client to a given URL, and what kind of requests to send, 
	 * and the encoding used for payloads
	 * @param  urlString     URL of the API to communicate with
	 * @param  requestMethod Type of request to send. Either "POST" or "GET"
	 * @param  stringEncoding  Encoding used for payloads
	 */
	public APIClient(String urlString, String requestMethod, String stringEncoding){
		this(urlString, requestMethod);
		this.stringEncoding = stringEncoding;
	}

	/**
	 * Builds the client to a given URL, what kind of requests to send
	 * @param  urlString     URL of the API to communicate with
	 * @param  requestMethod Type of request to send. Either "POST" or "GET"
	 * @param  stringEncoding  Encoding used for payloads
	 */
	public APIClient(String urlString, String requestMethod){
		this.url = new URI(urlString).toURL();
		if (requestMethod.equals("POST") || requestMethod.equals("GET")){
			this.requestMethod = requestMethod;
		} else{
			throw new IllegalArgumentException("requestMethod must be either \"POST\" or \"GET\"");
		}
	}

	/**
	 * Send the request only with no data. Useful for GET requests. 
	 * @return response as object T
	 */
	public T sendRequest(){
		// Attach empty headers
		Map<String, String> headers = Collections.<String, String>emptyMap();
		return convertFromString(getStringResponse(headers, ""));

	}


	/**
	 * Only send header data with request, without any payload 
	 * @param headers Header fields to send to the API URL
	 * @return response as object T
	 */
	public T sendRequest(Map<String, String> headers){
		return convertFromString(getStringResponse(headers, ""));

	}

	/**
	 * Send a request with a payload. The payload will be converted to a String to send through.
	 * The payload will not have any effect if the request method is GET
	 * 
	 * @param headers Header fields to send to the API URL
	 * @param payload payload to send. Only for POST requests
	 * @return response as an object of type T
	 */
	public T sendRequest(Map<String, String> headers, T payload){
		String payloadAsString = convertToString(payload);
		return convertFromString(getStringResponse(headers, payloadAsString));

	}

	/**
	 * Load the data from a given InputStream. Used by getStringResponse for loading the 
	 * error or output streams of a given connection.
	 * @param  inputStream input stream to read data from
	 * @return Data read from the input stream 
	 */
	private String readFromStream(InputStream inputStream){

		StringBuilder sb = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName(this.stringEncoding));
		BufferedReader br = new BufferedReader(reader);

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

		return sb.toString();

	}

	/**
	 * Send data to api. Get response back in string form. 
	 * @param  headers    Header fields to send to the API URL
	 * @param  payloadStr Payload in string format. Will have been converted from format T using convertFromString()
	 * @return            Data from API based on request
	 */
	protected String getStringResponse(Map<String, String> headers, String payloadStr){
			HttpURLConnection conn;
			try{
				// Create URL object
	        	conn = (HttpURLConnection) url.openConnection();
	        
		        // Set request method
		        conn.setRequestMethod(this.requestMethod);


		        // Add all headers to the request
		        for (Map.Entry<String, String> entry : headers.entrySet()) {
	            	conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
				conn.connect();

			} catch (IOException e){
	        	throw new IOException("Failed to connect to API at "+url.toString()+" : "+e.getMessage());
	        }

	        if (this.requestMethod.equals("POST")){
	        	conn.setDoOutput(true); // Enable writing output to the request
	        	// Send payload
				DataOutputStream apiOut = new DataOutputStream(conn.getOutputStream());
				apiOut.writeChars(payloadStr);
				apiOut.flush();
				apiOut.close();
	        } 

			// RESPONSE
            InputStreamReader apiReader; 
            
			// Read response
	        int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300){ // If response is a success code
            	return readFromStream(conn.getInputStream());
            } else{
            	String errorMessage = readFromStream(conn.getErrorStream());
	            // Throw an exception with the error details
	            throw new TAPException("API Communication Error at "+this.url.toString()+": Response code " + responseCode + ": " + errorMessage.toString().trim(), responseCode);
            }
	}

	public String getRequestMethod(){
		return requestMethod;
	}

	public URL getURL(){
		return url;
	}


	public String getStringEncoding(){
		return this.stringEncoding;
	}

}