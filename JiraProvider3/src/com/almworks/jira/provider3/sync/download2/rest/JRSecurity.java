package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.sync.schema.ServerSecurity;
import com.almworks.restconnector.json.JSONKey;

public class JRSecurity {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> DESCRIPTION = JSONKey.text("description");

  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerSecurity.ID)
      .map(NAME, ServerSecurity.NAME)
//      .map(DESCRIPTION, ServerSecurity.DESCRIPTION)
      .create(null);
}
