package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.sync.schema.ServerVersion;
import com.almworks.restconnector.json.JSONKey;

import java.util.Date;

public class JRVersion {
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<Boolean> ARCHIVED = JSONKey.bool("archived");
  public static final JSONKey<Boolean> RELEASED = JSONKey.bool("released");
  public static final JSONKey<Date> RELEASED_DATE = JSONKey.date("releaseDate");

  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerVersion.ID)
      .map(NAME, ServerVersion.NAME)
      .map(ARCHIVED, ServerVersion.ARCHIVED)
      .map(RELEASED, ServerVersion.RELEASED)
      .map(RELEASED_DATE, ServerVersion.RELEASE_DATE)
      .create(SupplyReference.supplyProject(ServerVersion.PROJECT));
}
