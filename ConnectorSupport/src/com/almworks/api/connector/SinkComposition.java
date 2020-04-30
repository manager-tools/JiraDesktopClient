package com.almworks.api.connector;

import java.util.Arrays;
import java.util.Collection;

public class SinkComposition<T> implements CollectionSink<T> {
  private final Collection<CollectionSink<T>> mySinks;

  public SinkComposition(Collection<CollectionSink<T>> sinks) {
    mySinks = sinks;
  }

  public static <T> SinkComposition<T> compose(CollectionSink<T>... sinks) {
    return new SinkComposition(Arrays.asList(sinks));
  }

  @Override
  public void pushStarted() throws ConnectorException {
    for (CollectionSink<T> sink : mySinks) sink.pushStarted();
  }

  @Override
  public void pushFinished() throws ConnectorException {
    for (CollectionSink<T> sink : mySinks) sink.pushFinished();
  }

  @Override
  public void push(T element) throws ConnectorException {
    for (CollectionSink<T> sink: mySinks) sink.push(element);
  }
}
