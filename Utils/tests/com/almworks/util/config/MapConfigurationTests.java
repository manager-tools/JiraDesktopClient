package com.almworks.util.config;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Const;

/**
 * @author : Dyoma
 */
public class MapConfigurationTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final Configuration myConfiguration = MapMedium.createConfig();

  public void testEmpty() {
    assertEquals("notSet", myConfiguration.getSetting("a", "notSet"));
    assertFalse(myConfiguration.isSet("a"));
    CHECK.empty(myConfiguration.getAllSettings("a"));
    CHECK.empty(myConfiguration.getAllSubsets("a"));
    Configuration subset = myConfiguration.getSubset("a");
    CHECK.empty(subset.getAllSettings(""));
    subset.setSetting("x", "y");
    CHECK.empty(subset.getAllSettings(""));
  }

  public void testSettings() {
    myConfiguration.setSetting("a", "v");
    assertTrue(myConfiguration.isSet("a"));
    assertEquals("v", myConfiguration.getSetting("a", "notSet"));
    CHECK.singleElement("v", myConfiguration.getAllSettings("a"));
    CHECK.empty(myConfiguration.getAllSettings(""));
    myConfiguration.setSetting("a", "v2");
    assertEquals("v2", myConfiguration.getSetting("a", "notSet"));
    CHECK.singleElement("a", myConfiguration.getAllSettingNames());
    CHECK.singleElement("v2", myConfiguration.getAllSettings("a"));
    CHECK.empty(myConfiguration.getAllSettings(""));
    myConfiguration.clear();
    CHECK.empty(myConfiguration.getAllSettingNames());
  }

  public void testRemoveSetting() {
    myConfiguration.setSetting("a", "b");
    CHECK.singleElement("b", myConfiguration.getAllSettings("a"));
    myConfiguration.setSettings("a", Const.EMPTY_STRINGS);
    CHECK.empty(myConfiguration.getAllSettingNames());
    assertFalse(myConfiguration.isSet("a"));
  }

  public void testSubsets() {
    MapMedium parent = new MapMedium(null, "root");
    assertEquals("root", parent.getName());
    Medium ss = parent.createSubset("ss");
    assertSame(ss, parent.getSubsets().get("ss"));
    CHECK.singleElement("ss", parent.getSubsets().getAllNames());
    CHECK.singleElement(ss, parent.getSubsets().getAll("ss"));
    assertFalse(parent.getSubsets().isSet(""));
    ss.removeMe();
    CHECK.empty(parent.getSubsets().getAllNames());
  }

  public void testCopy() {
    myConfiguration.setSettings("a", new String[]{"1", "2"});
    myConfiguration.createSubset("b").setSetting("x", "1");
    myConfiguration.createSubset("b").setSetting("y", "1");
    Configuration copy = ConfigurationUtil.copy(myConfiguration);
    CHECK.unordered(copy.getAllSettings("a"), new String[]{"1", "2"});
  }

  public void testHaveSameSettings() {
    myConfiguration.setSettings("a", new String[]{"1", "2"});
    myConfiguration.createSubset("b").setSetting("aSetting", "aValue");
    myConfiguration.createSubset("empty");
    checkSameSettings(myConfiguration);
    Configuration other = MapMedium.createConfig();
    checkDifferentSettings(other);

    Configuration subset = other.createSubset("b");
    checkDifferentSettings(other);
    subset.setSetting("aSetting", "aValue");
    checkDifferentSettings(other);
    assertTrue(ConfigurationUtil.haveSameSettings(myConfiguration.getSubset("b"), subset));
    other.setSettings("a", new String[]{"1", "2"});
    checkDifferentSettings(other);
    other.createSubset("empty");
    checkSameSettings(other);
    other.setSettings("a", new String[]{"1"});
    checkDifferentSettings(other);
    other.setSettings("a", new String[]{"1", "2"});
    checkSameSettings(other);

    myConfiguration.createSubset("b").setSetting("aSetting", "aValue");
    myConfiguration.createSubset("b");
    checkSameSettings(myConfiguration);
    other.createSubset("b");
    checkDifferentSettings(other);
    subset = other.createSubset("b");
    checkDifferentSettings(other);
    subset.setSetting("aSetting", "aValue");
    checkSameSettings(other);
    subset.setSetting("aSetting", "otherValue");
    checkDifferentSettings(other);
  }

  public void testClearThenRemove() {
    Configuration subset = myConfiguration.createSubset("s");
    myConfiguration.clear();
    subset.removeMe(); // Should not throw exception
  }

  private void checkSameSettings(Configuration other) {
    Configuration config = ConfigurationUtil.copy(myConfiguration);
    assertTrue(ConfigurationUtil.haveSameSettings(config, other));
    assertTrue(ConfigurationUtil.haveSameSettings(other, config));
  }

  private void checkDifferentSettings(Configuration other) {
    assertFalse(ConfigurationUtil.haveSameSettings(myConfiguration, other));
    assertFalse(ConfigurationUtil.haveSameSettings(other, myConfiguration));
  }
}
