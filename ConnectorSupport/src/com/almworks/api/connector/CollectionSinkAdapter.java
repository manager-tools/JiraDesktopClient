package com.almworks.api.connector;

public class CollectionSinkAdapter<T> implements CollectionSink<T> {
  public void push(T element) throws ConnectorException {
  }

  public void pushFinished() throws ConnectorException {
  }

  public void pushStarted() throws ConnectorException {
  }
}
