package com.almworks.http;

import com.almworks.api.http.HttpProxyInfo;
import com.almworks.api.http.HttpUtils;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.http.ExtendedHttpConnectionManager;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;

import java.lang.ref.WeakReference;

public class MyHttpClient extends HttpClient {
  private final Lifecycle myProxyLife = new Lifecycle();

  private HttpProxyInfo myProxyInfo;

  private String myProxyHost = null;
  private int myProxyPort = -1;
  private String myProxyUser = null;
  private String myProxyPassword = null;
  private boolean myUseProxy = false;
  private boolean myUseProxyAuth = false;

  public MyHttpClient() {
    super(new ExtendedHttpConnectionManager());
    myProxyInfo = HttpProxyInfo.NO_PROXY;
    refreshProxyInfo();
  }

  public void watchProxy(HttpProxyInfo proxyInfo) {
    myProxyLife.cycle();
    myProxyInfo = proxyInfo == null ? HttpProxyInfo.NO_PROXY : proxyInfo;
    refreshProxyInfo();

    Lifespan life = myProxyLife.lifespan();
    ThreadGate gate = ThreadGate.LONG(MyHttpClient.class);
    myProxyInfo.getModifiable().addChangeListener(life, gate, new MyChangeListener(this));
  }


  private synchronized void refreshProxyInfo() {
    boolean useProxy = myProxyInfo.isUsingProxy();
    boolean useProxyAuth = myProxyInfo.isAuthenticatedProxy();
    String newHost = useProxy ? Util.NN(myProxyInfo.getProxyHost()) : null;
    int newPort = useProxy ? myProxyInfo.getProxyPort() : -1;
    String newUser = useProxy ? myProxyInfo.getProxyUser() : null;
    String newPassword = useProxy ? myProxyInfo.getProxyPassword() : null;

    if (!Util.equals(newHost, myProxyHost) || newPort != myProxyPort || !Util.equals(newUser, myProxyUser)
      || !Util.equals(Util.NN(newPassword), Util.NN(myProxyPassword)) || myUseProxy != useProxy ||
      myUseProxyAuth != useProxyAuth)
    {
      myProxyHost = newHost;
      myProxyPort = newPort;
      myProxyUser = newUser;
      myProxyPassword = newPassword;
      myUseProxy = useProxy;
      myUseProxyAuth = useProxyAuth;
      HostConfiguration config = getHostConfiguration();
      if (config == null)
        config = new HostConfiguration();
      if (useProxy) {
        config.setProxy(myProxyHost, myProxyPort);
        if (myUseProxyAuth && myProxyUser != null) {
          Pair<String, String> ntpair = HttpUtils.getNTDomainUsername(Util.NN(myProxyUser));
          UsernamePasswordCredentials creds;
          if (Util.NN(ntpair.getFirst()).length() > 0) {
            creds = new NTCredentials(ntpair.getSecond(), Util.NN(myProxyPassword), HttpUtils.getLocalHostname(), ntpair.getFirst());
          } else {
            creds = new UsernamePasswordCredentials(myProxyUser, Util.NN(myProxyPassword));
          }
          getState().setProxyCredentials(AuthScope.ANY, creds);
        }
      } else {
        config.setProxyHost(null);
      }
      setHostConfiguration(config);
      refreshConnections();
    }
  }

  private synchronized void refreshConnections() {
    HttpConnectionManager manager = getHttpConnectionManager();
    if (manager instanceof ExtendedHttpConnectionManager) {
      try {
        ((ExtendedHttpConnectionManager) manager).closeConnection();
      } catch (Exception e) {
        Log.debug(e);
      }
    }
  }

  public void dispose() {
    myProxyLife.dispose();
  }

  private static class MyChangeListener implements ChangeListener {
    private final WeakReference<MyHttpClient> myReference;

    public MyChangeListener(MyHttpClient httpClient) {
      myReference = new WeakReference<MyHttpClient>(httpClient);
    }

    public void onChange() {
      MyHttpClient client = myReference.get();
      if (client != null) {
        client.refreshProxyInfo();
      }
    }
  }
}
