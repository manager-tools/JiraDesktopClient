package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import org.json.simple.JSONObject;

public class JRField {
  /**
   * Map of fieldId -> field details<br>
   * /issue/createmeta - in project/type<br>
   * /issue/KEY/editmeta - in root object
   *
   */
  public static final JSONKey<JSONObject> FIELDS  =JSONKey.object("fields");
  /**
   * /field
   */
  public static final JSONKey<String> ID = JSONKey.text("id");
  /**
   * /field, /issue/createmeta, /issue/editmeta|fields|_fieldName_
   */
  public static final JSONKey<String> NAME = JSONKey.text("name");
  /**
   * /field
   * @deprecated avoid usage of this flag since labels if JIRA system field, but it is "custom" from JC point of view
   */
  @SuppressWarnings("UnusedDeclaration") @Deprecated
  public static final JSONKey<Boolean> CUSTOM = JSONKey.bool("custom");
  public static final JSONKey<JSONObject> SCHEMA = JSONKey.object("schema");
  public static final JSONKey<String> SCHEMA_CUSTOM = JSONKey.text("custom");
  /**
   * @deprecated use String field id instead. Labels has no integer id, however JC treats labels as custom field.
   */
  @SuppressWarnings("UnusedDeclaration") @Deprecated
  private static final JSONKey<Integer> SCHEMA_CUSTOM_ID = JSONKey.integer("customId");
  public static final JSONKey<String> SCHEMA_SYSTEM = JSONKey.text("system");
  /**
   * /issue/createmeta
   */
  public static final ArrayKey<JSONObject> ALLOWED_VALUES = ArrayKey.objectArray("allowedValues");

  /**
   * /issue/editmeta|fields|_fieldName_
   */
  public static final JSONKey<Boolean> REQUIRED = JSONKey.bool("required");
  /**
   * /issue/editmeta|fields|_fieldName_
   */
  public static final ArrayKey<String> OPERATIONS = ArrayKey.textArray("operations");

}
