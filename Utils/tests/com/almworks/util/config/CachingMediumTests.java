package com.almworks.util.config;


import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author dyoma
 */
public class CachingMediumTests extends BaseTestCase {
  private CollectionsCompare CHECK = new CollectionsCompare();

  public void testIntiallyEmptySettings() {
    MapMedium mapMedium = new MapMedium(null, "root");
    Medium medium = new CachingMedium(mapMedium);
    assertEquals("root", medium.getName());
    String[] array123 = new String[]{"1", "2", "3"};
    medium.setSettings("a", Arrays.asList(array123));
    SubMedium<String> settings = medium.getSettings();
    CHECK.order(array123, settings.getAll("a"));
    CHECK.order(array123, settings.getAll(null));
    assertTrue(settings.isSet("a"));
    assertFalse(settings.isSet("b"));
    medium.setSettings("b", Collections.singletonList("x"));

    CHECK.unordered(mapMedium.getSettings().getAllNames(), new String[]{"a", "b"});
    CHECK.order(array123, mapMedium.getSettings().getAll("a"));
    CHECK.singleElement("x", mapMedium.getSettings().getAll("b"));
  }

  public void testInitallyEmptySubsets() {
    MapMedium mapMedium = new MapMedium(null, "root");
    Medium medium = new CachingMedium(mapMedium);
    Medium s11 = medium.createSubset("s1");
    assertTrue(medium.getSubsets().isSet("s1"));
    assertSame(s11, medium.getSubsets().get("s1"));
    CHECK.singleElement("s1", medium.getSubsets().getAllNames());
    CHECK.singleElement(s11, medium.getSubsets().getAll("s1"));
    CHECK.singleElement(s11, medium.getSubsets().getAll(null));
    s11.setSettings("a", Collections.singletonList("1"));

    Medium s12 = medium.createSubset("s1");
    Medium[] s1 = new Medium[]{s11, s12};
    CHECK.unordered(medium.getSubsets().getAll("s1"), s1);
    CHECK.unordered(medium.getSubsets().getAll(null), s1);
    CHECK.singleElement("s1", medium.getSubsets().getAllNames());

    CHECK.singleElement("s1", mapMedium.getSubsets().getAllNames());
    CHECK.size(2, mapMedium.getSubsets().getAll(null));

    s12.removeMe();
    CHECK.singleElement(s11, medium.getSubsets().getAll("s1"));
    CHECK.size(1, mapMedium.getSubsets().getAll("s1"));
    CHECK.singleElement("a", mapMedium.getSubsets().getAll("s1").get(0).getSettings().getAllNames());

    Medium s2 = medium.createSubset("s2");
    String[] s1_s2 = new String[]{"s1", "s2"};
    CHECK.unordered(medium.getSubsets().getAllNames(), s1_s2);
    CHECK.unordered(mapMedium.getSubsets().getAllNames(), s1_s2);
    s2.removeMe();
    CHECK.singleElement("s1", medium.getSubsets().getAllNames());
    CHECK.singleElement("s1", mapMedium.getSubsets().getAllNames());
  }

  public void testInitialNotEmpty() {
    MapMedium mapMedium = new MapMedium(null, "root");
    mapMedium.createSubset("s1").setSettings("a", Collections.singletonList("1"));
    mapMedium.createSubset("s2").setSettings("b", Collections.singletonList("2"));
    String[] array34 = new String[]{"3", "4"};
    mapMedium.setSettings("c", Arrays.asList(array34));

    Medium medium = new CachingMedium(mapMedium);
    assertTrue(medium.getSettings().isSet("c"));
    CHECK.order(array34, medium.getSettings().getAll("c"));
    SubMedium<? extends Medium> subsets = medium.getSubsets();
    CHECK.size(2, subsets.getAllNames());
    assertEquals("1", subsets.getAll("s1").get(0).getSettings().get("a"));
    assertEquals("2", subsets.getAll("s2").get(0).getSettings().get("b"));
  }

  public void testClear() {
    MapMedium mapMedium = new MapMedium(null, "root");
    mapMedium.setSettings("a", Collections.singletonList("1"));
    mapMedium.createSubset("s");
    Medium medium = new CachingMedium(mapMedium);
    medium.setSettings("b", Collections.singletonList("2"));
    medium.createSubset("ss");
    medium.clear();
    CHECK.empty(medium.getSettings().getAllNames());
    CHECK.empty(medium.getSettings().getAll(null));
    CHECK.empty(medium.getSubsets().getAllNames());
    CHECK.empty(medium.getSubsets().getAll(null));
    CHECK.empty(mapMedium.getSubsets().getAllNames());
    CHECK.empty(mapMedium.getSettings().getAllNames());
  }

  public void testSetEmptySettings() {
    Medium medium = new CachingMedium(new MapMedium(null, "root"));
    medium.setSettings("a", Collections.singletonList("1"));
    medium.setSettings("a", Collections15.<String>emptyList());
    CHECK.empty(medium.getSettings().getAllNames());
    assertFalse(medium.getSettings().isSet("a"));

    medium.setSettings("b", Collections15.<String>emptyList());
    CHECK.empty(medium.getSettings().getAllNames());
    assertFalse(medium.getSettings().isSet("b"));
  }

  public void testRemoveLastSubset() {
    Medium medium = new CachingMedium(new MapMedium(null, "root"));
    medium.createSubset("s").removeMe();
    assertFalse(medium.getSubsets().isSet("s"));
    CHECK.empty(medium.getSubsets().getAllNames());
  }

  public void testIsSetNull() {
    Medium medium = new CachingMedium(new MapMedium(null, "root"));
    assertFalse(medium.getSettings().isSet(null));
    assertFalse(medium.getSubsets().isSet(null));
    medium.setSettings("a", Collections.singletonList("1"));
    assertTrue(medium.getSettings().isSet(null));
    assertFalse(medium.getSubsets().isSet(null));
    Medium s = medium.createSubset("s");
    assertTrue(medium.getSubsets().isSet(null));
    s.removeMe();
    assertFalse(medium.getSubsets().isSet(null));
    medium.setSettings("a", Collections15.<String>emptyList());
    assertFalse(medium.getSettings().isSet("a"));
  }

  public void testClearThenRemove() {
    Medium medium = new CachingMedium(new MapMedium(null, "root"));
    Medium s = medium.createSubset("s");
    CHECK.size(1, medium.getSubsets().getAllNames());
    medium.clear();
    CHECK.empty(medium.getSubsets().getAllNames());
    s.removeMe();
    CHECK.empty(medium.getSubsets().getAllNames());
  }
}
