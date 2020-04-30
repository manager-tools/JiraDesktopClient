package com.almworks.util.xmlrpc;

import java.util.Vector;

public interface IncomingMessageFactory {
  String getRpcMethodName();

  IncomingMessage createMessage(Vector parameters) throws Exception;
}
