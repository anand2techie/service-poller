package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {
    private static int REFRESH_INTERVAL = 1000 * 5;
    private static String SERVICE_CACHE = "/tmp/kry_services.cache";

    private final int refreshInterval;
    private final String serviceCache;

    private HashMap<String, String> services = new HashMap<>();
    private BackgroundPoller poller = new BackgroundPoller();

    public MainVerticle() {
        this.refreshInterval = REFRESH_INTERVAL;
        this.serviceCache = SERVICE_CACHE;
    }

    MainVerticle(int refreshInterval, String serviceCache) {
        this.refreshInterval = refreshInterval;
        this.serviceCache = serviceCache;
    }

    @Override
    public void start(Future<Void> startFuture) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        vertx.setPeriodic(refreshInterval, timerId -> poller.pollServices(getServices()));
        loadServices();
        storeNewService("https://www.kry.se");
        setRoutes(router);
        vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(8080, result -> {
                if (result.succeeded()) {
                    System.out.println("KRY code test service started");
                    startFuture.complete();
                } else {
                    startFuture.fail(result.cause());
                }
            });
    }

    private void setRoutes(Router router) {
        router.route("/").handler(StaticHandler.create());
        router.get("/config").handler(req -> {
            JsonObject config = new JsonObject();
            config.put("refreshInterval", refreshInterval);
            req.response()
                .putHeader("content-type", "application/json")
                .end(config.encode());
        });
        router.get("/service")
            .handler(
                req -> {
                    List<JsonObject> jsonServices =
                        services.entrySet()
                            .stream()
                            .map(
                                service ->
                                    new JsonObject()
                                        .put("name", service.getKey())
                                        .put(
                                            "status",
                                            service.getValue()))
                            .collect(Collectors.toList());
                    req.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonArray(jsonServices).encode());
                });
        router.post("/service")
            .handler(
                req -> {
                    JsonObject jsonBody = req.getBodyAsJson();
                    String newUrl = jsonBody.getString("url");

                    if (!validURL(newUrl)) {
                        req.fail(412);
                        return;
                    }

                    storeNewService(newUrl);
                    req.response().putHeader("content-type", "text/plain").end("OK");
                });
        router.delete("/service/:id")
            .handler(
                ctx -> {
                    System.out.println(ctx.request().getParam("id"));
                    String service = new String(Base64.getDecoder().decode(ctx.request().getParam("id")));
                    if (services.containsKey(service)) {
                        removeService(service);
                        ctx.response().setStatusCode(200).putHeader("content-type", "text/plain").end("OK");
                    } else {
                        ctx.fail(404);
                    }
                }
            );
    }

    void storeNewService(String service) {
        services.putIfAbsent(service, "UNKNOWN");
        cacheServices();
    }

    void removeService(String service) {
        services.remove(service);
        cacheServices();
    }

    HashMap<String, String> getServices() {
        return services;
    }

    void loadServices() {
        ObjectInputStream objectInputStream = null;
        HashMap<String, String> services = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(serviceCache));
            services = (HashMap<String, String>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Couldn't load cached file");
            services = new HashMap<>();
        }

        this.services = services;
    }

    private void cacheServices() {
        try {
            ObjectOutputStream objectOutputStream =
                new ObjectOutputStream(new FileOutputStream(serviceCache));

            objectOutputStream.writeObject(services);
            objectOutputStream.close();
        } catch (IOException e) {
            System.out.println("Could not store service in cache.");
        }
    }

    /**
     * @return boolean whether or not the URL is valid. Can be further extended.
     */
    boolean validURL(String url) {
        try {
            URI uri = new URI(url).parseServerAuthority();
            if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }

        return true;
    }
}
