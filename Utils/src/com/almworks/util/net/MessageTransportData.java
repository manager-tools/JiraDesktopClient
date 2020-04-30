package com.almworks.util.net;

import java.net.InetAddress;

public class MessageTransportData {
  /**
   * Order of message processing
   */
  private long mySequence;

  private InetAddress mySourceAddress;

  private int mySourcePort;

  private InetAddress myDestinationAddress;

  private int myDestinationPort;


  public InetAddress getSourceAddress() {
    return mySourceAddress;
  }

  public void setSourceAddress(InetAddress sourceAddress) {
    mySourceAddress = sourceAddress;
  }

  public int getSourcePort() {
    return mySourcePort;
  }

  public void setSourcePort(int sourcePort) {
    mySourcePort = sourcePort;
  }

  public long getSequence() {
    return mySequence;
  }

  public void setSequence(long sequence) {
    mySequence = sequence;
  }

  public InetAddress getDestinationAddress() {
    return myDestinationAddress;
  }

  public void setDestinationAddress(InetAddress destinationAddress) {
    myDestinationAddress = destinationAddress;
  }

  public int getDestinationPort() {
    return myDestinationPort;
  }

  public void setDestinationPort(int destinationPort) {
    myDestinationPort = destinationPort;
  }


  public String toString() {
    return "(#" + mySequence + " " + mySourceAddress + ":" + mySourcePort + ")";
  }
}
