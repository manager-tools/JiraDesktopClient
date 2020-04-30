package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.restconnector.json.JSONKey;

public class JRLink {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<Entity> LINK_TYPE = new JSONKey<Entity>("type", JRLinkType.JSON_CONVERTOR);
  public static final JSONKey<Entity> INWARD_ISSUE = new JSONKey<Entity>("inwardIssue", JRIssue.DUMMY_JSON_CONERTOR);
  public static final JSONKey<Entity> OUTWARD_ISSUE = new JSONKey<Entity>("outwardIssue", JRIssue.DUMMY_JSON_CONERTOR);
}
