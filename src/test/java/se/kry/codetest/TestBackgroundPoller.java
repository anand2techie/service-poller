package se.kry.codetest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBackgroundPoller {
    BackgroundPoller backgroundPoller = new BackgroundPoller();

    @Test
    @DisplayName("Poll a non functional website")
    void pollNonFunctionalWebsite() {
        Map<String, String> services = new HashMap<String, String>();
        services.put("http://does-not-exist", "UNKNOWN");
        backgroundPoller.pollServices(services);

        assertEquals("could not connect", services.get("http://does-not-exist").toLowerCase());
    }

    @Test
    @DisplayName("Poll a slow website")
    void pollSlowWebsite() throws IOException {
        int port = startServer(5 * 1000);
        Map<String, String> services = new HashMap<String, String>();
        services.put("http://localhost:" + port, "UNKNOWN");
        backgroundPoller.pollServices(services);

        assertEquals("connection timeout", services.get("http://localhost:" + port).toLowerCase());
    }

    @Test
    @DisplayName("Poll a functional website")
    void pollFunctionalWebsite() throws IOException {
        int port = startServer(0);
        Map<String, String> services = new HashMap<String, String>();
        services.put("http://localhost:" + port, "UNKNOWN");
        backgroundPoller.pollServices(services);

        assertEquals("200", services.get("http://localhost:" + port).toLowerCase());
    }

    private int startServer(long delay) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new TestHandler(delay));
        server.setExecutor(null); // creates a default executor
        server.start();
        return server.getAddress().getPort();
    }

    static class TestHandler implements HttpHandler {
        private long delay;

        TestHandler(long delay) {
            this.delay = delay;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
            t.sendResponseHeaders(200, "Echo".length());
            OutputStream os = t.getResponseBody();
            os.write("Echo".getBytes());
            os.close();
        }
    }
}
