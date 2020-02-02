package se.kry.codetest;

import io.vertx.core.Future;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.Map;

class BackgroundPoller {
    Future<List<String>> pollServices(Map<String, String> services) {
        services.forEach((url, status) -> services.put(url, pollService(url)));
        return Future.succeededFuture();
    }

    private String pollService(String service) {
        int code;
        try {
            URL url = new URL(service);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.connect();

            code = connection.getResponseCode();
        } catch (SocketTimeoutException e) {
            return "Connection Timeout";
        } catch (IOException e) {
            return "Could not connect";
        }

        return String.valueOf(code);
    }
}
