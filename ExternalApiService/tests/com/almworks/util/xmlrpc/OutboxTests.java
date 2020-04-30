package com.almworks.util.xmlrpc;

import org.apache.xmlrpc.WebServer;

public class OutboxTests extends XmlRpcFixture {
  private static int PORT = 31008;

  private WebServer myServer;
  private OutboxHandler myHandler;
  private MessageOutbox myOutbox;

  protected void setUp() throws Exception {
    super.setUp();
    myServer = new WebServer(PORT);
    myServer.start();
    myHandler = new OutboxHandler();
    myServer.addHandler(XmlRpcUtils.SERVICE_NAME, myHandler);
    myOutbox = null;
  }

  protected void tearDown() throws Exception {
    if (myOutbox != null) {
      myOutbox.shutdown();
      myOutbox = null;
    }
    myServer.shutdown();
    myServer = null;
    myHandler = null;
    super.tearDown();
  }

  public void testPlain() throws InterruptedException {
    myOutbox = new MessageOutbox(PORT);
    myOutbox.enqueue(new SimpleOutgoingMessage("xxx", "yyy"));
    myHandler.check(2000, null, null);
    myOutbox.start();
    myHandler.check(2000, "almworks.xxx", "yyy");
  }

  public void testInterruptedThreadRecovery() throws InterruptedException {
    myOutbox = new MessageOutbox(PORT);
    myOutbox.start();
    myOutbox.enqueue(new SimpleOutgoingMessage("xxx"));
    myHandler.check(500, "almworks.xxx");
    myOutbox.getThread().interrupt();
    myOutbox.enqueue(new SimpleOutgoingMessage("yyy"));
    myHandler.check(500, "almworks.yyy");
  }
}
