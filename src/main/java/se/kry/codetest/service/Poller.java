package se.kry.codetest.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class Poller {
    public static final int DEFAULT_TIMEOUT_MS = 3000;
    private final WebClient webClient;

    public Poller(Vertx vertx) {
        this.webClient = WebClient.create(vertx);
    }

    public Future<Boolean> pollService(String url) {
        Future<Boolean> future = Future.future();

        webClient.getAbs(url)
                .timeout(DEFAULT_TIMEOUT_MS)
                .send(req -> future.complete(req.succeeded()));
        return future;
    }
}
