package com.almworks.api.engine;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.items.api.Database;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;

public interface Engine {
  Role<Engine> ROLE = Role.role("Engine");
  DBNamespace NS = DBNamespace.moduleNs("com.almworks.engine");

  ConnectionManager getConnectionManager();

  @NotNull
  Synchronizer getSynchronizer();

  EngineViews getViews();

  void registerGlobalDescriptor(ConstraintDescriptor descriptor);

  AListModel<ConstraintDescriptor> getGlobalDescriptors();

  Database getDatabase();
}
