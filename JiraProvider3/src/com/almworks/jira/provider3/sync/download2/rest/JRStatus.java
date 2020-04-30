package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.schema.ServerStatus;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;
import org.json.simple.JSONObject;

public class JRStatus {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> ICON = JSONKey.text("iconUrl");
  public static final JSONKey<String> DESCRIPTION = JSONKey.text("description");
  public static final JSONKey<JSONObject> CATEGORY = JSONKey.object("statusCategory");

  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerStatus.ID)
      .map(NAME, ServerStatus.NAME)
      .map(DESCRIPTION, ServerStatus.DESCRIPTION)
      .map(ICON, ServerStatus.ICON_URL)
      .mapEntity(CATEGORY, ServerStatus.CATEGORY, JRStatusCategory.JSON_CONVERTOR, false)
      .create(null);
  public static final Convertor<Object, Entity> JSON_CONVERTOR = new EntityParser.AsConvertor(ServerStatus.TYPE, PARSER);
}
