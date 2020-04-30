package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.sync.schema.ServerIssueType;
import com.almworks.restconnector.json.JSONKey;

public class JRIssueType {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> DESCRIPTION = JSONKey.text("description");
  public static final JSONKey<String> ICON = JSONKey.text("iconUrl");
  public static final JSONKey<Boolean> SUBTASK = JSONKey.bool("subtask");

  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerIssueType.ID)
      .map(NAME, ServerIssueType.NAME)
      .map(ICON, ServerIssueType.ICON_URL)
      .map(DESCRIPTION, ServerIssueType.DESCRIPTION)
      .create(null);// todo Subtask ??

}
