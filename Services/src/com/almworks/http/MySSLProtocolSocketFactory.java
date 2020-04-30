package com.almworks.http;

import org.almworks.util.Failure;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

class MySSLProtocolSocketFactory implements ProtocolSocketFactory, SecureProtocolSocketFactory {
  private SSLContext myContext = null;

  public synchronized SSLContext getContext() {
    if (myContext != null)
      return myContext;
    try {
      myContext = SSLContext.getInstance("SSL");
      myContext.init(null, new TrustManager[] {new MyX509TrustManager()}, new SecureRandom());
    } catch (NoSuchAlgorithmException e) {
      throw new Failure(e);
    } catch (KeyManagementException e) {
      throw new Failure(e);
    }
    return myContext;
  }

  private SSLSocketFactory delegate() {
    return getContext().getSocketFactory();
  }

  public Socket createSocket(final String host, final int port, final InetAddress clientHost, final int clientPort)
    throws IOException
  {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return delegate().createSocket(host, port, clientHost, clientPort);
      }
    }.create();
  }

  public Socket createSocket(final String host, final int port) throws IOException {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return delegate().createSocket(host, port);
      }
    }.create();
  }

  public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose)
    throws IOException
  {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return delegate().createSocket(socket, host, port, autoClose);
      }
    }.create();
  }

  public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
    HttpConnectionParams params) throws IOException
  {
    return new CreateSocketMethod(host, port) {
      protected Socket createUnsafe() throws IOException {
        return delegate().createSocket(host, port, localAddress, localPort);
      }
    }.create();
  }

  private static class MyX509TrustManager implements X509TrustManager {
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[] {};
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    }
  }
}
