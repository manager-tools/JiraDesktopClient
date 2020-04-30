package com.almworks.util.config;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * @author : Dyoma
 */
class SynchronizedConfiguration extends Configuration {
  public SynchronizedConfiguration(Medium medium, MediumWatcher watcher) {
    super(medium, watcher);
    assert watcher != null;
  }

  public void setSetting(String settingName, String value) {
    synchronized (getLock()) {
      super.setSetting(settingName, value);
    }
  }

  public String getOrSetDefault(String settingName, String defaultValue) {
    synchronized (getLock()) {
      return super.getOrSetDefault(settingName, defaultValue);
    }
  }

  public void setSettings(String settingName, String[] values) {
    synchronized (getLock()) {
      super.setSettings(settingName, values);
    }
  }

  public void setSettings(String settingName, List<String> values) {
    synchronized (getLock()) {
      super.setSettings(settingName, values);
    }
  }

  public void removeSettings(String settingName) {
    synchronized (getLock()) {
      super.removeSettings(settingName);
    }
  }

  public void removeSubsets(String subsetName) {
    synchronized (getLock()) {
      super.removeSubsets(subsetName);
    }
  }

  public Configuration createSubset(String subsetName) {
    synchronized (getLock()) {
      return super.createSubset(subsetName);
    }
  }

  public void removeMe() {
    synchronized (getLock()) {
      super.removeMe();
    }
  }

  public void clear() {
    synchronized (getLock()) {
      super.clear();
    }
  }

  @NotNull
  public Configuration getSubset(String name) {
    synchronized (getLock()) {
      return super.getSubset(name);
    }
  }

  public List<Configuration> getAllSubsets(String name) {
    synchronized (getLock()) {
      return super.getAllSubsets(name);
    }
  }

  public List<Configuration> getAllSubsets() {
    synchronized (getLock()) {
      return super.getAllSubsets();
    }
  }

  public Configuration getOrCreateSubset(String name) {
    synchronized (getLock()) {
      return super.getOrCreateSubset(name);
    }
  }

  public boolean isSet(String settingOrSubsetName) {
    synchronized (getLock()) {
      return super.isSet(settingOrSubsetName);
    }
  }

  public String getSetting(String settingName, String defaultValue) {
    synchronized (getLock()) {
      return super.getSetting(settingName, defaultValue);
    }
  }

  public String getMandatorySetting(String settingName) throws NoSettingException {
    synchronized (getLock()) {
      return super.getMandatorySetting(settingName);
    }
  }

  @NotNull
  public List<String> getAllSettings(String settingName) {
    synchronized (getLock()) {
      return super.getAllSettings(settingName);
    }
  }

  public Collection<String> getAllSettingNames() {
    synchronized (getLock()) {
      return super.getAllSettingNames();
    }
  }

  public Collection<String> getAllSubsetNames() {
    synchronized (getLock()) {
      return super.getAllSubsetNames();
    }
  }

  public boolean isEmpty() {
    synchronized (getLock()) {
      return super.isEmpty();
    }
  }

  public String getName() {
    synchronized (getLock()) {
      return super.getName();
    }
  }

  protected SynchronizedConfiguration createConfig(ReadonlyMedium medium) {
    return new SynchronizedConfiguration((Medium) medium, getRoot());
  }

  private Object getLock() {
    return getRoot();
  }
}
