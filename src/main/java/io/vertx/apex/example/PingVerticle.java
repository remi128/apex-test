package io.vertx.apex.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

public class PingVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(PingVerticle.class);

	public PingVerticle() {
		// TODO Auto-generated constructor stub
	}

	  
    @Override
    public void start() throws Exception {  
    	Handler handler = new Handler<Long>() {

			@Override
			public void handle(Long event) {
				// TODO Auto-generated method stub
				vertx.eventBus().send( "ping", "pingmessage", new DeliveryOptions().
				        setSendTimeout(500), reply -> {
				            if (reply.succeeded()) {
				                log.info("Received respone: " + reply.result().body());
				            } else {
				                log.info("Received no respone");
				            }
				        });
			}
		};
    	vertx.setPeriodic( 1000, handler );
    	
    	/*
        vertx.setTimer( 1000, msg -> {
        vertx.eventBus().send(   "ping", new DeliveryOptions().
        setSendTimeout(500), reply -> {
            if (reply.succeeded()) {
                log.info("Received respone: " + reply.result().body());
            } else {
                log.info("Received no respone");
            }
        });
        });
          */      
        vertx.eventBus().publish("pingToLog", "ping");
    }	
}
