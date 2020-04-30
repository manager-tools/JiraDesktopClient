package com.almworks.util.xmlrpc;

public interface MessageEndPoint {
  MessageOutbox getOutbox();

  void addIncomingMessageClass(Class<? extends EndpointIncomingMessage> clazz);

  void addIncomingMessageClasses(Class<? extends EndpointIncomingMessage>[] classes);

  void addIncomingMessageFactory(IncomingMessageFactory factory);
}
