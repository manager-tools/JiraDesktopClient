package com.almworks.util.net;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class NetworkMessageSpecification<M extends NetworkMessage> {
  private final Class<M> myMessageClass;
  private final int myMaximumLength;

  protected NetworkMessageSpecification(@NotNull Class<M> messageClass, int maximumLength) {
    assert maximumLength > 0;
    myMaximumLength = maximumLength;
    myMessageClass = messageClass;
  }

  public int getMaximumMessageLength() {
    return myMaximumLength;
  }

  @NotNull
  public Class<M> getMessageClass() {
    return myMessageClass;
  }

  /**
   * @throws IOException when message could not be serialized
   */
  @NotNull
  public abstract byte[] marshall(M message) throws IOException;

  /**
   * @throws IOException when message could not be deserialized (wrong message, etc)
   */
  @NotNull
  public abstract M unmarshall(byte[] message, MessageTransportData transportData) throws IOException;
}
