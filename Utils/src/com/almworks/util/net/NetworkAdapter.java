package com.almworks.util.net;

import com.almworks.util.events.EventSource;

public interface NetworkAdapter {
  void defineMessage(NetworkMessageSpecification specification);

  EventSource<NetworkAdapterListener> events();

  void send(NetworkMessage message);

  void start();

  void stop();
}
