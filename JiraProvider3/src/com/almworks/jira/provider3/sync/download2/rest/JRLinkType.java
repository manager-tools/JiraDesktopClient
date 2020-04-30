package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.schema.ServerLinkType;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;

public class JRLinkType {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> OUTWARD = JSONKey.text("outward");
  public static final JSONKey<String> INWARD = JSONKey.text("inward");
  public static final JSONKey<String> NAME = JSONKey.text("name");

  public static final EntityParser PARSER = new EntityParser.Builder()
    .map(ID, ServerLinkType.ID)
    .map(OUTWARD, ServerLinkType.OUTWARD_DESCRIPTION)
    .map(INWARD, ServerLinkType.INWARD_DESCRIPTION)
    .map(NAME, ServerLinkType.NAME)
    .create(null);
  public static final Convertor<Object, Entity> JSON_CONVERTOR = new EntityParser.AsConvertor(ServerLinkType.TYPE, PARSER);

}
