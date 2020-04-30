package com.almworks.util.http;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;

public class ExtendedHttpConnectionManager extends SimpleHttpConnectionManager {
  private final InfiniteOperationGuard myExtendedConnector = new InfiniteOperationGuard();

  public void closeConnection() {
    HttpConnection connection = httpConnection;
    try {
      super.releaseConnection(connection);
    } catch (Exception e) {
      // ignore
    }
    if (connection != null) {
      connection.close();
    }
  }

  public HttpConnection getConnectionWithTimeout(HostConfiguration hostConfiguration, long timeout) {
    if (httpConnection == null) {
      httpConnection = new ExtendedHttpConnection(hostConfiguration);
      httpConnection.setHttpConnectionManager(this);
      httpConnection.getParams().setDefaults(getParams());
    }
    releaseConnection(httpConnection);
    return super.getConnectionWithTimeout(hostConfiguration, timeout);
  }

  public InfiniteOperationGuard getConnector() {
    return myExtendedConnector;
  }


  public void closeIdleConnections(long idleTimeout) {
    if (httpConnection != null)
      super.closeIdleConnections(idleTimeout);
  }
}
