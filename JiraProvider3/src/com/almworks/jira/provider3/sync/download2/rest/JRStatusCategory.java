package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.schema.ServerStatusCategory;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;

public class JRStatusCategory {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> KEY = JSONKey.text("key");
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> COLOR_NAME = JSONKey.text("colorName");

  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerStatusCategory.ID)
      .map(KEY, ServerStatusCategory.KEY)
      .map(NAME, ServerStatusCategory.NAME)
      .map(COLOR_NAME, ServerStatusCategory.COLOR_NAME)
      .create(null);
  public static final Convertor<Object, Entity> JSON_CONVERTOR =  new EntityParser.AsConvertor(ServerStatusCategory.TYPE, PARSER);
}
