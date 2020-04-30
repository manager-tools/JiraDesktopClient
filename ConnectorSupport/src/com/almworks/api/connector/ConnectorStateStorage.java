package com.almworks.api.connector;

import com.almworks.util.collections.MultiMap;
import org.almworks.util.TypedKey;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.Nullable;

public interface ConnectorStateStorage {
  void setPersistentLong(String key, long value);

  long getPersistentLong(String key);

  void setPersistentString(String key, String value);

  String getPersistentString(String key);

  void setCookies(MultiMap<String, Cookie> cookies);

  @Nullable
  MultiMap<String, Cookie> getCookies();

  void removePersistent(String key);

  <T> void setRuntime(TypedKey<T> key, T value);

  <T> T getRuntime(TypedKey<T> key);

  void clearPersistent();
}
