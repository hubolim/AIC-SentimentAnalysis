package at.tuwien.aic;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Server {

    public static void main(String[] args) throws URISyntaxException, IOException {
        ResourceConfig config = new ResourceConfig();
        config.register(Rest.class);
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(new URI("http://localhost:8080/api/"), config);
        server.getServerConfiguration().addHttpHandler(new StaticHttpHandler(new File(".").getAbsolutePath() + "/web"));
        server.start();
        System.in.read();
    }

}
