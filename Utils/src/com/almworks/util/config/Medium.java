package com.almworks.util.config;

import java.util.List;

/**
 * @author : Dyoma
 */
public interface Medium extends ReadonlyMedium {
  void setSettings(String settingName, List<String> values);

  Medium createSubset(String subsetName);

  void removeMe();

  void clear();

  Medium EMPTY = new EmptyMedium();

  class EmptyMedium implements Medium {
    private EmptyMedium() {
    }

    public void setSettings(String settingName, List<String> values) {
    }

    public Medium createSubset(String subsetName) {
      return EMPTY;
    }

    public void removeMe() {
    }

    public void clear() {
    }

    public SubMedium<String> getSettings() {
      return SubMedium.EMPTY;
    }

    public SubMedium getSubsets() {
      return SubMedium.EMPTY;
    }

    public String getName() {
      return null;
    }
  }
}
