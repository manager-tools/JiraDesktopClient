package com.almworks.util.xmlrpc;

public class EndPoint implements MessageEndPoint {
  private final Object[] FACTORY_ADDITIONAL_PARAMETERS = new Object[] {this};

  private final MessageInbox myInbox;
  private final MessageOutbox myOutbox;

  public EndPoint(int listenPort, int connectPort) {
    this();
    myInbox.setPort(listenPort);
    myOutbox.setPort(connectPort);
  }

  public EndPoint() {
    myInbox = new MessageInbox();
    myOutbox = new MessageOutbox();
  }

  public MessageOutbox getOutbox() {
    return myOutbox;
  }

  public void addIncomingMessageClass(Class<? extends EndpointIncomingMessage> clazz) {
    myInbox.addFactory(new IntrospectionFactory(clazz, FACTORY_ADDITIONAL_PARAMETERS));
  }

  public void addIncomingMessageClasses(Class<? extends EndpointIncomingMessage>[] classes) {
    for (Class<? extends EndpointIncomingMessage> clazz : classes) {
      addIncomingMessageClass(clazz);
    }
  }

  public void addIncomingMessageFactory(IncomingMessageFactory factory) {
    myInbox.addFactory(factory);
  }

  public void start() {
    myOutbox.start();
    myInbox.start();
  }

  public void shutdown() throws InterruptedException {
    myInbox.shutdown();
    myOutbox.shutdown();
  }

  public int getInboxPort() {
    return myInbox.getActivePort();
  }

  public void setOutboxPort(int port) {
    myOutbox.clear();
    myOutbox.setPort(port);
  }

  public void changeInboxPort() {
    int activePort = myInbox.getActivePort();
    if (activePort != 0) {
      int newPort = activePort > 65000 ? 1999 : activePort + 1;
      myInbox.setPort(newPort);
    }
  }
}
