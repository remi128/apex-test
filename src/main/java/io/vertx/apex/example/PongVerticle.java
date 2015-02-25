package io.vertx.apex.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

public class PongVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(PongVerticle.class);
    private long counter = 0;

	public PongVerticle() {
		// TODO Auto-generated constructor stub
	}

    
    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer("ping").handler(msg -> {
            counter++;
            msg.reply("pong "+msg.body());
        });
          
        vertx.eventBus().consumer("pingToLog").handler(msg -> {
            log.info("Received a pingToLog. The counter value is "+counter);
        });
          
    }	
}
