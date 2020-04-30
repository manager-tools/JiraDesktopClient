package com.almworks.util.xmlrpc;

import java.util.Vector;

public abstract class CloningIncomingMessage extends IncomingMessage implements IncomingMessageFactory, Cloneable {
  private final String myMethodName;
  private Vector myParameters;

  protected CloningIncomingMessage(String methodName) {
    myMethodName = methodName;
  }

  public final String getRpcMethodName() {
    return myMethodName;
  }

  public IncomingMessage createMessage(Vector parameters) throws Exception {
    CloningIncomingMessage message = (CloningIncomingMessage) clone();
    message.reset(parameters);
    return message;
  }

  protected Vector getParameters() {
    assert myParameters != null;
    return myParameters;
  }

  protected void reset(Vector parameters) {
    myParameters = parameters;
  }
}
