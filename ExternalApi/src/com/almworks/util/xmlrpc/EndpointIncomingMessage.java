package com.almworks.util.xmlrpc;

import java.util.Vector;

public abstract class EndpointIncomingMessage extends IncomingMessage {
  private final MessageEndPoint myEndPoint;

  protected EndpointIncomingMessage(Vector requestParameters, MessageEndPoint endPoint) {
    myEndPoint = endPoint;
  }

  protected final MessageEndPoint getEndPoint() {
    return myEndPoint;
  }
}
