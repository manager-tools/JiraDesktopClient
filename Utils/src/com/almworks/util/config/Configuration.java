package com.almworks.util.config;

import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author : Dyoma
 */
public class Configuration extends ReadonlyConfiguration {
  public static final Configuration EMPTY_CONFIGURATION = new Configuration(Medium.EMPTY, null);
  public static final ReadonlyConfiguration EMPTY_READONLY_CONFIGURATION = EMPTY_CONFIGURATION;

  private final MediumWatcher myWatcher;

  Configuration(Medium medium, MediumWatcher watcher) {
    super(medium);
    myWatcher = watcher;
  }

  public static Configuration createWritable(Medium medium) {
    return createWritable(medium, null);
  }

  public static Configuration createWritable(Medium medium, MediumWatcher watcher) {
    assert medium != null;
    return new Configuration(medium, watcher);
  }

  public static Configuration createSynchonized(Medium medium, MediumWatcher watcher) {
    assert watcher != null;
    return new SynchronizedConfiguration(medium, watcher);
  }

  public void setSetting(String settingName, String value) {
    assert !settings().isSet(settingName) || settings().getAll(settingName).size() == 1 : this + ":" + settingName + ":" + value;
    setSettings(settingName, Collections.singletonList(Util.NN(value)));
    fireUpdated();
  }

  public void setSetting(String name, boolean value) {
    setSetting(name, Boolean.valueOf(value).toString());
  }

  public void setSetting(String name, int value) {
    setSetting(name, Integer.toString(value));
  }

  public void setSetting(String name, long value) {
    setSetting(name, Long.toString(value));
  }

  public String getOrSetDefault(String settingName, String defaultValue) {
    if (settings().isSet(settingName))
      return settings().get(settingName);
    else {
      setSetting(settingName, defaultValue);
      return defaultValue;
    }
  }

  public void setSettings(String settingName, String[] values) {
    setSettings(settingName, Arrays.asList(values));
    fireUpdated();
  }

  public void setSettings(String settingName, List<String> values) {
    if (settingName == null || settingName.length() == 0) {
      assert false : settingName + " " + values;
      return;
    }
    getMedium().setSettings(settingName, values);
    fireUpdated();
  }

  public void removeSettings(String settingName) {
    setSettings(settingName, Collections15.<String>emptyList());
  }

  public void removeSubsets(String subsetName) {
    List<Configuration> subsets = getAllSubsets(subsetName);
    for (Configuration subset : subsets) {
      subset.removeMe();
    }
  }

  @NotNull
  public Configuration createSubset(String subsetName) {
    Configuration subset = createConfig(getMedium().createSubset(subsetName));
    fireUpdated();
    return subset;
  }

  public void removeMe() {
    getMedium().removeMe();
    fireUpdated();
  }

  /**
   * If subset already exists returns it. Otherwise invokes {@link #createSubset(String)} and returns newly created one.
   *
   * @param name name of subset to access or create
   * @return subconfiguration
   */
  @NotNull
  public Configuration getOrCreateSubset(String name) {
    return subsets().isSet(name) ? getSubset(name) : createSubset(name);
  }

  @NotNull
  public Configuration getSubset(String name) {
    return (Configuration) super.getSubset(name);
  }

  public List<Configuration> getAllSubsets(String name) {
    return (List<Configuration>) super.getAllSubsets(name);
  }

  public List<Configuration> getAllSubsets() {
    return (List<Configuration>) super.getAllSubsets();
  }

  SubMedium<Medium> subsets() {
    return (SubMedium<Medium>) super.subsets();
  }

  public void clear() {
    getMedium().clear();
    fireUpdated();
  }

  @NotNull
  protected Configuration createConfig(ReadonlyMedium medium) {
    return new Configuration((Medium) medium, myWatcher);
  }

  private void fireUpdated() {
    if (myWatcher != null)
      myWatcher.onMediumUpdated();
  }

  protected MediumWatcher getRoot() {
    return myWatcher;
  }

  Medium getMedium() {
    return (Medium) super.getMedium();
  }
}
