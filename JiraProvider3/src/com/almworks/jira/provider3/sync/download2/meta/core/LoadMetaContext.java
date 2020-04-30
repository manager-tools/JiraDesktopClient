package com.almworks.jira.provider3.sync.download2.meta.core;

import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LoadMetaContext {
  private final Map<LoadedMetaKey<?>, Object> myValues = Collections15.hashMap();

  public <T> void putLoadedData(LoadedMetaKey<T> key, T data) {
    if (myValues.containsKey(key)) LogHelper.error("Already loaded", key, key.getFrom(myValues), data);
    key.putTo(myValues, data);
  }

  @NotNull
  public <T> T getData(LoadedMetaKey<T> key) throws JiraInternalException {
    T data = getDataOrNull(key);
    if (data == null) {
      LocalizedAccessor.Value message = key.getMissingMessage();
      LogHelper.warning("Missing message for key", key);
      throw new JiraInternalException(message != null ? message.create() : key.getName());
    }
    return data;
  }

  public <T> T getDataOrNull(LoadedMetaKey<T> key) {
    T data = key.getFrom(myValues);
    if (data == null && key.getMissingMessage() != null) LogHelper.warning("Data is not loaded", key);
    return data;
  }
}
