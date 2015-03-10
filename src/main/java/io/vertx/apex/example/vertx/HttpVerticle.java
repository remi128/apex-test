package io.vertx.apex.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

public class HttpVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(HttpVerticle.class);
    
    @Override
    public void start(Future startFuture) throws Exception {
  
        HttpServer httpServer = vertx
                .createHttpServer(new HttpServerOptions().setPort(8080))
                .requestHandler(   req -> {
                    req.response().end(req.path());
                });
    
        httpServer.listen(status -> {
            if (status.succeeded()) {
                startFuture.complete();
                return;
            }
            startFuture.fail(status.cause());
        });
    }
}
