package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.jira.provider3.sync.schema.ServerAttachment;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.collections.Convertor;

import java.util.Date;

public class JRAttachment {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<Entity> AUTHOR = JRUser.jsonKey("author");
  public static final JSONKey<Date> CREATED = JSONKey.dateTime("created");
  public static final JSONKey<String> FILENAME = JSONKey.text("filename");
  public static final JSONKey<String> MIME_TYPE = JSONKey.text("mimeType");
  public static final JSONKey<String> CONTENT = JSONKey.text("content");
  public static final JSONKey<String> SIZE_STRING = JSONKey.textOrInteger("size");

  public static final Convertor<Object, Entity> PARTIAL_JSON_CONVERTOR =
    new EntityParser.Builder()
      .map(ID, ServerAttachment.ID)
      .map(AUTHOR, ServerAttachment.AUTHOR)
      .map(CREATED, ServerAttachment.DATE)
      .map(FILENAME, ServerAttachment.FILE_NAME)
      .map(MIME_TYPE, ServerAttachment.MIME_TYPE)
      .map(CONTENT, ServerAttachment.FILE_URL)
      .map(SIZE_STRING, ServerAttachment.SIZE_STRING)
      .createPartialConvertor(ServerAttachment.TYPE);
}
