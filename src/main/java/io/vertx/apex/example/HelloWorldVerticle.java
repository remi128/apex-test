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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.ext.apex.core.Router;
import io.vertx.test.core.VertxTestBase;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HelloWorldVerticle extends AbstractVerticle {

	
  public void start() {
	  System.out.println( "starting" );
    Router router = Router.router(vertx);
    router.route().handler(rc -> rc.response().putHeader("content-type", "text/plain").end("Hello World!"));
    HttpServerOptions options = new HttpServerOptions().setPort(8080);
    
    HttpServer server = vertx.createHttpServer( options );
    
    server.requestHandler( router:: accept);
  }
}
