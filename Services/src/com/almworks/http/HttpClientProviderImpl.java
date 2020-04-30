package com.almworks.http;

import com.almworks.api.http.HttpClientProvider;
import com.almworks.api.http.HttpProxyInfo;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import org.almworks.util.Log;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.ssl.HttpsURLConnection;

public class HttpClientProviderImpl implements HttpClientProvider {
  private static final int DEFAULT_TIMEOUT = 90000;
  private static final int SOCKET_TIMEOUT = getSocketTimeout();
  private static final String IP_RESOLUTION_POSITIVE_CACHE_SECONDS = "60";
  private static final String IP_RESOLUTION_NEGATIVE_CACHE_SECONDS = "10";

  private static int getSocketTimeout() {
    int timeout = DEFAULT_TIMEOUT;
    String property = Env.getString(GlobalProperties.SOCKET_TIMEOUT);
    if (property != null) {
      try {
        timeout = Integer.parseInt(property);
      } catch (NumberFormatException e) {
        Log.warn("cannot set timeout to " + property);
      }
    }
    return timeout;
  }

  private static boolean ourProtocolsInstalled = false;
  private static boolean ourCookiePolicyInstalled = false;
  private static boolean ourLoggingInstalled = false;
  private static boolean ourNetworkParametersInstalled = false;

  private final HttpProxyInfo myHttpProxyInfo;

  public HttpClientProviderImpl(HttpProxyInfo httpProxyInfo) {
    myHttpProxyInfo = httpProxyInfo;
  }

  public HttpClient createHttpClient() {
    installLogging();
    installProtocols();
    installCookiePolicy();
    installNetworkParameters();
    MyHttpClient httpClient = new MyHttpClient();
    httpClient.watchProxy(myHttpProxyInfo);
    HttpConnectionManagerParams params = httpClient.getHttpConnectionManager().getParams();
    params.setConnectionTimeout(SOCKET_TIMEOUT);
    params.setSoTimeout(SOCKET_TIMEOUT);
    params.setStaleCheckingEnabled(false);
    return httpClient;
  }

  private void installNetworkParameters() {
    if (ourNetworkParametersInstalled)
      return;
    ourNetworkParametersInstalled = true;
    try {
      java.security.Security.setProperty("networkaddress.cache.ttl", IP_RESOLUTION_POSITIVE_CACHE_SECONDS);
      java.security.Security.setProperty("networkaddress.cache.negative.ttl", IP_RESOLUTION_NEGATIVE_CACHE_SECONDS);
    } catch (Exception e) {
      Log.warn("cannot set network properties", e);
    }
  }

  private static void installLogging() {
    if (ourLoggingInstalled)
      return;
    ourLoggingInstalled = true;
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
  }

  private static void installCookiePolicy() {
    if (ourCookiePolicyInstalled)
      return;
    ourCookiePolicyInstalled = true;
    CookiePolicy.registerCookieSpec(RFC2109Loosened.RFC2109_LOOSENED, RFC2109Loosened.class);
    DefaultHttpParams.setHttpParamsFactory(new DefaultHttpParamsFactory() {
      protected HttpParams createParams() {
        HttpParams params = super.createParams();
        params.setParameter(HttpMethodParams.COOKIE_POLICY, RFC2109Loosened.RFC2109_LOOSENED);
        return params;
      }
    });
  }

  private synchronized static void installProtocols() {
    if (ourProtocolsInstalled)
      return;
    MySSLProtocolSocketFactory sslFactory = new MySSLProtocolSocketFactory();
    Protocol myhttps = new Protocol("https", sslFactory, 443);
    Protocol.registerProtocol("https", myhttps);

    // needed for trust-all HTTPS in JRE
    HttpsURLConnection.setDefaultSSLSocketFactory(sslFactory.getContext().getSocketFactory());

    Protocol defaultHttp = Protocol.getProtocol("http");
    String scheme = defaultHttp == null ? "http" : defaultHttp.getScheme();
    int defaultPort = defaultHttp == null ? 80 : defaultHttp.getDefaultPort();

    ProtocolSocketFactory socketFactory =
      defaultHttp == null ? new DefaultProtocolSocketFactory() : defaultHttp.getSocketFactory();

    ProtocolSocketFactory httpFactory;
    if (defaultHttp == null || defaultHttp.getSocketFactory() instanceof DefaultProtocolSocketFactory) {
      httpFactory = new MyProtocolSocketFactory();
    } else {
      httpFactory = new MySocketExceptionFactoryWrapper(socketFactory); 
    }

    Protocol myhttp = new Protocol(scheme, httpFactory, defaultPort);
    Protocol.registerProtocol("http", myhttp);

    ourProtocolsInstalled = true;
  }
}
