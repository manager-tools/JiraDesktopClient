package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.restconnector.json.JSONKey;

public class JRGeneric {
  public static final JSONKey<String> ID_STR = JSONKey.text("id");
  public static final JSONKey<Integer> ID_INT = JSONKey.integer("id");
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> DESCRIPTION = JSONKey.text("description");
  public static final JSONKey<String> VALUE = JSONKey.text("value");

}
