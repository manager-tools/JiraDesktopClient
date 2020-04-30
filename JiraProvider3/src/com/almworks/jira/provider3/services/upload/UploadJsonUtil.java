package com.almworks.jira.provider3.services.upload;

import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class UploadJsonUtil {
  @SuppressWarnings("unchecked")
  public static JSONObject object(String key, @Nullable Object value) {
    JSONObject object = new JSONObject();
    object.put(key, value);
    return object;
  }

  @SuppressWarnings("unchecked")
  public static JSONArray singleObjectElementArray(String key, Object value) {
    JSONArray array = new JSONArray();
    array.add(object(key, value));
    return array;
  }
}
