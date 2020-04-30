package com.almworks.api.engine;

import com.almworks.util.Enumerable;

public class InitializationState extends Enumerable<InitializationState> {
  /**
   * Connection is not initialized, it cannot provide normal service.
   */
  public static final InitializationState NOT_INITIALIZED = new InitializationState("NOT_INITIALIZED");

  /**
   * Connection is not initialized, but the process of initialization goes on.
   */
  public static final InitializationState INITIALIZING = new InitializationState("INITIALIZING");

  /**
   * Connection is initialized.
   */
  public static final InitializationState INITIALIZED = new InitializationState("INITIALIZED");

  /**
   * Connection is initialized, but reinitialization is required, for example because settings have been
   * changed.
   */
  public static final InitializationState REINITIALIZATION_REQUIRED = new InitializationState("REINITIALIZATION_REQUIRED");

  /**
   * Connection is initialized, but the process of reinitialization takes place.
   */
  public static final InitializationState REINITIALIZING = new InitializationState("REINITIALIZING");

  private InitializationState(String name) {
    super(name);
  }

  public boolean isInitialized() {
    return this == INITIALIZED || this == REINITIALIZATION_REQUIRED || this == REINITIALIZING;
  }

  public boolean isInitializing() {
    return this == INITIALIZING || this == REINITIALIZING;
  }

  public boolean isInitializationRequired() {
    return this == NOT_INITIALIZED || this == REINITIALIZATION_REQUIRED;
  }
}
