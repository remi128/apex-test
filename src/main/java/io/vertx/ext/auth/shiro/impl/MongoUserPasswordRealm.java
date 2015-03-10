/*
 * Copyright 2014 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 and Apache License v2.0 which accompanies this distribution. The
 * Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html The Apache License v2.0 is available
 * at http://www.opensource.org/licenses/apache2.0.php You may elect to redistribute this code under either of these
 * licenses.
 */

package io.vertx.ext.auth.shiro.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.auth.shiro.impl.MongoUserPasswordRealm.InternalHandler;
import io.vertx.ext.mongo.MongoService;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;

/**
 * Realm that allows authentication and authorization via MongoDb. The driver expects an existing vertx service for
 * MongoDb
 * 
 * @author mremme
 */
public class MongoUserPasswordRealm extends AuthorizingRealm {
  private static final Logger log                     = LoggerFactory.getLogger(MongoUserPasswordRealm.class);

  private JdbcRealm           toremove;

  public static final String  DEFAULT_COLLECTION_NAME = "user";

  /**
   * The default name of the property for the username
   */
  public static final String  DEFAULT_USERNAME_FIELD  = "username";

  /**
   * The default name of the property for the password
   */
  public static final String  DEFAULT_PASSWORD_FIELD  = "password";

  /**
   * The default name of the property for the salt field
   */
  public static final String  DEFAULT_SALT_FIELD      = "salt";

  /**
   * Password hash salt configuration.
   * <ul>
   * <li>NO_SALT - password hashes are not salted.</li>
   * <li>CRYPT - password hashes are stored in unix crypt format.</li>
   * <li>COLUMN - salt is in a separate column in the database.</li>
   * <li>EXTERNAL - salt is not stored in the database. {@link #getSaltForUser(String)} will be called to get the salt</li>
   * </ul>
   */
  public enum SaltStyle {
    NO_SALT, CRYPT, COLUMN, EXTERNAL;

  };

  private final Vertx  vertx;

  private final String mongoServiceName;
  private String       usernameField            = DEFAULT_USERNAME_FIELD;
  private String       passwordField            = DEFAULT_PASSWORD_FIELD;
  private String       saltField                = DEFAULT_SALT_FIELD;
  private String       collectionName           = DEFAULT_COLLECTION_NAME;
  private boolean      usernameMustUnique       = false;
  private boolean      permissionsLookupEnabled = false;

  private SaltStyle    saltStyle                = SaltStyle.NO_SALT;

  public MongoUserPasswordRealm(Vertx vertx, String mongoServiceName) {
    this.mongoServiceName = mongoServiceName;
    this.vertx = vertx;
  }

  /**
   * Set the name of the collection to be used. Defaults to DEFAULT_COLLECTION_NAME
   * 
   * @param collectionName
   * @return
   */
  public MongoUserPasswordRealm setCollectionName(String collectionName) {
    this.collectionName = collectionName;
    return this;
  }

  /**
   * Set the saltstyle as defined by enumeration {@link SaltStyle}. Defaults to DEFAULT_SALT_FIELD
   * 
   * @param saltStyle
   * @return
   */
  public MongoUserPasswordRealm setSaltStyle(SaltStyle saltStyle) {
    switch (saltStyle) {
    case NO_SALT:
      break;
    case CRYPT:
      // TODO: separate password and hash from getPasswordForUser[0]
      throw new UnsupportedOperationException("Not implemented yet, saltstyle " + saltStyle);
      //break;
    case COLUMN:
      throw new UnsupportedOperationException("Not implemented yet, saltstyle " + saltStyle);
    case EXTERNAL:
      throw new UnsupportedOperationException("Not implemented yet, saltstyle " + saltStyle);
    }
    this.saltStyle = saltStyle;
    return this;
  }

  /**
   * Set the name of the field to be used for the username. Defaults to DEFAULT_USERNAME_FIELD
   * 
   * @param fieldName
   * @return
   */
  public MongoUserPasswordRealm setUsernameField(String fieldName) {
    this.usernameField = fieldName;
    return this;
  }

  /**
   * Set the name of the field to be used for the password Defaults to DEFAULT_PASSWORD_FIELD
   * 
   * @param fieldName
   * @return
   */
  public MongoUserPasswordRealm setPasswordField(String fieldName) {
    this.passwordField = fieldName;
    return this;
  }

  /**
   * Defines wether a username in the store must be unique or not. If not, then the combination of username and password
   * will define a fitting user
   * 
   * @param unique
   * @return
   */
  public MongoUserPasswordRealm setUsernameMustUnique(boolean unique) {
    this.usernameMustUnique = unique;
    return this;
  }

