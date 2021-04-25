package se.kry.codetest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;
import se.kry.codetest.mapper.ServiceMapper;
import se.kry.codetest.model.Service;
import se.kry.codetest.service.ServiceRegistry;
import se.kry.codetest.service.BackgroundPoller;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static se.kry.codetest.mapper.ServiceMapper.toService;

@Slf4j
public class MainVerticle extends AbstractVerticle {

    public static final int PERIOD_IN_MINUTES = 60 * 1000;
    public static final String SERVICES_BASE_API_PATH = "/api/v1/services";
    private ServiceRegistry apiPollerService;

    @Override
    public void start(Future<Void> startFuture) {
        apiPollerService = new ServiceRegistry(vertx);
        apiPollerService.createDb();
        final BackgroundPoller poller = new BackgroundPoller(vertx);
        vertx.setPeriodic(PERIOD_IN_MINUTES, timerId -> poller.pollServices());

        Router router = createRouter(vertx);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        log.info("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private Router createRouter(Vertx vertx) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        registerRoutes(router);

        return router;
    }

    private void registerRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        registerGetServiceRoute(router);
        registerPostServiceRoute(router);
        registerDeleteServiceRoute(router);
        registerUpdateServiceRoute(router);
    }

    private void registerGetServiceRoute(Router router) {
        router.get(SERVICES_BASE_API_PATH)
                .handler(req ->
                        apiPollerService.getAll()
                                .setHandler(getAllServices -> {
                                    if (getAllServices.failed()) {
                                        errorResponse(req, HttpResponseStatus.INTERNAL_SERVER_ERROR, "An error occurred while getting all services");
                                    } else {
                                        List<JsonObject> jsonServices = ServiceMapper.toJsonObjects(getAllServices.result());
                                        req.response()
                                                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                                                .setStatusCode(HttpResponseStatus.OK.code())
                                                .end(new JsonArray(jsonServices).encode());
                                    }
                                })
                );
    }

    private void registerPostServiceRoute(Router router) {
        router.post(SERVICES_BASE_API_PATH)
                .handler(req -> {
                    JsonObject jsonBody = req.getBodyAsJson();

                    if (!jsonBody.containsKey("url")) {
                        errorResponse(req, HttpResponseStatus.BAD_REQUEST, "url parameter is required");
                    }
                    apiPollerService.create(jsonBody.getString("url"), jsonBody.getString("name", ""))
                            .setHandler(createRequest -> {
                                if (createRequest.failed()) {
                                    errorResponse(req, HttpResponseStatus.INTERNAL_SERVER_ERROR, "An error occurred while creating service: " + createRequest.cause().getMessage());
                                } else {
                                    req.response()
                                            .setStatusCode(HttpResponseStatus.CREATED.code())
                                            .setStatusMessage(createRequest.result())
                                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .end();
                                }
                            });
                });
    }

    private void registerDeleteServiceRoute(Router router) {
        router.delete(SERVICES_BASE_API_PATH + "/:id")
                .handler(req -> {
                    final String id = req.pathParam("id");

                    apiPollerService.delete(id)
                            .setHandler(updateResponseHandler(req, "An error occurred while deleting service: "));
                });
    }

    private void registerUpdateServiceRoute(Router router) {
        router.put(SERVICES_BASE_API_PATH + "/:id")
                .handler(req -> {
                    final String id = req.pathParam("id");

                    final JsonObject jsonBody = req.getBodyAsJson();

                    if (!jsonBody.containsKey("url")) {
                        errorResponse(req, HttpResponseStatus.BAD_REQUEST, "url parameter is required");
                    } else {
                        final Service service = toService(id, req.getBodyAsJson());

                        apiPollerService.update(service)
                                .setHandler(updateResponseHandler(req, "An error occurred while updating service: "));
                    }
                });
    }

    private void errorResponse(RoutingContext req, HttpResponseStatus badRequest, String s) {
        req.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(badRequest.code())
                .setStatusMessage(s)
                .end();
    }

    private Handler<AsyncResult<Boolean>> updateResponseHandler(RoutingContext req, String s) {
        return updateRequest -> {
            if (updateRequest.failed()) {
                req.response()
                        .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                        .setStatusMessage(s + updateRequest.cause().getMessage())
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end();
            } else {
                int statusCode = HttpResponseStatus.NOT_FOUND.code();
                if (Boolean.TRUE.equals(updateRequest.result())) {
                    statusCode = HttpResponseStatus.NO_CONTENT.code();
                }
                req.response()
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(statusCode)
                        .end();
            }
        };
    }
}



