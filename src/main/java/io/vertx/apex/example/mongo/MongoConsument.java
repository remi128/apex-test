/**
 * 
 */
package io.vertx.apex.example.mongo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoService;

/**
 * @author mremme
 *
 */
public class MongoConsument extends AbstractVerticle {

	/**
	 * 
	 */
	public MongoConsument() {
		// TODO Auto-generated constructor stub
	}

	@Override
	  public void start() {
	    saveObject();
	  }
	
	private void saveObject(){
		JsonObject save = new JsonObject();
		save.put("collection", "books");
		JsonObject document = new JsonObject().put("title", "The Hobbit");
		save.put("document", document);
		save.put("options", new JsonObject());
		vertx.eventBus().send("vertx.mongo", save, new DeliveryOptions().addHeader("action", "save"), saveResult -> {
		  if (saveResult.succeeded()) {
		    String id = (String) saveResult.result().body();
		    System.out.println("Saved book with id " + id);
		  } else {
		    saveResult.cause().printStackTrace();
		  }
		});		
		
	}
	
	private void saveByService(){
        MongoService proxy = MongoService.createEventBusProxy(vertx, "vertx.mongo");

        // Now do stuff with it:

	    Handler<AsyncResult<Long>> handler = new Handler<AsyncResult<Long>>() {
	
	
			@Override
			public void handle(AsyncResult<Long> arg0) {
				// TODO Auto-generated method stub
				System.out.println( arg0 );
			}
	    	   
	    	   
		};
	        
        proxy.count(  "books", new JsonObject(), handler);	    
	}
	
	private JsonObject createObject(){
		JsonObject document = new JsonObject().put("title", "The Hobbit");

		return document;
	}
}
