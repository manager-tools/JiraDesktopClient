package com.almworks.spi.provider;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.InitializationState;
import com.almworks.util.model.ScalarModel;
import org.jetbrains.annotations.NotNull;

public interface ConnectionContext {
  @NotNull
  ComponentContainer getContainer();

  Connection getConnection();

  String getLastInitializationError();

  void stop();

  ScalarModel<InitializationState> getInitializationState();

  void setInitializationInProgress();

  void setInitializationResult(boolean success, String error);

  void requestReinitialization();
}