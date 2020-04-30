package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.sync.schema.ServerGroup;
import com.almworks.restconnector.json.JSONKey;

public class JRGroup {
  public static final JSONKey<String> ID = JSONKey.textLower("name");
  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerGroup.ID)
      .create(null);
}
