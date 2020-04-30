package com.almworks.engine.gui;

import com.almworks.util.properties.Role;

public interface ItemMessagesRegistry {
  Role<ItemMessagesRegistry> ROLE = Role.role(ItemMessagesRegistry.class);

  void addMessageProvider(ItemMessageProvider provider);

  ItemMessageProvider[] getProviders();
}
