package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.restconnector.json.JSONKey;
import org.json.simple.JSONObject;

public class JRTransition {
  public static final JSONKey<JSONObject> TO_STATUS = JSONKey.object("to");
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> NAME = JSONKey.textNNTrim("name");
  public static final JSONKey<JSONObject> FIELDS = JSONKey.object("fields");
}
