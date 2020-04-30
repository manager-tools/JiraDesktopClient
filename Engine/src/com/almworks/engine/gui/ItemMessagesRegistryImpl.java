package com.almworks.engine.gui;

import org.almworks.util.Collections15;

import java.util.List;

public class ItemMessagesRegistryImpl implements ItemMessagesRegistry {
  private final List<ItemMessageProvider> myProviders = Collections15.arrayList();

  public void addMessageProvider(ItemMessageProvider provider) {
    synchronized (myProviders) {
      myProviders.add(provider);
    }
  }

  public ItemMessageProvider[] getProviders() {
    synchronized (myProviders) {
      return myProviders.toArray(new ItemMessageProvider[myProviders.size()]);
    }
  }
}
