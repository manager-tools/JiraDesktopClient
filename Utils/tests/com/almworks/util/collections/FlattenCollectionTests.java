package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author : Dyoma
 */
public class FlattenCollectionTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();

  public void testDisjoint() {
    HashSet<Object> first = Collections15.hashSet();
    HashSet<Object> second = Collections15.hashSet();
    HashSet<Object> last = Collections15.hashSet();
    first.add("1");
    first.add("2");
    second.add("a");
    FlattenCollection collection = new FlattenCollection(Arrays.asList(new Set[]{first, null, second, last}));
    String[] expected = new String[]{"1", "2", "a"};
    checkElements(expected, collection);
    second.remove("a");
    checkElements(new String[]{"1", "2"}, collection);
    last.add("x");
    checkElements(new String[]{"1", "2", "x"}, collection);
    first.clear();
    CHECK.singleElement("x", collection.iterator());
    assertEquals(1, collection.size());
  }

  private void checkElements(String[] expected, FlattenCollection collection) {
    CHECK.unordered(expected, collection.iterator());
    CHECK.unordered(collection, expected);
  }

  public void testIsEmpty() {
    HashSet<Object> first = Collections15.hashSet();
    HashSet<Object> last = Collections15.hashSet();
    FlattenCollection collection = new FlattenCollection(Arrays.asList(new Set[]{first, null, last}));
    assertTrue(collection.isEmpty());
    first.add("1");
    assertFalse(collection.isEmpty());
    last.add("2");
    first.clear();
    assertFalse(collection.isEmpty());
  }
}
