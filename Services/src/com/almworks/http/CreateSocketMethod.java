package com.almworks.http;

import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;

abstract class CreateSocketMethod {
  private final String myHost;
  private final int myPort;

  protected CreateSocketMethod(String host, int port) {
    myHost = host;
    myPort = port;
  }

  @NotNull
  public Socket create() throws IOException {
    try {
      Socket result = createUnsafe();
      if (result == null)
        throw new IOException("cannot create socket");
      return result;
    } catch (IOException e) {
      throw e;
    } catch (RuntimeException e) {
      throw handleRuntimeException(myHost, myPort, e);
    }
  }

  protected abstract Socket createUnsafe() throws IOException;

  static RuntimeException handleRuntimeException(String host, int port, RuntimeException e) throws IOException {
    if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
      Log.debug("wrapping exception for " + host + ":" + port, e);
      String message = e.getMessage();
      throw new IOException(message == null || message.length() == 0 ? "cannot create connection to " + host + ":" + port : message);
    } else {
      return e;
    }
  }
}
