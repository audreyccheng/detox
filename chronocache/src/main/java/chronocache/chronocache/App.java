package chronocache.chronocache;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.*;

/**
 * Hello world!
 *
 */
public class App 
{
    @SuppressWarnings("restriction")
	public static void main( String[] args ) throws IllegalArgumentException, IOException
    {
    	ResourceConfig resourceConfig = new PackagesResourceConfig("chronocache.rest");
		HttpServer httpServer =  HttpServerFactory.create(getURI(), resourceConfig);
	    httpServer.start();
	    System.out.println(String.format("\nJersey Application Server started with WADL available at " + "%sapplication.wadl\n", getURI()));
    }
    
    private static URI getURI() {
        return UriBuilder.fromUri("http://" + getHostName() + "/").port(8085).build();
    }
	 
    private static String getHostName() {
        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return hostName;
    }
}
