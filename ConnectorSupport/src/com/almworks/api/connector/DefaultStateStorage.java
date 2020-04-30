package com.almworks.api.connector;

import com.almworks.util.collections.MultiMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.apache.commons.httpclient.Cookie;

import java.util.Map;

public class DefaultStateStorage implements ConnectorStateStorage {
  private final Map<String, String> myMap = Collections15.hashMap();
  private final Map<TypedKey<?>, ?> myRuntimeMap = Collections15.hashMap();
  private MultiMap<String, Cookie> myCookies;

  public void setPersistentString(String key, String value) {
    synchronized (myMap) {
      myMap.put(key, value);
    }
  }

  public String getPersistentString(String key) {
    synchronized (myMap) {
      return myMap.get(key);
    }
  }

  public synchronized void setCookies(MultiMap<String, Cookie> cookies) {
    myCookies = MultiMap.createCopyOrNull(cookies);
  }

  public synchronized MultiMap<String, Cookie> getCookies() {
    return MultiMap.createCopyOrNull(myCookies);
  }

  public void removePersistent(String key) {
    synchronized (myMap) {
      myMap.remove(key);
    }
  }

  public void setPersistentLong(String key, long value) {
    setPersistentString(key, String.valueOf(value));
  }

  public long getPersistentLong(String key) {
    String s = getPersistentString(key);
    try {
      return s == null ? 0 : Long.valueOf(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public <T> void setRuntime(TypedKey<T> key, T value) {
    key.putTo(myRuntimeMap, value);
  }

  public <T> T getRuntime(TypedKey<T> key) {
    return key.getFrom(myRuntimeMap);
  }

  public void clearPersistent() {
    synchronized (myMap) {
      myMap.clear();
    }
    synchronized (this) {
      myCookies = null;
    }
  }
}
