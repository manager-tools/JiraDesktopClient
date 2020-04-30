package com.almworks.api.store;


public interface StoreAccess {
  void store(byte[] data);

  byte[] load();

  void clear();
}