  /**
   * Set the name of the field to be used for the salt ( if needed )
   * 
   * @param fieldName
   * @return
   */
  public MongoUserPasswordRealm setSaltField(String fieldName) {
    this.saltField = fieldName;
    return this;
  }

  /**
   * Activate / deactivate permission lookup
   * 
   * @param permissionsLookupEnabled
   *          the permissionsLookupEnabled to set
   */
  public MongoUserPasswordRealm setPermissionsLookupEnabled(boolean permissionsLookupEnabled) {
    this.permissionsLookupEnabled = permissionsLookupEnabled;
    return this;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.shiro.realm.AuthenticatingRealm#supports(org.apache.shiro.authc.AuthenticationToken)
   */
  @Override
  public boolean supports(AuthenticationToken token) {
    return token != null && getAuthenticationTokenClass().isAssignableFrom(UsernamePasswordToken.class);
  }

  /*
   * (non-Javadoc)
   * @see org.apache.shiro.realm.AuthorizingRealm#doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
   */
  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    //null usernames are invalid
    if (principals == null) {
      throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
    }

    String username = (String) getAvailablePrincipal(principals);

    /*
     * Eventuell muß hier auf eindeutigen Usernamen umgestellt werden um auf ein Userobject zu kommen
     */
    if (username != null)
      throw new UnsupportedOperationException("Prüfe wie man auf Userobject kommt");

    Set<String> roleNames = null;
    Set<String> permissions = null;
    roleNames = getRoleNamesForUser(username);
    if (permissionsLookupEnabled) {
      permissions = getPermissions(username, roleNames);
    }

    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
    info.setStringPermissions(permissions);
    return info;

  }

  /*
   * (non-Javadoc)
   * @see org.apache.shiro.realm.AuthenticatingRealm#doGetAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken)
   */
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;
    String username = upToken.getUsername();

    // Null username is invalid
    if (username == null) {
      throw new AuthenticationException("Null usernames are not allowed by this realm.");
    }

    MongoService mongoService = MongoService.createEventBusProxy(vertx, mongoServiceName);
    JsonObject query = createQuery(username);
    InternalHandler handler = new InternalHandler(username);
    mongoService.find(this.collectionName, query, handler);
    return handler.info;
  }

  /**
   * The default implementation uses the usernameField as search field
   * 
   * @param username
   * @return
   */
  protected JsonObject createQuery(String username) {
    return new JsonObject().put(usernameField, username);
  }

  private AuthenticationInfo handleSelection(AsyncResult<List<JsonObject>> resultList, String username) {
    if (usernameMustUnique && resultList.result().size() > 1)
      throw new AuthenticationException("More than one user row found for user [" + username
          + "]. Usernames must be unique.");

    for (JsonObject json : resultList.result()) {
      AuthenticationInfo info = handleObject(json, username);
      if (info != null)
        return info;
    }
    return null;
  }

  protected Set<String> getRoleNamesForUser(String username) {
    Set<String> roleNames = new LinkedHashSet<String>();
    return roleNames;
  }

  protected Set<String> getPermissions(String username, Collection<String> roleNames) {
    throw new UnsupportedOperationException("Not yet supported: permissions");
  }

  private AuthenticationInfo handleObject(JsonObject userObject, String username) throws AuthenticationException {
    SimpleAuthenticationInfo info = null;

    String password = null;
    String salt = null;
    switch (saltStyle) {
    case NO_SALT:
      password = getPasswordForUser(userObject);
      break;
    case CRYPT:
      // TODO: separate password and hash, can't happen at that time, cause canceled through init already
      break;
    case COLUMN:
      // TODO: separate password and hash, can't happen at that time, cause canceled through init already
      break;
    case EXTERNAL:
      // TODO: separate password and hash, can't happen at that time, cause canceled through init already
    }

    if (password == null) {
      throw new UnknownAccountException("No account found for user [" + username + "]");
    }
    info = new SimpleAuthenticationInfo(username, password.toCharArray(), getName());
    if (salt != null) {
      info.setCredentialsSalt(ByteSource.Util.bytes(salt));
    }

    return info;
  }

  protected String getSaltForUser(String username) {
    return username;
  }

  private String getPasswordForUser(JsonObject userObject) {
    switch (saltStyle) {
    case NO_SALT:
      return userObject.getString(passwordField);

    default:
      return null;
    }
  }

  class InternalHandler implements Handler<AsyncResult<List<JsonObject>>> {
    AuthenticationInfo info = null;
    String             username;

    InternalHandler(String username) {
      this.username = username;
    }

    @Override
    public void handle(AsyncResult<List<JsonObject>> res) {
      if (res.succeeded()) {
        info = handleSelection(res, username);
      } else {
        throw new AuthenticationException(res.cause());
      }
    }

  }

}
