package com.almworks.util.properties;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.TypedKeyRegistry;

import java.io.*;

public class StorableMapTests extends BaseTestCase {
  private static final TypedKeyRegistry ALL = new TypedKeyRegistry();
  private static StorableKey<String> NAME = new StorableKey<String>("name", String.class, ALL);
  private static StorableKey<Integer> AGE = new StorableKey<Integer>("age", Integer.class, ALL);
  private static StorableKey<String> STATE = new StorableKey<String>("state", String.class, ALL);

  public void testBasicOperations() throws IOException, StorableException {
    StorableMap map = createMap1();
    map = storeRestoreMap(map);
    assertEquals("Vasya", NAME.get(map));
    assertEquals(19, AGE.get(map).intValue());
  }

  private StorableMap storeRestoreMap(StorableMap map) throws IOException, StorableException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    map.store(new DataOutputStream(baos));
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    map = map.newMap();
    map.restore(new DataInputStream(bais), NAME);
    return map;
  }

  private StorableMap createMap1() {
    StorableMap map = StorableMap.Factory.create();
    NAME.put(map, "Vasya");
    AGE.put(map, new Integer(19));
    map.fix();
    return map;
  }

  public void testEqualsAndHashCode() throws StorableException, IOException {
    StorableMap map = createMap1();
    StorableMap map2 = storeRestoreMap(map);
    assertEquals(map.hashCode(), map2.hashCode());
    assertEquals(map, map2);
  }

  public void testNotEquals() throws StorableException, IOException {
    StorableMap map = createMap1();
    StorableMap map2;

    map2 = StorableMap.Factory.create();
    NAME.put(map2, "Vasya");
    AGE.put(map2, new Integer(18));
    map2.fix();
    assertNotSame(map, map2);

    map2 = StorableMap.Factory.create();
    NAME.put(map2, "Vasya");
    map2.fix();
    assertNotSame(map, map2);

    map2 = StorableMap.Factory.create();
    NAME.put(map2, "Vasya");
    AGE.put(map2, new Integer(19));
    STATE.put(map2, "worried");
    map2.fix();
    assertNotSame(map, map2);
  }
}
