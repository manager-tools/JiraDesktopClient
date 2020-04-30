package com.almworks.http;

import com.almworks.api.http.HttpReportAcceptor;
import com.almworks.api.http.HttpUtils;
import org.almworks.util.Log;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthState;

import java.io.IOException;
import java.io.InputStream;

public class ApacheHttpMethodExecutor extends HttpMethodExecutor {
  public ApacheHttpMethodExecutor(HttpMethodBase method, HttpClient client) {
    super(method, client);
  }

  public void execute() throws IOException, RuntimeException {
    myClient.executeMethod(myMethod);
  }

  public Header getResponseHeader(String name) {
    return myMethod.getResponseHeader(name);
  }

  protected int getMethodStatusCode() {
    if (myMethod.getStatusLine() == null) {
      Log.warn(this + ": http response with no status line, assuming code 500");
      return 500;
    }
    return myMethod.getStatusCode();
  }

  public String getStatusText() {
    if (myMethod.getStatusLine() == null) {
      Log.warn(this + ": http response with no status line, assuming error bad reply");
      return "bad reply: no status line";
    }
    return myMethod.getStatusText();
  }

  public AuthState getProxyAuthState() {
    return myMethod.getProxyAuthState();
  }

  public AuthState getHostAuthState() {
    return myMethod.getHostAuthState();
  }

  public void reportTo(HttpReportAcceptor acceptor) throws Exception {
    assert acceptor != null;
    if (myMethod.isRequestSent()) {
      String name = myMethod.getName();
      URI uri = myMethod.getURI();
      HttpVersion version = myMethod.getEffectiveVersion();
      Header[] headers = myMethod.getRequestHeaders();
      StatusLine statusLine = myMethod.getStatusLine();
      Header[] responseHeaders = myMethod.getResponseHeaders();
      acceptor.report(name, uri, version, headers, statusLine, responseHeaders);
    }
  }

  public InputStream getResponseBodyAsStream() throws IOException {
    return myMethod.getResponseBodyAsStream();
  }

  public void dispose() {
    myMethod.releaseConnection();
  }

  public String getResponseCharSet() {
    return myMethod.getResponseCharSet();
  }

  public Header[] getResponseHeaders() {
    return myMethod.getResponseHeaders();
  }

  public long getResponseContentLength() {
    return myMethod.getResponseContentLength();
  }

  public String toString() {
    return "AHME[" + HttpUtils.toString(myMethod) + "]";
  }
}
