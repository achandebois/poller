package se.kry.codetest.migrate;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import se.kry.codetest.repository.ServiceRegistryRepository;

@Slf4j
public class DBMigration {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        createDatabase(vertx);
    }

    public static void createDatabase(Vertx vertx) {
        ServiceRegistryRepository apiPollerRepository = new ServiceRegistryRepository(vertx);
        apiPollerRepository.createDb()
                .setHandler(done -> {
                    if (done.succeeded()) {
                        log.info("completed db migrations");
                    } else {
                        log.error("An error occurred while creating database", done.cause());
                    }
                    vertx.close(shutdown -> System.exit(0));
                });
    }


}
