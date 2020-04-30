package com.almworks.api.passman;

import com.almworks.util.properties.StorableKey;
import com.almworks.util.properties.StorableMap;
import org.almworks.util.TypedKeyRegistry;

public class PMDomain {
  private static final TypedKeyRegistry<Key> ourRegistry = new TypedKeyRegistry<Key>();

  public static final Key<String> KIND = new Key<String>("kind", String.class);
  public static final Key<String> HOST = new Key<String>("host", String.class);
  public static final Key<Integer> PORT = new Key<Integer>("port", Integer.class);
  public static final Key<String> REALM = new Key<String>("realm", String.class);

  private final StorableMap myMap;

  public PMDomain() {
    myMap = StorableMap.Factory.create();
  }

  public PMDomain(String kind, String host, int port, String realm) {
    myMap = StorableMap.Factory.create();
    KIND.put(myMap, kind);
    HOST.put(myMap, host);
    PORT.put(myMap, port);
    REALM.put(myMap, realm);
    myMap.fix();
  }

  public PMDomain(String host, int port) {
    myMap = StorableMap.Factory.create();
    HOST.put(myMap, host);
    PORT.put(myMap, port);
    myMap.fix();
  }

  public StorableMap getMap() {
    return myMap;
  }

  public int hashCode() {
    return myMap.hashCode();
  }

  public boolean equals(Object obj) {
    return ((obj instanceof PMDomain) && (((PMDomain) obj).getMap().equals(myMap)));
  }

  public static final class Key <T> extends StorableKey<T> {
    public Key(String name, Class<T> valueClass) {
      super(name, valueClass, ourRegistry);
    }
  }
}
