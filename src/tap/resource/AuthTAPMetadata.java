package tap.resource;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import tap.metadata.TAPSchema;
import tap.resource.TAPResource;
import uws.UWSToolBox;
import uk.ac.starlink.votable.VOSerializer;
import tap.resource.TAP;
import tap.metadata.TAPMetadata;
import uws.UWSToolBox;
import uws.job.user.JobOwner;
import java.lang.UnsupportedOperationException;

public class AuthTAPMetadata extends TAPMetadata implements TAPResource {

	/** Resource name of the TAP metadata. This name is also used - in this class - in the TAP URL to identify this resource.
	 * Here it corresponds to the following URI: ".../tables". 
	 * 
	 * NOTE: As a big chunk of vollt still relies on TAPMetadata, authentication will also be needed elsewhere in the code to ensure users can not query
	 * tables they do not have access to. This is just to replace the endpoint /tables.
	 *       
	 * */
	 // Replace the tables resource
	

	public static String RESOURCE_NAME = "tables";

	private TAP tap;

	// To avoid having to change TAPMetadata, cache it all under this hashmap
	private HashMap<String, List<String>> cachedAllowedTables = new HashMap<>();


	
	// Constructor
	public AuthTAPMetadata(TAP tapService){
		super();
		this.tap = tapService;
	}

	@Override
	public boolean executeResource(HttpServletRequest request, HttpServletResponse response) throws IOException{
		response.setContentType("application/xml");
		response.setCharacterEncoding(UWSToolBox.DEFAULT_CHAR_ENCODING);

		PrintWriter writer = response.getWriter();
		// Get the User, copied over from TAP.java
		AuthJobOwner user = null;
		// Identify the user:
		try{
			user = (AuthJobOwner) UWSToolBox.getUser(request, tap.getServiceConnection().getUserIdentifier());
		}catch(UWSException ue){
			getLogger().logTAP(LogLevel.ERROR, null, "IDENT_USER", "Can not identify the HTTP request user!", ue);
			throw new TAPException(ue);
		}
		
		write(writer, user);
		return false;
	}

	// Override methods that used to return all data available and have them throw exceptions
	@Override 
	public void write(final PrintWriter writer) throws IOException{
		throw new UnsupportedOperationException("Attempted to access universal method of TAPMetaData");
		
	}

	@Override 
	public void writeSchema(TAPSchema s, final PrintWriter writer) throws IOException{
		throw new UnsupportedOperationException("Attempted to access universal method of TAPMetaData");
		
	}

	// Use old method, however call writeSchema that filters out the tables the user can't access
	public void write(final PrintWriter writer, AuthJobOwner user) throws IOException{
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		if (xsltPath != null){
			writer.print("<?xml-stylesheet type=\"text/xsl\" ");
			writer.print(VOSerializer.formatAttribute("href", xsltPath));
			writer.println("?>");
		}

		/* TODO The XSD schema for VOSITables should be fixed soon! This schema should be changed here before the library is released!
		 * Note: the XSD schema at http://www.ivoa.net/xml/VOSITables/v1.0 contains an incorrect targetNamespace ("http://www.ivoa.net/xml/VOSICapabilities/v1.0").
		 *       In order to make this XML document valid, a custom location toward a correct XSD schema is used: http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd */
		writer.println("<vosi:tableset xmlns:vosi=\"http://www.ivoa.net/xml/VOSITables/v1.0\" xmlns:vod=\"http://www.ivoa.net/xml/VODataService/v1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VODataService/v1.1 http://www.ivoa.net/xml/VOSITables/v1.0 http://vo.ari.uni-heidelberg.de/docs/schemata/VOSITables-v1.0.xsd\">");
		
		for(TAPSchema s : schemas.values()){
			writeSchema(s, writer, user);
		}

		writer.println("</vosi:tableset>");

		UWSToolBox.flush(writer);
	}

	// Slight difference: each table is checked to see if its owned by the user
	public void writeSchema(final PrintWriter writer, AuthJobOwner user) throws IOException{
		final String prefix = "\t\t";
		writer.println("\t<schema>");

		writeAtt(prefix, "name", s.getRawName(), false, writer);
		writeAtt(prefix, "title", s.getTitle(), true, writer);
		writeAtt(prefix, "description", s.getDescription(), true, writer);
		writeAtt(prefix, "utype", s.getUtype(), true, writer);

		int nbColumns = 0;
		for(TAPTable t : s){
			if (!user.getAllowedTables().contains(t.getFullName())){
				continue; // Don't add this table if it's not in the user's list of allowed tables
			}

			// write each table:
			nbColumns += writeTable(t, writer);

			// flush the PrintWriter buffer when at least 30 tables have been read:
			/* Note: the buffer may have already been flushed before automatically,
			 *       but this manual flush is also checking whether any error has occurred while writing the previous characters.
			 *       If so, a ClientAbortException (extension of IOException) is thrown in order to interrupt the writing of the
			 *       metadata and thus, in order to spare server resources (and particularly memory if the metadata set is large). */
			if (nbColumns / 30 > 1){
				UWSToolBox.flush(writer);
				nbColumns = 0;
			}

		}

		writer.println("\t</schema>");

		if (nbColumns > 0)
			UWSToolBox.flush(writer);
	}

	
}


