package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.remotedata.issue.fields.JsonUserParser;
import com.almworks.jira.provider3.sync.schema.ServerWorklog;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;

import java.util.Date;

public class JRWorklog {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<Entity> AUTHOR = JsonUserParser.jsonKey("author");
  public static final JSONKey<Date> CREATED = JSONKey.dateTime("created");
  public static final JSONKey<Entity> UPDATE_AUTHOR = JsonUserParser.jsonKey("updateAuthor");
  public static final JSONKey<Date> UPDATED = JSONKey.dateTime("updated");
  public static final JSONKey<Integer> TIME_SECONDS = JSONKey.integer("timeSpentSeconds");
  public static final JSONKey<Date> STARTED = JSONKey.dateTime("started");
  public static final JSONKey<String> COMMENT = JSONKey.textTrimLines("comment");
  public static final JSONKey<Entity> VISIBILITY = JRVisibility.jsonKey("visibility");

  public static final Convertor<Object, Entity> PARTIAL_JSON_CONVERTOR =
    new EntityParser.Builder()
      .map(ID, ServerWorklog.ID)
      .map(AUTHOR, ServerWorklog.AUTHOR)
      .map(CREATED, ServerWorklog.CREATED)
      .map(UPDATE_AUTHOR, ServerWorklog.EDITOR)
      .map(UPDATED, ServerWorklog.UPDATED)
      .map(TIME_SECONDS, ServerWorklog.TIME_SECONDS)
      .map(STARTED, ServerWorklog.START_DATE)
      .map(COMMENT, ServerWorklog.COMMENT)
      .set(ServerWorklog.SECURITY, null)
      .map(VISIBILITY, ServerWorklog.SECURITY)
      .createPartialConvertor(ServerWorklog.TYPE);
}
