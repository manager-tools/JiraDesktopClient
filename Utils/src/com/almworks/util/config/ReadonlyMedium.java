package com.almworks.util.config;

/**
 * @author : Dyoma
 */
public interface ReadonlyMedium<M extends ReadonlyMedium> {
  SubMedium<String> getSettings();

  SubMedium<? extends M> getSubsets();

  String getName();

  ReadonlyMedium EMPTY_READONLY = new ReadonlyMedium() {
    public SubMedium<String> getSettings() {
      return SubMedium.EMPTY;
    }

    public SubMedium getSubsets() {
      return SubMedium.EMPTY;
    }

    public String getName() {
      return null;
    }
  };
}
