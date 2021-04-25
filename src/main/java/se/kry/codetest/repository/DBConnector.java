package se.kry.codetest.repository;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.UpdateResult;

public class DBConnector {

    private static final String DB_PATH = "poller.db";
    private SQLClient client;

    public DBConnector(Vertx vertx) {

        ConfigRetriever retriever = ConfigRetriever.create(vertx);

        retriever.getConfig(conf -> {
            String dbPath = DB_PATH;
            if (conf.succeeded() && conf.result().containsKey("db_path")) {
                dbPath = conf.result().getString("db_path");
            }
            JsonObject config = new JsonObject()
                    .put("url", "jdbc:sqlite:" + dbPath)
                    .put("driver_class", "org.sqlite.JDBC")
                    .put("max_pool_size", 30);

            client = JDBCClient.createShared(vertx, config);
        });
    }

    public Future<ResultSet> query(String query) {
        return query(query, new JsonArray());
    }

    public Future<ResultSet> query(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        Future<ResultSet> queryResultFuture = Future.future();

        client.queryWithParams(query, params, result -> {
            if (result.failed()) {
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
            }
        });
        return queryResultFuture;
    }

    public Future<UpdateResult> update(String update) {
        return update(update, new JsonArray());
    }

    public Future<UpdateResult> update(String update, JsonArray params) {
        if (update == null || update.isEmpty()) {
            return Future.failedFuture("Update is null or empty");
        }
        if (!update.endsWith(";")) {
            update = update + ";";
        }

        Future<UpdateResult> queryResultFuture = Future.future();

        client.updateWithParams(update, params, result -> {
            if (result.failed()) {
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
            }
        });
        return queryResultFuture;
    }
}
