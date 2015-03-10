/*
 * Copyright 2014 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 and Apache License v2.0 which accompanies this distribution. The
 * Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html The Apache License v2.0 is available
 * at http://www.opensource.org/licenses/apache2.0.php You may elect to redistribute this code under either of these
 * licenses.
 */

package io.vertx.apex.example.apex;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Route;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.handler.AuthHandler;
import io.vertx.ext.apex.handler.BodyHandler;
import io.vertx.ext.apex.handler.CookieHandler;
import io.vertx.ext.apex.handler.FormLoginHandler;
import io.vertx.ext.apex.handler.RedirectAuthHandler;
import io.vertx.ext.apex.handler.SessionHandler;
import io.vertx.ext.apex.handler.StaticHandler;
import io.vertx.ext.apex.handler.TemplateHandler;
import io.vertx.ext.apex.handler.TimeoutHandler;
import io.vertx.ext.apex.sstore.LocalSessionStore;
import io.vertx.ext.apex.sstore.SessionStore;
import io.vertx.ext.apex.templ.ThymeleafTemplateEngine;
import io.vertx.ext.apex.templ.impl.ThymeleafTemplateEngineImpl;
import io.vertx.ext.auth.AuthService;
import io.vertx.ext.auth.shiro.PropertiesAuthRealmConstants;
import io.vertx.ext.auth.shiro.impl.MongoAuthRealmImpl;
import io.vertx.ext.auth.shiro.impl.ShiroAuthServiceImpl;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HelloWorldApexVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(HelloWorldApexVerticle.class);

  @Override
  public void start() {
    System.out.println("starting");

    Router router = Router.router(vertx);
    installSessionStore(router);
    installAuth(router);

    installStatic(router);
    installTemplateHandler(router);

    addMultiRouts(router);
    addParameterRoute(router);
    routeByRegex(router);
    mountSubRouter(router);

    addFailureHandler(router);

    addGlobaleRoute(router);

    HttpServerOptions options = new HttpServerOptions().setPort(8080);

    HttpServer server = vertx.createHttpServer(options);
    server.requestHandler(router::accept).listen();
  }

  /**
   * http://localhost:8080/private/somepath
   * 
   * @param router
   */
  private void installAuth(Router router) {
    JsonObject config = new JsonObject();
    config.put(PropertiesAuthRealmConstants.PROPERTIES_PROPS_PATH_FIELD, "classpath:test-auth.properties");

    // AuthService authService = ShiroAuthService.create(vertx, ShiroAuthRealmType.PROPERTIES, config);

    MongoAuthRealmImpl mongoRealm = new MongoAuthRealmImpl(vertx);
    AuthService authService = new ShiroAuthServiceImpl(vertx, mongoRealm, config);

    AuthHandler redirectAuthHandler = RedirectAuthHandler.create(authService, "/static/loginpage.html");

    // All requests to paths starting with '/private/' will be protected
    router.route("/private/").handler(redirectAuthHandler);

    // Handle the actual login
    BodyHandler bh = BodyHandler.create();
    router.route("/login").handler(bh);
    router.route("/login").handler(FormLoginHandler.create(authService));

    router.route("/private/somepath").handler(routingContext -> {

      // This will require a login

        // This will have the value true
        boolean isLoggedIn = routingContext.session().isLoggedIn();
        System.out.println("logged in: " + isLoggedIn);
        routingContext.response().putHeader("content-type", "text/html")
            .end("user: " + routingContext.session().getLoginID() + "<br/><a href='logout'>logout</a>");
      });

    router.route("/private/logout").handler(routingContext -> {
      routingContext.session().logout();
      routingContext.response().end("logged out; user: " + routingContext.session().getLoginID());
    });

  }

  private void installSessionStore(Router router) {
    SessionStore store = LocalSessionStore.create(vertx);
    SessionHandler sessionHandler = SessionHandler.create(store);
    router.route().handler(CookieHandler.create());
    router.route().handler(sessionHandler);
    router.route().handler(TimeoutHandler.create(5000));
  }

  /**
   * Uses Thymeleaf as templateengine
   * 
   * @param router
   */
  private void installTemplateHandler(Router router) {
    ThymeleafTemplateEngineImpl engine = new ThymeleafTemplateEngineImpl();
    engine.setMode(ThymeleafTemplateEngine.DEFAULT_TEMPLATE_MODE);
    engine.setCacheable(false);
    TemplateHandler th = TemplateHandler.create(engine);

    router.route("/dynamic/").handler(th);
  }

  /**
   * Anlegen eines Verzeichnis "webroot" im Projekt, darin eine Datei test.html anlegen URI aufrufen:
   * http://localhost:8080/static/test.html
   */
  private void installStatic(Router router) {
    StaticHandler sh = StaticHandler.create();
    sh.setCachingEnabled(false);
    sh.setCacheEntryTimeout(1);
    router.route("/static/").handler(sh);
  }

  private void addSessionTestRoutes(Router router) {
    router.get("/setSessionParameter").handler(rc -> {
      rc.session().put("testkey", "testvalue");

    });
  }

  /**
   * AUfruf durch http://localhost:8080/provocateError
   * 
   * @param router
   */
  private void addFailureHandler(Router router) {
    Route route = router.get();

    route.failureHandler(frc -> {
      int statusCode = frc.statusCode();
      if (statusCode == 401) {
        frc.next();
      } else {

        String reply = "Status-Code: " + frc.statusCode();
        if (frc.failure() != null) {
          reply += "\n" + frc.failure().toString();
        }
        if (frc.request().absoluteURI() != null)
          reply += "\n" + frc.request().absoluteURI();

        frc.response().end(reply);
      }

    });

    router.get("/provocateError").handler(routingContext -> {
      throw new RuntimeException("provozierte Exception ");
    });
  }

  /**
   * Aufruf durch http://localhost:8080/productsAPI/products/pro12334
   * 
   * @param mainRouter
   */
  private void mountSubRouter(Router mainRouter) {
    Router restAPI = Router.router(vertx);

    restAPI.get("/products/:productID").handler(rc -> {

      String productID = rc.request().getParam("productid");
      String method = rc.request().method().toString();
      rc.response().end("RESTAPI: " + method + ": " + productID);

    });

    restAPI.put("/products/:productID").handler(rc -> {

      String productID = rc.request().getParam("productid");
      String method = rc.request().method().toString();
      rc.response().end("RESTAPI: " + method + ": " + productID);

    });

    restAPI.delete("/products/:productID").handler(rc -> {

      String productID = rc.request().getParam("productid");
      String method = rc.request().method().toString();
      rc.response().end("RESTAPI: " + method + ": " + productID);

    });

    Handler<RoutingContext> handler = new Handler<RoutingContext>() {

      @Override
      public void handle(RoutingContext event) {
        event.response().putHeader("content-type", "text/plain").end("RestApi: " + event.request().method());
      }

    };

    restAPI.route().handler(handler);

    mainRouter.mountSubRouter("/productsAPI", restAPI);
  }

  /**
   * Aufruf durch http://localhost:8080/irgendwas/foo
   * 
   * @param router
   */
  private void routeByRegex(Router router) {
    Route route = router.route().pathRegex(".*foo");

    route.handler(routingContext -> {
      routingContext.response().putHeader("content-type", "text/plain").end(routingContext.request().absoluteURI());
    });
  }

  /**
   * Aufruf durch http://localhost:8080/catalogue/products/derTyp/dieID/
   * 
   * @param router
   */
  private void addParameterRoute(Router router) {
    Route route = router.route(HttpMethod.GET, "/catalogue/products/:productype/:productid/");

    route.handler(routingContext -> {

      String productType = routingContext.request().getParam("productype");
      String productID = routingContext.request().getParam("productid");

      routingContext.response().putHeader("content-type", "text/plain")
          .end("Typ: " + productType + "  \n" + "ID: " + productID);

    });
  }

  private void addGlobaleRoute(Router router) {
    Handler<RoutingContext> handler = new Handler<RoutingContext>() {

      @Override
      public void handle(RoutingContext event) {
        event.response().putHeader("content-type", "text/plain").end("Hello World!: " + event.request().method());
      }

    };

    router.route().order(1000).handler(handler);
  }

  /**
   * Aufruf von http://localhost:8080/some/path/
   * 
   * @param router
   */
  private void addMultiRouts(Router router) {
    Route route1 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.putHeader("content-length", "500");
      response.write("route1\n");
      routingContext.next();
    });

    Route route2 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route2\n");
      routingContext.next();
    });

    Route route3 = router.route("/some/path/").handler(routingContext -> {

      HttpServerResponse response = routingContext.response();
      response.write("route3");
      routingContext.response().end();
    });
  }

}
