package com.almworks.api.passman;

import com.almworks.util.properties.StorableKey;
import com.almworks.util.properties.StorableMap;
import org.almworks.util.TypedKeyRegistry;

public class PMCredentials {
  private static final TypedKeyRegistry<Key> ourRegistry = new TypedKeyRegistry<Key>();

  public static final Key<String> PASSWORD = new Key<String>("password", String.class);
  public static final Key<String> USERNAME = new Key<String>("username", String.class);
  public static final Key<String> AUTHTYPE = new Key<String>("authtype", String.class);

  private final StorableMap myMap;

  public PMCredentials(String username, String password) {
    myMap = StorableMap.Factory.create();
    USERNAME.put(myMap, username);
    PASSWORD.put(myMap, password);
    myMap.fix();
  }

  public PMCredentials() {
    myMap = StorableMap.Factory.create();
  }

  public StorableMap getMap() {
    return myMap;
  }

  public String getPassword() {
    return PASSWORD.get(myMap);
  }

  public String getUsername() {
    return USERNAME.get(myMap);
  }

  public String getAuthType() {
    return AUTHTYPE.get(myMap);
  }

  public int hashCode() {
    return myMap.hashCode();
  }

  public boolean equals(Object obj) {
    return ((obj instanceof PMCredentials) && (((PMCredentials) obj).getMap().equals(myMap)));
  }

  public String toString() {
    return getUsername() + (getPassword() == null ? ":null" : ":***") + ":" + getAuthType();
  }

  public static final class Key<T> extends StorableKey<T> {
    private Key(String name, Class<T> valueClass) {
      super(name, valueClass, ourRegistry);
    }
  }
}
