package com.almworks.util.xmlrpc;

public abstract class IncomingMessage {
  protected abstract void process() throws MessageProcessingException;
}
