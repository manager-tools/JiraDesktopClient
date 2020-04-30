package com.almworks.api.engine;

/**
 * @author Vasya
 */
public interface ConnectionChangeListener {
  public void onChange(Connection connection, ConnectionState oldState, ConnectionState newState);
}
