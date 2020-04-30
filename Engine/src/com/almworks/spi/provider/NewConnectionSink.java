package com.almworks.spi.provider;

import com.almworks.api.engine.Connection;

public interface NewConnectionSink {
  void connectionCreated(Connection connection);
  void showMessage(String message);
  void initializationComplete();
}
