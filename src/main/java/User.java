/**
 * 
 */


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import de.braintags.fairytales.FairyTaleVerticle;
import de.braintags.fairytales.model.AbstractRecord;

/**
 * Definition of a user
 * 
 * @author mremme
 */
public class User extends AbstractRecord {
  private String username;
  private String emailAddress;
  private String firstName;
  private String lastName;
  private String gender;
  private String password;

  /**
   * 
   */
  public User() {
    super(FairyTaleVerticle.USER_COLLECTION_NAME);
  }

  /**
     * 
     */
  public User(String collection) {
    super(collection);
  }

  /**
   * 
   */
  public User(String collection, JsonObject json, MongoClient mongoClient, Handler<AsyncResult<Void>> resultHandler) {
    super(collection, json, mongoClient, resultHandler);
  }

  /**
	 * 
	 */
  public User(String collection, String username, String lastName, String firstName, String email, String password) {
    super(collection);
    this.username = username;
    this.emailAddress = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.password = password;
  }

  /**
   * @return the username
   */
  public final String getUsername() {
    return username;
  }

  /**
   * @param username
   *          the username to set
   */
  public final void setUsername(String username) {
    this.username = username;
  }

  /**
   * @return the emailAddress
   */
  public final String getEmailAddress() {
    return emailAddress;
  }

  /**
   * @param emailAddress
   *          the emailAddress to set
   */
  public final void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  /**
   * @return the firstName
   */
  public final String getFirstName() {
    return firstName;
  }

  /**
   * @param firstName
   *          the firstName to set
   */
  public final void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /**
   * @return the lastName
   */
  public final String getLastName() {
    return lastName;
  }

  /**
   * @param lastName
   *          the lastName to set
   */
  public final void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /**
   * @return the gender
   */
  public final String getGender() {
    return gender;
  }

  /**
   * @param gender
   *          the gender to set
   */
  public final void setGender(String gender) {
    this.gender = gender;
  }

  /**
   * @return the password
   */
  public final String getPassword() {
    return password;
  }

  /**
   * @param password
   *          the password to set
   */
  public final void setPassword(String password) {
    this.password = password;
  }

  /**
   * Find and load a user by username
   * 
   * @param mongoClient
   * @param collectionName
   * @param principal
   * @param resultHandler
   */
  public static void findUserByPrincipal(MongoClient mongoClient, String collectionName, JsonObject principal,
      Handler<AsyncResult<User>> resultHandler) {
    mongoClient.find(collectionName, principal, res -> {

      if (res.failed()) {
        Future<User> future = Future.failedFuture(res.cause());
        resultHandler.handle(future);
      } else {
        if (res.result().size() == 0) {
          Future<User> future = Future.failedFuture("The user '" + principal + "'does not exist");
          resultHandler.handle(future);
        } else if (res.result().size() > 1) {
          Future<User> future = Future.failedFuture("The user '" + principal + "'exists multiple");
          resultHandler.handle(future);
        } else {
          User user = new User(collectionName);
          user.fromJson(res.result().get(0), mongoClient, userResult -> {
            if (userResult.failed()) {
              resultHandler.handle(Future.failedFuture(userResult.cause()));
            } else {
              Future<User> future = Future.succeededFuture(user);
              resultHandler.handle(future);
            }
          });
        }
      }
    });

  }

  public static void insertUser(MongoClient mongoClient, String username, String lastName, String firstName,
      String email, String password, Handler<AsyncResult<User>> resultHandler) {
    String collectionName = FairyTaleVerticle.USER_COLLECTION_NAME;
    JsonObject query = new JsonObject();
    query.put(FairyTaleVerticle.USERNAME_FIELD, username);
    mongoClient.count(collectionName, query, res -> {
      if (res.failed()) {
        Future<User> future = Future.failedFuture(res.cause());
        resultHandler.handle(future);
      } else {
        if (res.result() > 0) {
          Future<User> future = Future.failedFuture("The user with username '" + username + "'exists already");
          resultHandler.handle(future);
        } else {

          try {
            //create and insert
        User user = new User(collectionName, username, lastName, firstName, email, password);
        mongoClient.insert(collectionName, user.toJSon(), insertResult -> {
          if (insertResult.failed()) {
            Future<User> future = Future.failedFuture(insertResult.cause());
            resultHandler.handle(future);
          } else {
            String id = insertResult.result();
            user.set_id(id);
            Future<User> future = Future.succeededFuture(user);
            resultHandler.handle(future);
          }
        });
      } catch (Throwable e) {
        Future<User> future = Future.failedFuture(e);
        resultHandler.handle(future);
      }
    }
  }
}   );
  }

  /**
   * @return
   */
  @Override
  public JsonObject toJSon() {
    JsonObject json = super.toJSon();
    addToJson(json, FairyTaleVerticle.USERNAME_FIELD, username);
    addToJson(json, "lastName", lastName);
    addToJson(json, "firstName", firstName);
    addToJson(json, "email", emailAddress);
    addToJson(json, FairyTaleVerticle.PASSWORD_FIELD, password);
    addToJson(json, "gender", gender);
    return json;
  }

  /*
   * @see de.braintags.fairytales.model.AbstractRecord#fromJson(io.vertx.core.json.JsonObject)
   */
  @Override
  public void fromJson(JsonObject json, MongoClient mongoClient, Handler<AsyncResult<Void>> resultHandler) {
    this.username = json.getString(FairyTaleVerticle.USERNAME_FIELD);
    this.emailAddress = json.getString("email");
    this.firstName = json.getString("firstName");
    this.gender = json.getString("gender");
    this.lastName = json.getString("lastName");
    this.password = json.getString(FairyTaleVerticle.PASSWORD_FIELD);
    super.fromJson(json, mongoClient, resultHandler);
  }

  /*
   * (non-Javadoc)
   * @see de.braintags.fairytales.model.AbstractRecord#afterloadActions(io.vertx.ext.mongo.MongoClient,
   * io.vertx.core.Handler)
   */
  @Override
  public void afterloadActions(MongoClient mongoClient, Handler<AsyncResult<Void>> resultHandler) {
    resultHandler.handle(Future.succeededFuture());
  }

}
