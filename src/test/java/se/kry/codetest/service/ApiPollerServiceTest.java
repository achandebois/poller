package se.kry.codetest.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.MainVerticle;
import se.kry.codetest.model.Service;
import se.kry.codetest.model.ServiceStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ApiPollerServiceTest {

    private ServiceRegistry apiPollerService;

    @BeforeEach
    void init(Vertx vertx, VertxTestContext testContext) {
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(new JsonObject().put("db_path", "/tmp/poller.db"));
        vertx.deployVerticle(new MainVerticle(), deploymentOptions, testContext.succeeding(id -> testContext.completeNow()));        apiPollerService = new ServiceRegistry(vertx);
    }

    @AfterEach
    void tearDown() {
        apiPollerService.deleteAll();
    }

    @Test
    @DisplayName("should not create a service with invalid url")
    void testNotCreateServiceBecauseInvalidUrl(VertxTestContext testContext) {
        final String url = "url-not-valid" + UUID.randomUUID().toString();

        apiPollerService.create(url, "not-valid")
                .setHandler(createRequest -> {
                    testContext.verify(() -> assertTrue(createRequest.failed()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should not create a service with null url")
    void testNotCreateServiceBecauseNullUrl(VertxTestContext testContext) {
        final String url = null;
        final String name = null;

        apiPollerService.create(url, name)
                .setHandler(createRequest -> {
                    testContext.verify(() -> assertTrue(createRequest.failed()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should not update a service because null id")
    void testUpdateServiceStatusWithNullId(VertxTestContext testContext) {
        final String id = null;

        apiPollerService.updateStatus(id, ServiceStatus.OK)
                .setHandler(updateRequest -> {
                    testContext.verify(() -> assertTrue(updateRequest.failed()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should not update a service because null status")
    void testUpdateServiceStatusWithNullStatus(VertxTestContext testContext) {
        final String id = UUID.randomUUID().toString();
        final ServiceStatus status = null;

        apiPollerService.updateStatus(id, status)
                .setHandler(updateRequest -> {
                    testContext.verify(() -> assertTrue(updateRequest.failed()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should not delete a service because null id")
    void testNotDeleteService(VertxTestContext testContext) {
        final String id = null;

        apiPollerService.delete(id)
                .setHandler(deleteRequest -> {
                    testContext.verify(() -> assertTrue(deleteRequest.failed()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should create a service")
    void testCreateService(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerService.create(url, name)
                .setHandler(createRequest -> {
                    testContext.verify(() -> assertNotNull(createRequest.result()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should update a service status")
    void testUpdateServiceStatus(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerService.create(url, name)
                .setHandler(createRequest ->
                        testContext.verify(() -> assertNotNull(createRequest.result()))
                )
                .compose(id -> apiPollerService.updateStatus(id, ServiceStatus.OK))
                .setHandler(updateRequest -> {
                    testContext.verify(() -> assertTrue(updateRequest.result()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should update a service")
    void testUpdateService(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerService.create(url, name)
                .setHandler(createRequest ->
                        testContext.verify(() -> assertNotNull(createRequest.result()))
                )
                .compose(id -> apiPollerService.update(
                        Service.builder()
                                .id(id)
                                .url("http://www.my-url.com/" + UUID.randomUUID().toString())
                                .name(UUID.randomUUID().toString())
                                .build()
                ))
                .setHandler(updateRequest -> {
                    testContext.verify(() -> assertTrue(updateRequest.result()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should delete a service")
    void testDeleteService(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerService.create(url, name)
                .setHandler(createRequest ->
                        testContext.verify(() -> assertNotNull(createRequest.result()))
                )
                .compose(id -> apiPollerService.delete(id))
                .setHandler(deleteRequest -> {
                    testContext.verify(() -> assertTrue(deleteRequest.result()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should get all services")
    void testFindAllServices(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerService.create(url, name)
                .setHandler(createRequest ->
                        testContext.verify(() -> assertNotNull(createRequest.result()))
                )
                .compose(id -> apiPollerService.getAll())
                .setHandler(getAllRequest -> {
                    testContext.verify(() -> {
                        final List<Service> services = getAllRequest.result();
                        assertNotNull(services);
                        assertEquals(1, services.size());
                        assertEquals(url, services.get(0).getUrl());
                        assertEquals(ServiceStatus.UNKNOWN, services.get(0).getServiceStatus());
                        assertNotNull(services.get(0).getCreationDate());
                        assertNotNull(services.get(0).getId());
                    });
                    testContext.completeNow();
                });
    }
}
