/*
 * Copyright 2014 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 and Apache License v2.0 which accompanies this distribution. The
 * Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html The Apache License v2.0 is available
 * at http://www.opensource.org/licenses/apache2.0.php You may elect to redistribute this code under either of these
 * licenses.
 */

package io.vertx.ext.auth.shiro.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.shiro.impl.MongoUserPasswordRealm.SaltStyle;

import org.apache.shiro.mgt.DefaultSecurityManager;

/**
 * A Realm for use with a Vertx-Mongo-Service.
 * 
 * @author mremme
 */
public class MongoAuthRealmImpl extends ShiroAuthRealmImpl {

  /**
   * The property name to be used to set the name of the mongo service to be used
   */
  public static final String PROPERTY_MONGO_SERVICENAME        = "mongoServiceName";

  /**
   * The property name to be used to set the name of the collection inside the config
   */
  public static final String PROPERTY_COLLECTION_NAME          = "collectionName";

  /**
   * The property name to be used to set the name of the field, where the username is stored inside
   */
  public static final String PROPERTY_USERNAME_FIELD           = "usernameField";

  /**
   * The property name to be used to set the name of the field, where the password is stored inside
   */
  public static final String PROPERTY_PASSWORD_FIELD           = "passwordField";

  /**
   * The property name to be used to set the name of the field, where the SALT is stored inside
   */
  public static final String PROPERTY_SALT_FIELD               = "saltField";

  /**
   * The property name to be used to set the name of the field, where the salt style is stored inside
   * 
   * @see SaltStyle
   */
  public static final String PROPERTY_SALT_STYLE               = "saltStyle";

  /**
   * The property name to be used to set the name of the field, where the permissionsLookupEnabled is stored inside
   */
  public static final String PROPERTY_PERMISSIONLOOKUP_ENABLED = "permissionsLookupEnabled";

  /**
   * The property name to be used to set the name of the field, where the usernameMustUnique is stored inside
   */
  public static final String PROPERTY_USERNAME_UNIQUE          = "usernameMustUnique";

  private Vertx              vertx;

  /**
	 * 
	 */
  public MongoAuthRealmImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  /*
   * (non-Javadoc)
   * @see de.braintags.vertx.auth.MongoAuthRealm#init(io.vertx.core.json.JsonObject)
   */
  @Override
  public void init(JsonObject config) {
    this.config = config;

    String mongoserviceName = config.getString(PROPERTY_MONGO_SERVICENAME);
    if (mongoserviceName == null) {
      throw new IllegalArgumentException("parameter must be defined: " + PROPERTY_MONGO_SERVICENAME);
    }

    MongoUserPasswordRealm mongoRealm = new MongoUserPasswordRealm(vertx, mongoserviceName);

    String collectionName = config.getString(PROPERTY_COLLECTION_NAME);
    if (collectionName != null) {
      mongoRealm.setCollectionName(collectionName);
    }

    String passwordField = config.getString(PROPERTY_PASSWORD_FIELD);
    if (passwordField != null) {
      mongoRealm.setPasswordField(passwordField);
    }

    String saltField = config.getString(PROPERTY_SALT_FIELD);
    if (saltField != null) {
      mongoRealm.setSaltField(saltField);
    }

    String usernameField = config.getString(PROPERTY_USERNAME_FIELD);
    if (usernameField != null) {
      mongoRealm.setUsernameField(usernameField);
    }

    String saltstyle = config.getString(PROPERTY_SALT_STYLE);
    if (saltstyle != null) {
      mongoRealm.setSaltStyle(SaltStyle.valueOf(saltstyle));
    }

    boolean permissionsLookupEnabled = config.getBoolean(PROPERTY_PERMISSIONLOOKUP_ENABLED, false);
    mongoRealm.setPermissionsLookupEnabled(permissionsLookupEnabled);

    boolean usernameMustUnique = config.getBoolean(PROPERTY_USERNAME_UNIQUE, true);
    mongoRealm.setUsernameMustUnique(usernameMustUnique);

    mongoRealm.init();
    this.securityManager = new DefaultSecurityManager(mongoRealm);
    this.realm = mongoRealm;
  }

}
