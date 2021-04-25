package se.kry.codetest.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import se.kry.codetest.MainVerticle;
import se.kry.codetest.model.Service;
import se.kry.codetest.model.ServiceStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({
        VertxExtension.class
})
class BackgroundPollerTest {

    private Poller poller;
    private BackgroundPoller backgroundPoller;
    private ServiceRegistry apiPollerService;

    @BeforeEach
    void init(Vertx vertx, VertxTestContext testContext) {
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(new JsonObject().put("db_path", "/tmp/poller.db"));
        vertx.deployVerticle(new MainVerticle(), deploymentOptions, testContext.succeeding(id -> testContext.completeNow()));        apiPollerService = new ServiceRegistry(vertx);
        poller = Mockito.mock(Poller.class);
        backgroundPoller = new BackgroundPoller(poller, apiPollerService);
    }

    @AfterEach
    void tearDown() {
        apiPollerService.deleteAll();
    }

    @Test
    @DisplayName("should poll services and update them to OK when succeed")
    void testPollWithASuccessfulService(VertxTestContext testContext) {

        Mockito.when(poller.pollService(Mockito.anyString()))
                .thenReturn(Future.succeededFuture(true));

        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerService.create(url, name)
                .setHandler(createRequest ->
                        testContext.verify(() -> assertNotNull(createRequest.result()))
                )
                .compose(id -> backgroundPoller.pollServices())
                .setHandler(pollServicesRequest ->
                        testContext.verify(() -> {
                            final List<Future<Service>> futures = pollServicesRequest.result();
                            assertNotNull(futures);
                            futures.forEach(serviceFuture ->
                                    serviceFuture.setHandler(
                                            event -> testContext.verify(() -> assertEquals(ServiceStatus.OK, event.result().getServiceStatus())))
                            );
                            testContext.completeNow();
                        })
                );
    }

    @Test
    @DisplayName("should poll services and update them to FAILED when fail")
    void testPollWithAFailingService(VertxTestContext testContext) {
        Mockito.when(poller.pollService(Mockito.anyString()))
                .thenReturn(Future.succeededFuture(false));

        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerService.create(url, name)
                .setHandler(createRequest ->
                        testContext.verify(() -> assertNotNull(createRequest.result()))
                )
                .compose(id -> backgroundPoller.pollServices())
                .setHandler(pollServicesRequest ->
                        testContext.verify(() -> {
                            final List<Future<Service>> futures = pollServicesRequest.result();
                            assertNotNull(futures);
                            futures.forEach(serviceFuture ->
                                    serviceFuture.setHandler(
                                            event -> testContext.verify(() -> assertEquals(ServiceStatus.FAILED, event.result().getServiceStatus())))
                            );
                            testContext.completeNow();
                        })
                );
    }
}
