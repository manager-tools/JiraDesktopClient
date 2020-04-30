package com.almworks.jira.provider3.app.connection.setup;

class ConnectionTestProgress {
  public static final ConnectionTestProgress GENERIC_FAILURE =
    new ConnectionTestProgress(State.FAILURE, "Connection test failed");

  public static final ConnectionTestProgress TESTING_CONNECTION =
    new ConnectionTestProgress(State.WORKING, "Testing connection");
  public static final ConnectionTestProgress CONNECTION_OK_LOADING_PROJECTS =
    new ConnectionTestProgress(State.WORKING, "Connection established, loading project list");
  public static final ConnectionTestProgress TEST_SUCCESSFUL =
    new ConnectionTestProgress(State.SUCCESS, "Connection test successful");
  public static final ConnectionTestProgress PROJECTS_FAILED =
    new ConnectionTestProgress(State.FAILURE, "Could not load projects");

  private final String myMessage;
  private final State myState;

  public ConnectionTestProgress(State state, String message) {
    myMessage = message;
    myState = state;
  }

  public String getMessage() {
    return myMessage;
  }

  public State getState() {
    return myState;
  }

  public String toString() {
    return myMessage;
  }

  public static enum State {
    WORKING,
    SUCCESS,
    FAILURE
  }
}
