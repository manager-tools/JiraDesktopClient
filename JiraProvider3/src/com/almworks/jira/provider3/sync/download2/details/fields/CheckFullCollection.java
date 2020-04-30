package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Collection;

public class CheckFullCollection implements JsonIssueField {
  public static final JsonIssueField INSTANCE = new CheckFullCollection();

  private static final JSONKey<Integer> TOTAL = JSONKey.integer("total");
  private static final JSONKey<Integer> START_AT = JSONKey.integer("startAt");
  private static final JSONKey<Integer> MAX_RESULTS = JSONKey.integer("maxResults");

  @Override
  public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
    if (jsonValue == null) return null;
    JSONObject obj = Util.castNullable(JSONObject.class, jsonValue);
    if (obj == null) LogHelper.error("Expected object", jsonValue);
    else LogHelper.assertError(isFullCollection(obj), obj);
    return null;
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    return null;
  }

  public static boolean isFullCollection(JSONObject obj) {
    Integer total = CheckFullCollection.TOTAL.getValue(obj);
    Integer startAt = CheckFullCollection.START_AT.getValue(obj);
    Integer maxResults = CheckFullCollection.MAX_RESULTS.getValue(obj);
    if (total == null || startAt == null || maxResults == null) {
      LogHelper.error("Missing data", total, startAt, maxResults);
      return false;
    }
    return startAt == 0 && total <= maxResults;
  }
}
