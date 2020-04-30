package com.almworks.util.net;

public abstract class NetworkMessage {
  private MessageTransportData myTransportData;

  public MessageTransportData getTransportData() {
    return myTransportData;
  }

  public void setTransportData(MessageTransportData transportData) {
    myTransportData = transportData;
  }
}
