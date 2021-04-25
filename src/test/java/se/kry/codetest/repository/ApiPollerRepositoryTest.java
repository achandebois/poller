package se.kry.codetest.repository;

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
class ApiPollerRepositoryTest {

    private ServiceRegistryRepository apiPollerRepository;

    @BeforeEach
    void init(Vertx vertx, VertxTestContext testContext) {
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(new JsonObject().put("db_path", "/tmp/poller.db"));
        vertx.deployVerticle(new MainVerticle(), deploymentOptions, testContext.succeeding(id -> testContext.completeNow()));
        apiPollerRepository = new ServiceRegistryRepository(vertx);
    }

    @AfterEach
    void tearDown() {
        apiPollerRepository.truncate();
    }

    @Test
    @DisplayName("should save a service in database")
    void testSaveService(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerRepository.save(url, name, ServiceStatus.UNKNOWN)
                .setHandler(saveQuery -> {
                    testContext.verify(() -> assertNotNull(saveQuery.result()));
                    testContext.completeNow();
                });

    }

    @Test
    @DisplayName("should update a service status in database")
    void testUpdateServiceStatus(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerRepository.save(url, name, ServiceStatus.UNKNOWN)
                .setHandler(saveQuery ->
                        testContext.verify(() -> assertNotNull(saveQuery.result()))
                )
                .compose(id -> apiPollerRepository.updateStatus(id, ServiceStatus.OK))
                .setHandler(updateQuery -> {
                    testContext.verify(() -> assertTrue(updateQuery.result()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should update a service in database")
    void testUpdateService(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerRepository.save(url, name, ServiceStatus.UNKNOWN)
                .setHandler(saveQuery ->
                        testContext.verify(() -> assertNotNull(saveQuery.result()))
                )
                .compose(id ->
                        apiPollerRepository.update(
                                Service.builder()
                                        .id(id)
                                        .url("http://www.my-url.com/" + UUID.randomUUID().toString())
                                        .name(UUID.randomUUID().toString())
                                        .build()
                        )
                )
                .setHandler(updateQuery -> {
                    testContext.verify(() -> assertTrue(updateQuery.result()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should delete a service in database")
    void testDeleteService(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerRepository.save(url, name, ServiceStatus.UNKNOWN)
                .setHandler(saveQuery ->
                        testContext.verify(() -> assertNotNull(saveQuery.result()))
                )
                .compose(id -> apiPollerRepository.delete(id))
                .setHandler(deleteQuery -> {
                    testContext.verify(() -> assertTrue(deleteQuery.result()));
                    testContext.completeNow();
                });
    }

    @Test
    @DisplayName("should find all services in database")
    void testFindAllServices(VertxTestContext testContext) {
        final String url = "http://www.my-url.com/" + UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        apiPollerRepository.save(url, name, ServiceStatus.UNKNOWN)
                .setHandler(saveQuery ->
                        testContext.verify(() -> assertNotNull(saveQuery.result()))
                )
                .compose(id -> apiPollerRepository.findAll())
                .setHandler(findAllQuery -> {
                    testContext.verify(() -> {
                        final List<Service> services = findAllQuery.result();
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
