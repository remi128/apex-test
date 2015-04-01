/**
 * 
 */
package io.vertx.apex.example.mongo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

/**
 * @author mremme
 */
public class MongoTestMain extends AbstractVerticle {

  /**
	 * 
	 */
  public MongoTestMain() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public void start() throws Exception {
    publishMongoService();
    vertx.deployVerticle(MongoConsument.class.getName());

  }

  private void publishMongoService() {
    JsonObject config = new JsonObject();
    //config.put("db_name", "mydb");
    //config.put("username", "john").put("password", "passw0rd");
    //vertx.deployVerticle("service:io.vertx:vertx-mongo-service", new DeploymentOptions().setConfig(config));

    // Deploy service - can be anywhere on your network
    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle("service:io.vertx-vertx-mongo-service", options, res -> {

      if (res.succeeded()) {
        System.out.println("mongo service deployed");
      } else {
        // Failed to deploy
        res.cause().printStackTrace();
      }

    });

  }

}
