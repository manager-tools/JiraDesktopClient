package com.almworks.api.connector;

public interface CollectionSink<T> {
  void pushStarted() throws ConnectorException;

  void pushFinished() throws ConnectorException;

  void push(T element) throws ConnectorException;
}
