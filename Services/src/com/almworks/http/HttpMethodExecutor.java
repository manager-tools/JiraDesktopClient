package com.almworks.http;

import com.almworks.api.http.HttpReportAcceptor;
import org.almworks.util.Util;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

abstract class HttpMethodExecutor {
  protected final HttpMethodBase myMethod;
  protected final HttpClient myClient;

  protected HttpMethodExecutor(HttpMethodBase method, HttpClient client) {
    assert method != null;
    myMethod = method;
    myClient = client;
  }

  public HttpMethodBase getMethod() {
    return myMethod;
  }

  public abstract void execute() throws IOException, RuntimeException;

  @Nullable
  public abstract Header getResponseHeader(String name);

  protected abstract int getMethodStatusCode();

  public abstract String getStatusText();

  public abstract AuthState getProxyAuthState();

  public abstract AuthState getHostAuthState();

  public abstract void reportTo(HttpReportAcceptor acceptor) throws Exception;

  @Nullable
  public abstract InputStream getResponseBodyAsStream() throws IOException;

  public abstract void dispose();

  public abstract String getResponseCharSet();

  public abstract Header[] getResponseHeaders();

  public abstract long getResponseContentLength();

  public final int getStatusCode() {
    // some servers override initial status code line with Status: header
    Header header = getResponseHeader("Status");
    if (header != null) {
      String value = header.getValue();
      if (value != null) {
        int k = value.indexOf(' ');
        if (k == 3) {
          int code = Util.toInt(value.substring(0, 3), -1);
          if (code >= 100 && code < 1000) {
            return code;
          }
        }
      }
    }
    return getMethodStatusCode();
  }

  public URI getURI() throws URIException {
    return myMethod.getURI();
  }
}
