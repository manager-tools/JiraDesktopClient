package com.almworks.util.config;

/**
 * @author : Dyoma
 */
public interface MediumWatcher {
  MediumWatcher BLIND = new MediumWatcher() {
    public void onMediumUpdated() {
    }
  };

  void onMediumUpdated();
}
