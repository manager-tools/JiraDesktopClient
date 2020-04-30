package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.sync.schema.ServerComponent;
import com.almworks.restconnector.json.JSONKey;

public class JRComponent {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerComponent.ID)
      .map(NAME, ServerComponent.NAME)
      .create(SupplyReference.supplyProject(ServerComponent.PROJECT));
}
