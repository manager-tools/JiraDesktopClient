package com.almworks.http;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class is a hack workaround for java problems:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5069130
 *
 */
class MyProtocolSocketFactory extends DefaultProtocolSocketFactory {
  public MyProtocolSocketFactory() {
  }

  public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort)
    throws IOException
  {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return MyProtocolSocketFactory.super.createSocket(host, port, localAddress, localPort);
      }
    }.create();
  }

  public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
    final HttpConnectionParams params) throws IOException
  {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return MyProtocolSocketFactory.super.createSocket(host, port, localAddress, localPort, params);
      }
    }.create();
  }

  public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return MyProtocolSocketFactory.super.createSocket(host, port);
      }
    }.create();
  }


  /**
   * WTF! Have to redefine the parent's lame equals / hashCode, otherwise keep alive does not work (different config)
   */
  public boolean equals(Object obj) {
    return ((obj != null) && obj.getClass().equals(MyProtocolSocketFactory.class));
  }

  public int hashCode() {
    return MyProtocolSocketFactory.class.hashCode();
  }
}
