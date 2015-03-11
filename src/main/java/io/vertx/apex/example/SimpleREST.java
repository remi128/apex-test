/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.apex.example;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple REST/HTTP micro-service example.
 * The REST service manages a product directory.
 * 
 * There are three possible operations on the REST API:
 * List all products: GET /products
 * Add a product: PUT /products/<productID>
 * Get product details: GET /products/<productID>
 * 
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class SimpleREST  {

  public static void main(String[] args) {
    new SimpleREST().go();
  }

  private SimpleREST that = this;
  private Map<String, JsonObject> products = new HashMap<>();

  public void go() {

    setUpInitialData();

    Vertx vertx = Vertx.vertx();

    Router router = Router.router(vertx);

    router.route().handler(   BodyHandler.create() );
    
    router.route( HttpMethod.GET, "/products/:productID").handler(that::handleGetProduct);
    router.route( HttpMethod.PUT, "/products/:productID").handler(that::handleAddProduct);
    router.route( HttpMethod.GET, "/products").handler(that::handleListProducts);

    vertx.createHttpServer( new HttpServerOptions().setPort(8080) ).requestHandler( router::accept);
  }

  private void handleGetProduct(RoutingContext routingContext) {
    String productID = getParameter( routingContext, "productID");
    HttpServerResponse response = routingContext.response();
    if (productID == null) {
      sendError(400, response);
    } else {
      JsonObject product = products.get(productID);
      if (product == null) {
        sendError(404, response);
      } else {
        response.putHeader("content-type", "application/json").end(product.encode());
      }
    }
  }

  private void handleAddProduct(RoutingContext routingContext) {
    String productID = getParameter(routingContext, "productID");
    HttpServerResponse response = routingContext.response();
    if (productID == null) {
      sendError(400, response);
    } else {
      JsonObject product = routingContext.getBodyAsJson();
      if (product == null) {
        sendError(400, response);
      } else {
        products.put(productID, product);
        response.end();
      }
    }
  }

  private String getParameter( RoutingContext context, String name ){
	  HttpServerRequest request = context.request();
	  MultiMap mm = request.params();
	  String returnValue =  mm.get(name);
	  return returnValue;
  }
  
  private void handleListProducts(RoutingContext routingContext) {
    JsonArray arr = new JsonArray();
    products.values().forEach(arr::add);
    routingContext.response().putHeader("content-type", "application/json").end(arr.encode());
  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }

  private void setUpInitialData() {
    addProduct(new JsonObject().put("id", "prod3568").put("name", "Egg Whisk").put("price", 3.99).put("weight", 150));
    addProduct(new JsonObject().put("id", "prod7340").put("name", "Tea Cosy").put("price", 5.99).put("weight", 100));
    addProduct(new JsonObject().put("id", "prod8643").put("name", "Spatula").put("price", 1.00).put("weight", 80));
  }

  private void addProduct(JsonObject product) {
    products.put(product.getString("id"), product);
  }
}
