package io.vertx.apex.example.apex;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

public class MainApexVerticle extends AbstractVerticle {

	public MainApexVerticle() {
	}

    @Override
    public void start(Future startFuture) throws Exception {
        vertx.deployVerticle( HelloWorldApexVerticle.class.getName() );
    }

}
