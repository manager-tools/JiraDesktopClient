package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.HashMap;

/**
 * @author : Dyoma
 */
public class InheritedMappingTests extends BaseTestCase {
  private CollectionsCompare CHECK = new CollectionsCompare();
  private HashMap myParent = Collections15.hashMap();
  private InheritedMapping myMap = new InheritedMapping(myParent);

  protected void tearDown() throws Exception {
    myParent = null;
    myMap = null;
    CHECK = null;
    super.tearDown();
  }

  public void testUsingParent() {
    assertTrue(myMap.isEmpty());
    CHECK.empty(myMap.values());

    myParent.put("1", "v");
    assertFalse(myMap.isEmpty());
    checkMapping("1", "v");
    CHECK.singleElement("v", myMap.values());

    myParent.put("1", null);
    assertFalse(myMap.isEmpty());
    checkMapping("1", null);
    CHECK.singleElement(null, myMap.values());
  }

  public void testOverriding() {
    myParent.put("1", "v");
    myMap.put("1", "o");
    checkMapping("1", "o");
    myMap.put("1", null);
    checkMapping("1", null);
    myMap.remove("1");
    checkMapping("1", "v");
    myMap.remove("1");
    checkMapping("1", "v");
    myMap.putAll(Collections.singletonMap("1", "o"));
    checkMapping("1", "o");
    myMap.clear();
    checkMapping("1", "v");
  }

  private void checkMapping(String key, String value) {
    assertEquals(value, myMap.get(key));
    assertTrue(myMap.containsKey(key));
    assertTrue(myMap.containsValue(value));
    assertTrue(myMap.values().contains(value));
  }
}
