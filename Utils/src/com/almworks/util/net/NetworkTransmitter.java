package com.almworks.util.net;

public interface NetworkTransmitter {
  void send(byte[] bytes) throws InterruptedException;
}
