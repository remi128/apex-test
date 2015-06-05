/**
 * 
 */


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.Calendar;
import java.util.List;

/**
 * An abstract definition of a record to be written into BSON
 * 
 * @author mremme
 */
public abstract class AbstractRecord {
  private String   id         = null;
  private Calendar createdOn  = Calendar.getInstance();
  private Calendar modifiedOn = Calendar.getInstance();
  private String   collection;

  /**
   * 
   */
  public AbstractRecord() {
  }

  /**
	 * 
	 */
  public AbstractRecord(String collection) {
    this.collection = collection;
  }

  /**
   * 
   */
  public AbstractRecord(String collection, JsonObject json, MongoClient mongoClient,
      Handler<AsyncResult<Void>> resultHandler) {
    this.collection = collection;
    fromJson(json, mongoClient, resultHandler);
  }

  /**
   * The creation date of the instance
   * 
   * @return
   */
  public final Calendar getCreatedOn() {
    return createdOn;
  }

  /**
   * The creation date of the instance
   * 
   * @param createdOn
   */
  public final void setCreatedOn(Calendar createdOn) {
    this.createdOn = createdOn;
  }

  /**
   * The modification date of the instance
   * 
   * @return
   */
  public final Calendar getModifiedOn() {
    return modifiedOn;
  }

  /**
   * The modification date of the instance
   * 
   * @param modifiedOn
   */
  public final void setModifiedOn(Calendar modifiedOn) {
    this.modifiedOn = modifiedOn;
  }

  /**
   * The id of the instance, which is normally set by mongo itself
   * 
   * @return
   */
  public final String getId() {
    return id;
  }

  /**
   * The id of the instance, which is normally set by mongo itself
   * 
   * @param _id
   */
  public final void set_id(String id) {
    this.id = id;
  }

  /**
   * Get the collection, where the current record is stored inside
   * 
   * @return
   */
  public String getCollection() {
    return collection;
  }

  /**
   * Creates a JSonObject with the correct structure from the current instance. This method sets the value of the field
   * modifiedOn to the current time
   * 
   * @param authProvider
   * @return
   */
  public JsonObject toJSon() {
    JsonObject jsonOb = new JsonObject();
    if (id != null && id.hashCode() != 0)
      jsonOb.put("_id", id);
    modifiedOn = Calendar.getInstance();
    jsonOb.put("createdOn", createdOn.getTimeInMillis()).put("modifiedOn", modifiedOn.getTimeInMillis());
    return jsonOb;
  }

  /**
   * Adds the String value to json. Null values are not stored
   * 
   * @param json
   * @param fieldName
   * @param value
   * @return
   */
  protected JsonObject addToJson(JsonObject json, String fieldName, String value) {
    if (value != null)
      return json.put(fieldName, value);
    return json;
  }

  /**
   * Fill the instance with values from the given JsonObject
   * 
   * @param json
   */
  public void fromJson(JsonObject json, MongoClient mongoClient, Handler<AsyncResult<Void>> resultHandler) {
    id = json.getString("_id");
    long s = json.getLong("createdOn", 0l);
    if (s != 0) {
      createdOn.setTimeInMillis(s);
    } else {
      createdOn = Calendar.getInstance();
    }
    s = json.getLong("modifiedOn", 0l);
    if (s != 0) {
      modifiedOn.setTimeInMillis(s);
    }
    afterloadActions(mongoClient, resultHandler);
  }

  /**
   * Delete the current instance from mongo
   * 
   * @param service
   * @param resultHandler
   */
  public void delete(MongoClient service, Handler<AsyncResult<AbstractRecord>> resultHandler) {
    if (id == null || id.hashCode() == 0) {
      resultHandler.handle(Future.succeededFuture(this));
    } else {
      service.remove(collection, new JsonObject().put("_id", id), res -> {
        if (res.succeeded()) {
          id = null;
          resultHandler.handle(Future.succeededFuture(this));
        } else {
          resultHandler.handle(Future.failedFuture(res.cause()));
        }
      });
    }
  }

  /**
   * This method loads the contents of the record with the given id and places the content into the current instance
   * 
   * @param context
   * @param mongoClient
   * @param id
   * @param resultHandler
   */
  public final void loadRecordById(MongoClient mongoClient, String id, Handler<AsyncResult<Void>> resultHandler) {
    JsonObject query = new JsonObject().put("_id", id);
    mongoClient.find(getCollection(), query, selection -> {
      if (selection.failed()) {
        Future<Void> future = Future.failedFuture(selection.cause());
        resultHandler.handle(future);
      } else {
        List<JsonObject> resultList = selection.result();
        if (resultList.isEmpty()) {
          Future<Void> future = Future.failedFuture("No record found with id " + id);
          resultHandler.handle(future);
        } else if (resultList.size() > 1) {
          Future<Void> future = Future.failedFuture("More than one record found with id " + id);
          resultHandler.handle(future);
        } else {
          fromJson(resultList.get(0), mongoClient, resultHandler);
        }
      }
    });
  }

  public final void updateRecord(MongoClient mongoClient, Handler<AsyncResult<AbstractRecord>> resultHandler) {
    try {
      mongoClient.save(getCollection(), toJSon(), result -> {
        if (result.failed()) {
          Future<AbstractRecord> future = Future.failedFuture(result.cause());
          resultHandler.handle(future);
        } else {
          Future<AbstractRecord> future = Future.succeededFuture(this);
          resultHandler.handle(future);
        }
      });
    } catch (Throwable e) {
      Future<AbstractRecord> future = Future.failedFuture(e);
      resultHandler.handle(future);
    }
  }

  /**
   * This method performs actions like loading objects, which are referenced and stored by an ID
   * 
   * @param mongoClient
   * @param resultHandler
   */
  protected abstract void afterloadActions(MongoClient mongoClient, Handler<AsyncResult<Void>> resultHandler);

}
