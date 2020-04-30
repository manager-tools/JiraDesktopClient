package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.schema.ServerComment;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;

import java.util.Date;

public class JRComment {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> BODY = JSONKey.textTrimLines("body");
  public static final JSONKey<Entity> AUTHOR = JRUser.jsonKey("author");
  public static final JSONKey<Date> CREATED = JSONKey.dateTime("created");
  public static final JSONKey<Entity> UPDATE_AUTHOR = JRUser.jsonKey("updateAuthor");
  public static final JSONKey<Date> UPDATED = JSONKey.dateTime("updated");
  public static final JSONKey<Entity> VISIBILITY = JRVisibility.jsonKey("visibility");

  public static final Convertor<Object, Entity> PARTIAL_JSON_CONVERTOR =
    new EntityParser.Builder()
      .map(ID, ServerComment.ID)
      .map(BODY, ServerComment.TEXT)
      .map(AUTHOR, ServerComment.AUTHOR)
      .map(CREATED, ServerComment.CREATED)
      .map(UPDATE_AUTHOR, ServerComment.EDITOR)
      .map(UPDATED, ServerComment.UPDATED)
      .set(ServerComment.SECURITY, null)
      .map(VISIBILITY, ServerComment.SECURITY)
      .createPartialConvertor(ServerComment.TYPE);

}
