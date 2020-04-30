package com.almworks.util.xmlrpc;

import org.apache.xmlrpc.XmlRpcClient;

import java.util.Vector;

public class InboxTests extends XmlRpcFixture {
  private static int PORT = 31003;

  private MessageInbox myInbox;
  private InboxHandler myHandler;

  protected void setUp() throws Exception {
    super.setUp();
    myInbox = new MessageInbox(PORT);
    myInbox.start();
    myHandler = new InboxHandler();
    myInbox.addFactory(myHandler.getMessageFactory());
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    myInbox.shutdown();
    myInbox = null;
    myHandler = null;
  }

  public void test() throws Exception {
    XmlRpcClient client = new XmlRpcClient("127.0.0.1", PORT);
    client.execute("almworks.test", new Vector());
    myHandler.check(500, "test");
  }
}
