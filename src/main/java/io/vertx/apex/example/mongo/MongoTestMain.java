/**
 * 
 */
package io.vertx.apex.example.mongo;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

/**
 * @author mremme
 *
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
    
    private void publishMongoService(){
    	JsonObject config = new JsonObject();
    	//config.put("db_name", "mydb");
    	//config.put("username", "john").put("password", "passw0rd");
    	vertx.deployVerticle( "service:io.vertx:mongo-service", new DeploymentOptions().setConfig(config));
    }
    
}
