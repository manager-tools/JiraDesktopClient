package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Decomposes JSON object. Create united result collection by processing each known value key.<br>
 * Supports {@link com.almworks.jira.provider3.sync.download2.details.JsonIssueField#loadNull() send null} to missing keys (if JSON object does not contain any value for the key)
 */
public class ObjectField implements JsonIssueField {
  private final Map<String, JsonIssueField> myFieldMap;
  /**
   * If false - processes only fields contained in a JSON object.<br>
   * If true - calls {@link com.almworks.jira.provider3.sync.download2.details.JsonIssueField#loadNull()} for every known field missing in the object.
   */
  private final boolean mySendNulls;

  private ObjectField(Map<String, JsonIssueField> fieldMap, boolean sendNulls) {
    myFieldMap = fieldMap;
    mySendNulls = sendNulls;
  }

  public static JsonIssueField getField(String key, JsonIssueField valueLoader, boolean sendNulls) {
    return new ObjectField(Collections.singletonMap(key, valueLoader), sendNulls);
  }

  @Override
  public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
    JSONObject object = Util.castNullable(JSONObject.class, jsonValue);
    if (object == null) {
      LogHelper.error("Expected object", myFieldMap.keySet());
      return null;
    }
    Set<String> missingKeys = mySendNulls ? Collections15.hashSet(myFieldMap.keySet()) : null;
    ArrayList<ParsedValue> result = Collections15.arrayList();
    //noinspection unchecked
    for (Map.Entry<String, Object> entry : ((Map<String, Object>) object).entrySet()) {
      String key = entry.getKey();
      if (missingKeys != null) missingKeys.remove(key);
      Object value = entry.getValue();
      JsonIssueField field = getField(key);
      if (field == null) continue;
      Collection<? extends JsonIssueField.ParsedValue> values = field.loadValue(value);
      if (values != null) result.addAll(values);
    }
    if (missingKeys != null) {
      for (String key : missingKeys) {
        JsonIssueField field = getField(key);
        Collection<? extends JsonIssueField.ParsedValue> values;
        if (field != null) values = field.loadNull();
        else {
          LogHelper.error("Missing", key);
          values = null;
        }
        if (values != null) result.addAll(values);
      }
    }
    return result;
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    if (!mySendNulls) { // decide later
      LogHelper.error("No send nulls", myFieldMap.keySet());
      return null;
    }
    ArrayList<ParsedValue> result = Collections15.arrayList();
    for (JsonIssueField field : myFieldMap.values()) {
      Collection<? extends ParsedValue> values = field.loadNull();
      if (values != null) result.addAll(values);
    }
    return result.isEmpty() ? null : result;
  }

  @Nullable
  public JsonIssueField getField(String key) {
    return myFieldMap.get(key);
  }

  public static class Builder {
    private final Map<String, JsonIssueField> myFieldMap = Collections15.hashMap();
    private final boolean mySendNull;

    public Builder(boolean sendNull) {
      mySendNull = sendNull;
    }

    public Builder add(String key, JsonIssueField field) {
      myFieldMap.put(key, field);
      return this;
    }

    public ObjectField create() {
      return new ObjectField(Collections15.unmodifiableMapCopy(myFieldMap), mySendNull);
    }
  }
}
