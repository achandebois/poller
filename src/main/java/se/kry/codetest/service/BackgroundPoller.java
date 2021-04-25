package se.kry.codetest.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import se.kry.codetest.model.Service;
import se.kry.codetest.model.ServiceStatus;

import java.util.ArrayList;
import java.util.List;

import static se.kry.codetest.model.ServiceStatus.FAILED;
import static se.kry.codetest.model.ServiceStatus.OK;

@Slf4j
public class BackgroundPoller {

    private final Poller poller;
    private final ServiceRegistry apiPollerService;

    public BackgroundPoller(Vertx vertx) {
        this.poller = new Poller(vertx);
        this.apiPollerService = new ServiceRegistry(vertx);
    }

    BackgroundPoller(Poller poller, ServiceRegistry apiPollerService) {
        this.poller = poller;
        this.apiPollerService = apiPollerService;
    }

    public Future<List<Future<Service>>> pollServices() {
        List<Future<Service>> futures = new ArrayList<>();
        Future<List<Future<Service>>> future = Future.future();

        apiPollerService.getAll()
                .setHandler(services -> {
                    services.result()
                            .forEach(svc -> {
                                final Future<Service> fut = pollService(svc);
                                futures.add(fut);
                            });
                    future.complete(futures);
                });
        return future;
    }

    private Future<Service> pollService(Service svc) {
        Future<Service> future = Future.future();

        poller.pollService(svc.getUrl())
                .setHandler(pollRequest -> {
                            if (pollRequest.failed()) {
                                log.error("An error occurred while polling service {}", svc.toString(), pollRequest.cause());
                            }
                            ServiceStatus status = Boolean.TRUE.equals(pollRequest.result()) ? OK : FAILED;
                            apiPollerService.updateStatus(svc.getId(), status);
                            future.complete(svc.withServiceStatus(status));
                        }
                );
        return future;
    }
}
