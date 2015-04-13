package io.vertx.ext.auth.mongo;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.mongo.MongoService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

/**
 * @author mremme
 */

public class MongoAuthTest extends MongoBaseTest {
  private static final Logger log = LoggerFactory.getLogger(MongoAuthTest.class);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    initMongoService();
  }

  @Override
  public void tearDown() throws Exception {
    mongoService.stop();
    super.tearDown();
  }

  /**
   * Test a user with unique username and password
   */
  @Test
  public void testLoginUniqueUser() {
    MongoAuthService authService = initDefaultAuthService();
    assertTrue(initOneUser("Michael", "ps1"));

    for (int i = 0; i < 30; i++) {
      JsonObject credentials = new JsonObject().put(MongoAuthProvider.DEFAULT_USERNAME_FIELD, "Michael").put(
          MongoAuthProvider.DEFAULT_PASSWORD_FIELD, "ps1");
      authService.login(credentials, onSuccess(res -> {
        System.out.println(String.valueOf(res));
        assertNotNull(res);

        //testComplete();
        }));
    }
    //await();
  }

  /**
   * Test a user with duplicate username and unique password. This should be accepted by the default implementation
   */
  @Test
  public void testLoginAllowedDoublette() {
    MongoAuthService authService = initDefaultAuthService();
    assertTrue(initOneUser("allowedDoublette", "ps1"));
    assertTrue(initOneUser("allowedDoublette", "ps2"));

    JsonObject credentials = new JsonObject().put(MongoAuthProvider.DEFAULT_USERNAME_FIELD, "allowedDoublette").put(
        MongoAuthProvider.DEFAULT_PASSWORD_FIELD, "ps1");
    authService.login(credentials, onSuccess(res -> {
      assertNotNull(res);
      //authService.
        testComplete();

      }));
    await();
  }

  /**
   * Test a user with duplicate username AND duplicate password. This should NOT be accepted by the default
   * implementation
   */
  @Test
  public void testLoginUnallowedDoublette() {
    MongoAuthService authService = initDefaultAuthService();
    assertTrue(initOneUser("unallowedDoublette", "ps2"));
    assertTrue(initOneUser("unallowedDoublette", "ps2"));

    JsonObject credentials = new JsonObject().put(MongoAuthProvider.DEFAULT_USERNAME_FIELD, "unallowedDoublette").put(
        MongoAuthProvider.DEFAULT_PASSWORD_FIELD, "ps2");
    authService.login(credentials, onFailure(res -> {
      assertTrue(res instanceof AuthenticationException);
      testComplete();
    }));
    await();
  }

  /**
   * null username should throw exceptino
   */
  @Test
  public void testLoginNullUsername() {
    MongoAuthService authService = initDefaultAuthService();
    JsonObject credentials = new JsonObject().put(MongoAuthProvider.DEFAULT_PASSWORD_FIELD, "ps2");
    authService.login(credentials, onFailure(res -> {
      assertTrue(res instanceof AuthenticationException);
      testComplete();
    }));
    await();
  }

  /**
   * null password should throw exception
   */
  @Test
  public void testLoginNullPassword() {
    MongoAuthService authService = initDefaultAuthService();
    JsonObject credentials = new JsonObject().put(MongoAuthProvider.DEFAULT_USERNAME_FIELD, "unallowedDoublette");
    authService.login(credentials, onFailure(res -> {
      assertTrue(res instanceof AuthenticationException);
      testComplete();
    }));
    await();
  }

  /**
   * null password should throw exception
   */
  @Test
  public void testRoleAccess() {
    MongoAuthService authService = initDefaultAuthService();
    assertTrue(initOneUser("MichaelRole", "ps1", Arrays.asList("role1", "role2")));

    JsonObject credentials = new JsonObject().put(MongoAuthProvider.DEFAULT_USERNAME_FIELD, "MichaelRole").put(
        MongoAuthProvider.DEFAULT_PASSWORD_FIELD, "ps1");
    authService.login(credentials, onSuccess(res -> {
      System.out.println(String.valueOf(res));
      assertNotNull(res);

      authService.hasRole(res, "role1", onSuccess(roleResult -> {
        System.out.println(String.valueOf(roleResult));
        assertNotNull(roleResult);
      }));

      testComplete();
    }));
    await();
  }

  private MongoAuthService initDefaultAuthService() {
    return new MongoAuthService(vertx, mongoService, createDefaultConfig());
  }

  private JsonObject createDefaultConfig() {
    JsonObject js = new JsonObject();
    return js;
  }

  private void initMongoService() throws Exception {
    JsonObject config = getConfig();
    mongoService = MongoService.create(vertx, config);
    mongoService.start();
    CountDownLatch latch = new CountDownLatch(1);
    dropCollections(latch);
    awaitLatch(latch);
  }

  private boolean initOneUser(String username, String password) {
    JsonObject user = new JsonObject().put(MongoAuthProvider.DEFAULT_USERNAME_FIELD, username).put(
        MongoAuthProvider.DEFAULT_PASSWORD_FIELD, password);
    final StringBuffer returnString = new StringBuffer();
    mongoService.save(MongoAuthProvider.DEFAULT_COLLECTION_NAME, user, res -> {

      if (res.succeeded()) {
      } else {
        res.cause().printStackTrace();
        returnString.append("failed");
      }
    });
    return returnString.length() == 0;
  }

  private boolean initOneUser(String username, String password, List<String> roles) {
    JsonObject user = new JsonObject().put(MongoAuthProvider.DEFAULT_USERNAME_FIELD, username).put(
        MongoAuthProvider.DEFAULT_PASSWORD_FIELD, password);
    JsonArray roleArray = new JsonArray(roles);
    user.put(MongoAuthProvider.DEFAULT_ROLE_FIELD, roles);

    final StringBuffer returnString = new StringBuffer();
    mongoService.save(MongoAuthProvider.DEFAULT_COLLECTION_NAME, user, res -> {

      if (res.succeeded()) {
      } else {
        res.cause().printStackTrace();
        returnString.append("failed");
      }
    });
    return returnString.length() == 0;
  }

}
