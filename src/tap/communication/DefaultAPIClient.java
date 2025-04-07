package tap.communication;
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

/**
 *
 * <p> 
 * Subclass of {@link APIClient}. Default implementation which handles the raw string data of the response and request bodies.
 * </p>
 * 
 * @author Anthony Heng (AAO)
 * @version 04/2025
 */
public class DefaultAPIClient extends APIClient<String> {
	/**
	 * Create a APIClient for a given URL, and what kind of requests to send, 
	 * and the encoding used for payloads
	 * @param  urlString     URL of the API to communicate with
	 * @param  requestMethod Type of request to send. Either "POST" or "GET"
	 * @param  stringEncoding  Encoding used for payloads
	 */
	public DefaultAPIClient(String urlString, String requestMethod, String payloadEncoding){
		super(urlString, requestMethod, payloadEncoding);
	}
	/**
	 * Create a APIClient for a given URL, what kind of requests to send
	 * @param  urlString     URL of the API to communicate with
	 * @param  requestMethod Type of request to send. Either "POST" or "GET"
	 * @param  stringEncoding  Encoding used for payloads
	 */
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

