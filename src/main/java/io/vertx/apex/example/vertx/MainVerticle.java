package io.vertx.apex.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {

	public MainVerticle() {
		// TODO Auto-generated constructor stub
	}

    @Override
    public void start(Future startFuture) throws Exception {
        vertx.deployVerticle(HttpVerticle.class.getName(), new DeploymentOptions().setIsolationGroup("myGroup"));
        vertx.deployVerticle(PingVerticle.class.getName(), new DeploymentOptions().setInstances(2));
        vertx.deployVerticle(PongVerticle.class.getName());
    }

}
