package io.vertx.apex.test;

import io.vertx.apex.example.HttpVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.test.core.VertxTestBase;

import org.junit.Before;
import org.junit.Test;

public class HttpVerticleTest extends VertxTestBase {

    @Before
    public void setUpTest() {
        vertx.deployVerticle(HttpVerticle.class.getName());
        waitUntil(() -> vertx.deployments().size() == 1);
    }
    
    @Test
    public void testGetHttpResponse() {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions());
        httpClient.request(HttpMethod.GET, 8080, "localhost", "/hallo", response -> {
            response.bodyHandler(body -> {
                    assertEquals("/hallo", body.getString(0,body.length()));
                    testComplete();
                }
            );
        }).end();
        await();
    }
}
