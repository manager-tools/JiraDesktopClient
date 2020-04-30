package com.almworks.http;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class is a hack workaround for java problems:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5069130
 */
class MySocketExceptionFactoryWrapper implements ProtocolSocketFactory {
  @NotNull
  private final ProtocolSocketFactory myDelegate;

  public MySocketExceptionFactoryWrapper(@NotNull ProtocolSocketFactory delegate) {
    myDelegate = delegate;
  }

  public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort)
    throws IOException
  {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return myDelegate.createSocket(host, port, localAddress, localPort);
      }
    }.create();
  }

  public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
    final HttpConnectionParams params) throws IOException
  {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return myDelegate.createSocket(host, port, localAddress, localPort, params);
      }
    }.create();
  }

  public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return myDelegate.createSocket(host, port);
      }
    }.create();
  }
}
