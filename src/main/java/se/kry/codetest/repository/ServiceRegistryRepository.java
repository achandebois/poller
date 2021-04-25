package se.kry.codetest.repository;

import com.fasterxml.uuid.Generators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import se.kry.codetest.model.Service;
import se.kry.codetest.model.ServiceStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceRegistryRepository {

    private final DBConnector connector;

    public ServiceRegistryRepository(Vertx vertx) {
        this.connector = new DBConnector(vertx);
    }

    public Future<Boolean> createDb() {
        final Future<ResultSet> createDbQuery = connector.query("CREATE TABLE IF NOT EXISTS service (" +
                "id CHAR(128) PRIMARY KEY NOT NULL UNIQUE," +
                "name VARCHAR(128) UNIQUE," +
                "url VARCHAR(128) NOT NULL UNIQUE," +
                "status VARCHAR(12) NOT NULL," +
                "creation_date INTEGER NOT NULL " +
                ")");

        Future<Boolean> createDbFuture = Future.future();
        createDbQuery.setHandler(createDbQueryResult -> {
            if (createDbQueryResult.failed()) {
                createDbFuture.fail(createDbQueryResult.cause());
            } else {
                createDbFuture.complete(true);
            }
        });

        return createDbFuture;
    }

    public Future<Boolean> truncate() {
        final Future<ResultSet> deleteQuery = connector.query("DELETE FROM service");

        Future<Boolean> deleteFuture = Future.future();
        deleteQuery.setHandler(queryResult -> {
            if (queryResult.failed()) {
                deleteFuture.fail(queryResult.cause());
            } else {
                deleteFuture.complete(true);
            }
        });

        return deleteFuture;
    }

    public Future<List<Service>> findAll() {
        final Future<ResultSet> selectQuery = connector.query("SELECT id, name, url, status, creation_date FROM service LIMIT 100");

        Future<List<Service>> selectFuture = Future.future();
        selectQuery.setHandler(queryResult -> {
            if (queryResult.failed()) {
                selectFuture.fail(queryResult.cause());
            } else {
                final List<Service> services = queryResult.result()
                        .getRows()
                        .stream()
                        .map(row -> new Service(
                                        row.getString("id"),
                                        row.getString("name"),
                                        row.getString("url"),
                                        ServiceStatus.valueOf(row.getString("status")),
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(row.getLong("creation_date")), ZoneId.systemDefault())
                                )
                        ).collect(Collectors.toList());
                selectFuture.complete(services);
            }
        });

        return selectFuture;
    }

    public Future<String> save(String url, String name, ServiceStatus serviceStatus) {
        JsonArray jsonArray = new JsonArray();
        final String id = Generators.timeBasedGenerator().generate().toString();
        jsonArray.add(id);
        jsonArray.add(name != null ? name : "");
        jsonArray.add(url);
        jsonArray.add(serviceStatus);
        jsonArray.add(Instant.now(Clock.systemDefaultZone()));
        final Future<UpdateResult> saveQuery = connector.update("INSERT INTO service (id, name, url, status, creation_date) VALUES(?,?,?,?,?)", jsonArray);

        Future<String> saveFuture = Future.future();
        saveQuery.setHandler(saveResult -> {
            if (saveResult.failed()) {
                saveFuture.fail(saveResult.cause());
            } else {
                saveFuture.complete(id);
            }
        });

        return saveFuture;
    }

    public Future<Boolean> update(Service service) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(service.getUrl());
        jsonArray.add(service.getName());
        jsonArray.add(ServiceStatus.UNKNOWN);
        jsonArray.add(service.getId());
        final Future<UpdateResult> updateQuery = connector.update("UPDATE service SET url = ?, name = ?, status = ?  WHERE id = ?", jsonArray);

        return toBooleanFuture(updateQuery);
    }

    public Future<Boolean> updateStatus(String id, ServiceStatus status) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(status);
        jsonArray.add(id);
        final Future<UpdateResult> updateQuery = connector.update("UPDATE service SET status = ? WHERE id = ?", jsonArray);

        return toBooleanFuture(updateQuery);
    }

    public Future<Boolean> delete(String id) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(id);
        final Future<UpdateResult> deleteQuery = connector.update("DELETE FROM service WHERE id = ?", jsonArray);

        return toBooleanFuture(deleteQuery);
    }


    private Future<Boolean> toBooleanFuture(Future<UpdateResult> updateQuery) {
        Future<Boolean> updateFuture = Future.future();
        updateQuery.setHandler(updateResult -> {
            if (updateResult.failed()) {
                updateFuture.fail(updateResult.cause());
            } else {
                updateFuture.complete(updateResult.result().getUpdated() == 1);
            }
        });
        return updateFuture;
    }
}
