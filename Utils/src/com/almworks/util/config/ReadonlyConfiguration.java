package com.almworks.util.config;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ReadonlyConfiguration {
  private final ReadonlyMedium myMedium;

  public ReadonlyConfiguration(ReadonlyMedium medium) {
    assert medium != null;
    myMedium = medium;
  }

  /**
   * Checks if setting or subset exists in this configuration
   *
   * @param settingOrSubsetName name of setting or subset
   * @return true if setting or subset with given name exists
   */
  public boolean isSet(String settingOrSubsetName) {
    return settings().isSet(settingOrSubsetName) || subsets().isSet(settingOrSubsetName);
  }

  public String getSetting(String settingName, String defaultValue) {
    SubMedium<String> settings = settings();
    return settings.isSet(settingName) ? settings.get(settingName) : defaultValue;
  }

  public boolean getBooleanSetting(String name, boolean defaultValue) {
    return Boolean.valueOf(getSetting(name, Boolean.toString(defaultValue))).booleanValue();
  }

  public int getIntegerSetting(String name, int defaultValue) {
    try {
      String setting = getSetting(name, null);
      if (setting == null)
        return defaultValue;
      else
        return Integer.parseInt(setting);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public long getLongSetting(String name, int defaultValue) {
    try {
      String setting = getSetting(name, null);
      if (setting == null)
        return defaultValue;
      else
        return Long.parseLong(setting);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public String getMandatorySetting(String settingName) throws NoSettingException {
    SubMedium<String> settings = settings();
    if (settings.isSet(settingName))
      return settings.get(settingName);
    else
      throw new NoSettingException(settingName);
  }

  @NotNull
  public List<String> getAllSettings(String settingName) {
    return getAllSettings(settingName, null);
  }

  @NotNull
  public List<String> getAllSettings(String settingName, List<String> buffer) {
    SubMedium<String> settings = settings();
    return settings.getAll(settingName, buffer);
  }

  public Collection<String> getAllSettingNames() {
    return settings().getAllNames();
  }

  @NotNull
  public ReadonlyConfiguration getSubset(String name) {
    if (!subsets().isSet(name))
      return Configuration.EMPTY_CONFIGURATION;
    return createConfig(subsets().get(name));
  }

  public List<? extends ReadonlyConfiguration> getAllSubsets(String name) {
    if (!subsets().isSet(name))
      return Collections15.emptyList();
    List<ReadonlyConfiguration> result = Collections15.arrayList();
    List<? extends ReadonlyMedium> all = subsets().getAll(name);
    for (ReadonlyMedium medium : all) {
      result.add(createConfig(medium));
    }
    return result;
  }

  public List<? extends ReadonlyConfiguration> getAllSubsets() {
    SubMedium<? extends ReadonlyMedium> subsets = subsets();
    List<? extends ReadonlyMedium> all = subsets.getAll();
    if (all.size() == 0)
      return Collections15.emptyList();
    List<ReadonlyConfiguration> result = Collections15.arrayList();
    for (ReadonlyMedium medium : all) {
      result.add(createConfig(medium));
    }
    return result;
  }

  protected ReadonlyConfiguration createConfig(ReadonlyMedium medium) {
    return new ReadonlyConfiguration(medium);
  }

  public Collection<String> getAllSubsetNames() {
    return subsets().getAllNames();
  }

  public String getName() {
    return getMedium().getName();
  }

  SubMedium<String> settings() {
    return getMedium().getSettings();
  }

  SubMedium<? extends ReadonlyMedium> subsets() {
    return getMedium().getSubsets();
  }

  ReadonlyMedium getMedium() {
    return myMedium;
  }

  public static class NoSettingException extends ConfigurationException {
    public NoSettingException(String settingName) {
      super("No setting '" + settingName + "'");
    }
  }

  public boolean isEmpty() {
    return myMedium.getSubsets().getAllNames().size() == 0 && myMedium.getSettings().getAllNames().size() == 0;
  }
}
