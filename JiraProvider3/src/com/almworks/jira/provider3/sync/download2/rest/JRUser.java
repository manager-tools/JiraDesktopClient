package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;

public class JRUser {
  public static final JSONKey<String> ID = JSONKey.textLower("name");
  public static final JSONKey<String> NAME = JSONKey.text("displayName");
  public static final JSONKey<String> ICON_16 = JRAvatar.EXT_URL_16;
  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerUser.ID)
      .map(NAME, ServerUser.NAME)
      .create(null);
  private static final Convertor<Object, Entity> CONVERTOR = new EntityParser.AsConvertor(ServerUser.TYPE, PARSER);

  public static JSONKey<Entity> jsonKey(String key) {
    return new JSONKey<Entity>(key, CONVERTOR);
  }
}
