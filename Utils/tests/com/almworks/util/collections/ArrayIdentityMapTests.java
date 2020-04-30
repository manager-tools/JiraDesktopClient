package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;

/**
 * @author dyoma
 */
@SuppressWarnings({"RawUseOfParameterizedType", "unchecked"})
public class ArrayIdentityMapTests extends BaseTestCase {
  private static final String KEY_1 = "1";
  private static final String KEY_2 = "2";
  private static final String KEY_3 = "3";

  public void test() {
    ArrayIdentityMap map = new ArrayIdentityMap(2);
    assertEquals(0, map.size());
    assertNull(map.put(KEY_1, "a"));
    assertEquals("a", map.get(KEY_1));
    assertEquals(1, map.size());
    map.put(KEY_2, "b");
    assertEquals(2, map.size());
    assertEquals("a", map.get(KEY_1));
    assertEquals("b", map.get(KEY_2));
    map.put(KEY_3, "c");
    assertEquals(3, map.size());
    assertEquals("a", map.get(KEY_1));
    assertEquals("b", map.get(KEY_2));
    assertEquals("c", map.get(KEY_3));
    assertTrue(map.containsKey(KEY_3));

    assertEquals("b", map.put(KEY_2, "bb"));
    assertEquals(3, map.size());
    assertEquals("a", map.get(KEY_1));
    assertEquals("bb", map.get(KEY_2));
    assertEquals("c", map.get(KEY_3));

    assertNull(map.remove("missingKey"));
    assertEquals(3, map.size());
    assertEquals("bb", map.remove(KEY_2));
    assertEquals(2, map.size());
    assertEquals("a", map.get(KEY_1));
    assertEquals("c", map.get(KEY_3));
    assertFalse(map.containsKey(KEY_2));

    assertEquals("c", map.remove(KEY_3));
    assertEquals(1, map.size());
    assertEquals("a", map.get(KEY_1));
    assertFalse(map.containsKey(KEY_3));

    map.put(KEY_2, "b");
    assertEquals("a", map.remove(KEY_1));
    assertEquals(1, map.size());
    assertEquals("b", map.get(KEY_2));
    map.remove(KEY_2);
    assertEquals(0, map.size());
  }
}
