package com.almworks.util.net;

import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;

public interface NetworkReceiver {
  void setSink(Procedure<Pair<byte[], MessageTransportData>> sink);

  void setBufferSize(int bufferSize);

  void start();

  void stop();
}
