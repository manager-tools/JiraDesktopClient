package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;
import org.json.simple.JSONObject;

import java.util.Date;

/**
 * Applicable to issue obtained from /search
 */
public class JRIssue {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> KEY = JSONKey.text("key");

  public static final JSONKey<JSONObject> FIELDS = JSONKey.object("fields");
  private static final JSONKey<Date> F_UPDATED = JSONKey.dateTime("updated");
  private static final JSONKey<Date> F_CREATED = JSONKey.dateTime("created");
  private static final JSONKey<String> F_SUMMARY = JSONKey.textNNTrim("summary");
  private static final JSONKey<JSONObject> F_ISSUE_TYPE = JSONKey.object("issuetype");
  private static final JSONKey<JSONObject> F_PROJECT = JSONKey.object("project");
  private static final JSONKey<JSONObject> F_PARENT = JSONKey.object("parent");
  private static final JSONKey<JSONObject> F_STATUS = JSONKey.object("status");

  public static final JSONKey<Date> UPDATED = JSONKey.composition(FIELDS, F_UPDATED);
  public static final JSONKey<Date> CREATED = JSONKey.composition(FIELDS, F_CREATED);
  public static final JSONKey<String> SUMMARY = JSONKey.composition(FIELDS, F_SUMMARY);
  public static final JSONKey<JSONObject> ISSUE_TYPE = JSONKey.composition(FIELDS, F_ISSUE_TYPE);
  public static final JSONKey<JSONObject> PROJECT = JSONKey.composition(FIELDS, F_PROJECT);
  public static final JSONKey<JSONObject> PARENT = JSONKey.composition(FIELDS, F_PARENT);
  public static final JSONKey<JSONObject> STATUS = JSONKey.composition(FIELDS, F_STATUS);
  public static final EntityParser PARSER = new EntityParser.Builder()
    .map(ID, ServerIssue.ID)
    .map(KEY, ServerIssue.KEY)
    .downloadStage(DownloadStageMark.DUMMY)
    .create(null);
  public static final Convertor<Object, Entity> DUMMY_JSON_CONERTOR = new EntityParser.AsConvertor(ServerIssue.TYPE, PARSER); // todo add fields JCO-1373
}
