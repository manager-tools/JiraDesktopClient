package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.sync.schema.ServerPriority;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.SelfIdExtractor;
import com.almworks.util.collections.Convertor;

public class JRPriority {
  private static final Convertor<Object, Integer> ID_EXTRACTOR = new SelfIdExtractor("/rest/api/2/priority/");
  public static final JSONKey<Integer> ID = new JSONKey<Integer>("self", ID_EXTRACTOR);
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> COLOR = JSONKey.text("statusColor");
  public static final JSONKey<String> DESCRIPTION = JSONKey.text("description");
  public static final JSONKey<String> ICON = JSONKey.text("iconUrl");
  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerPriority.ID)
      .map(NAME, ServerPriority.NAME)
      .map(ICON, ServerPriority.ICON_URL)
      .create(null); // todo other
}
