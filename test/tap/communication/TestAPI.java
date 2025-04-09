package tap.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import tap.communication.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

// Java Program to Set up a Basic HTTP Server
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.io.DataOutputStream;


// Setup basic HTTP server
// Driver Class
public class TestAPI {
	private static HttpServer server;
	private static InetSocketAddress serverAddress;

	private static int TEST_SERVER_PORT_DEFAULT = 8090;

	private static int usePort;
	
	@BeforeClass
	public static void setUp() throws Exception {

		usePort = TEST_SERVER_PORT_DEFAULT;
		while(!portAvailable(usePort)){
			usePort++; // Keep moving up ports until we find one we can use
		}
		serverAddress = new InetSocketAddress("127.0.0.1",usePort);

		// Create an HttpServer instance
        server = HttpServer.create(serverAddress, 0);

        

		// Testing json
		// server.createContext("/json", new HttpHandler() {
		//    public void handle(HttpExchange exchange) throws IOException {
		//        	String response = "TestResponse";
	    //         exchange.sendResponseHeaders(200, response.length());
	    //         OutputStream os = exchange.getResponseBody();
	    //         os.write(response.getBytes());
	    //         os.close();
		//    }
		// });

		// // Testing json with headers
		// server.createContext("/json", new HttpHandler() {
		//    public void handle(HttpExchange exchange) throws IOException {
		//        	String response = "TestResponse";
	    //         exchange.sendResponseHeaders(200, response.length());
	    //         OutputStream os = exchange.getResponseBody();
	    //         os.write(response.getBytes());
	    //         os.close();
		//    }
		// });
        // server.createContext("/json", new JSONResponse());

        // Start the server
        server.setExecutor(null); // Use the default executor
        server.start();

        System.out.println("Server is running at "+serverAddress.getAddress()); // Change this to loggingaddress.toString()

        
	    
    }

    // Check if port available to host test server
    private static boolean portAvailable(int port) throws IllegalStateException {
	    try (Socket ignored = new Socket("localhost", port)) {
	        return false;
	    } catch (ConnectException e) {
	        return true;
	    } catch (IOException e) {
	        throw new IllegalStateException("Error while trying to check open port", e);
	    }
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		server.stop(5);
		
	}

	/**
	 */
	@Test
	public void testDefaultStrGet() throws Exception {
		// For testing str
        server.createContext("/str", new HttpHandler() {
		   public void handle(HttpExchange exchange) throws IOException {
		       	String response = "TestResponse";
	            exchange.sendResponseHeaders(200, response.length());
	           	OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});

		// Start tests
		String serverResponse = "";

		// define a custom HttpHandler
		try {
			// Starting client
			DefaultAPIClient client = new DefaultAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+"/str", "GET");
			// Sending request
			serverResponse = client.sendRequest();
		} catch(Exception e) {
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		}

		// System.out.println((byte)serverResponse.charAt(serverResponse.length()-1));
		assertEquals("TestResponse",serverResponse);
	}

	@Test 
	public void testDefaultStrPost() throws Exception {

		// Test sending text: The text send should be mirrored back
		String requestBody = "TestRequest";


		// Testing str with headers
		server.createContext("/strMirror", new HttpHandler() {
		   public void handle(HttpExchange exchange) throws IOException {

		   		exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

	            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), Charset.forName("UTF-8"));
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(reader);
		        String line;
		        while ((line = br.readLine()) != null) {
		            sb.append(line);
		        }	
		        br.close();
		        String response = sb.toString();
	            exchange.sendResponseHeaders(200, response.length());
	           	OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});
		// Test sending text: The text send should be mirrored back
		try {
			// Starting client
			DefaultAPIClient client = new DefaultAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+"/strMirror", "POST");
			// Sending request
			HashMap<String, List<String>> headers = new HashMap<>();

			serverResponse = client.sendRequest(headers, requestBody);
		} catch(Exception e) {
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		}

		// System.out.println((byte)serverResponse.charAt(serverResponse.length()-1));
		assertEquals(requestBody, serverResponse);
	}


	@Test 
	public void testDefaultStrHeaders() throws Exception {
		// Testing str with headers
		server.createContext("/strheader", new HttpHandler() {
		   public void handle(HttpExchange exchange) throws IOException {
		   		Headers headers = exchange.getRequestHeaders();

		   		StringBuilder sb = new StringBuilder();
		   		sb.append("Headers: ")
		        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
		            String key = entry.getKey();
		            List<String> values = entry.getValue();
		            sb.append(key).append(":").append(String.join(", ", values)).append("\n");
		        }
		        return sb.toString();

	            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), Charset.forName("UTF-8"));
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(reader);
		        String line;
		        while ((line = br.readLine()) != null) {
		            sb.append(line);
		        }	
		        br.close();
		        String response = sb.toString();
	            exchange.sendResponseHeaders(200, response.length());
	            OutputStream os = exchange.getResponseBody();
	            DataOutputStream outStream = new DataOutputStream(os);
				try{
					outStream.writeBytes(response);
					outStream.flush();
				} finally {
					outStream.close();
				}
		   }
		});

		String expectedResponse = "Headers: h1:one, two, three\nfoo:bar"
		try {
			// Starting client
			DefaultAPIClient client = new DefaultAPIClient("http://"+server.getAddress().getHostName()+":"+server.getAddress().getPort()+"/strMirror", "POST");
			// Sending request
			HashMap<String, List<String>> headers = new HashMap<>();
			headers.put("h1", Arrays.asList(new String[]{"one", "two", "three"}));
			headers.put("foo", Arrays.asList(new String[]{"bar"}));

			serverResponse = client.sendRequest(headers);
			

		} catch(Exception e) {
			fail("Failed to initialize and connect to the test server! : " + getPertinentMessage(e));
		}
		assertEquals(expectedResponse, serverResponse);



	}

	public static final String getPertinentMessage(final Exception ex){
		return (ex.getCause() == null || ex.getMessage().equals(ex.getCause().getMessage())) ? ex.getMessage() : ex.getCause().getMessage();
	}


}
