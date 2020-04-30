package com.almworks.api.http;

import com.almworks.http.MyHttpClient;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.http.ExtendedHttpConnectionManager;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;

public class DefaultHttpMaterial implements HttpMaterial {
  private volatile HttpClient myHttpClient = null;
  private FeedbackHandler myFeedbackHandler = null;
  private ScalarModel<Boolean> myCancelFlag;
  private final Lifecycle myCancelFlagDetach = new Lifecycle();
  private final HttpClientProvider myHttpClientProvider;
  private String myCharset = null;
  private long myLastServerResponseTime = 0;
  private final HttpLoaderFactory myLoaderFactory;
  private boolean myIgnoreProxy = false;
  private String myUserAgent;
  private boolean myQuiet;

  public DefaultHttpMaterial(HttpClientProvider httpClientProvider, HttpLoaderFactory loaderFactory) {
    assert httpClientProvider != null;
    assert loaderFactory != null;
    myHttpClientProvider = httpClientProvider;
    myLoaderFactory = loaderFactory;
  }

  public FeedbackHandler getFeedbackHandler() {
    return myFeedbackHandler;
  }

  public void setFeedbackHandler(FeedbackHandler feedbackHandler) {
    myFeedbackHandler = feedbackHandler;
  }

  public HttpClient getHttpClient() {
    if (myHttpClient == null) {
      myHttpClient = myHttpClientProvider.createHttpClient();
      if (myIgnoreProxy) {
        myHttpClient.getHostConfiguration().setProxyHost(null);
      }
//      JCO-757      
//      myHttpClient.getState().setCookiePolicy(org.apache.commons.httpclient.cookie.CookiePolicy.COMPATIBILITY);
    }
    return myHttpClient;
  }

  public void dispose() {
    try {
      closeConnection();
    } catch (Exception e) {
      // whatever
    }

    HttpClient httpClient = myHttpClient;
    if (httpClient != null) {
      httpClient.getHttpConnectionManager().closeIdleConnections(1);
      if (httpClient instanceof MyHttpClient) {
        ((MyHttpClient) httpClient).dispose();
      }
      myHttpClient = null;
    }

    myCancelFlagDetach.dispose();
  }

  public void setCancelFlag(Lifespan lifespan, ScalarModel<Boolean> cancelFlag) {
    myCancelFlagDetach.cycle();
    myCancelFlag = cancelFlag;
    if (cancelFlag != null) {
      ScalarModel.Adapter<Boolean> listener = new ScalarModel.Adapter<Boolean>() {
        public void onScalarChanged(ScalarModelEvent<Boolean> event) {
          Boolean b = event.getNewValue();
          if (b != null && b)
            closeConnection();
        }
      };
      myCancelFlag.getEventSource().addListener(myCancelFlagDetach.lifespan(), ThreadGate.LONG(this), listener);
      lifespan.add(myCancelFlagDetach.getCurrentCycleDetach());
    }
  }

  public ScalarModel<Boolean> getCancelFlag() {
    return myCancelFlag;
  }

  public void closeConnection() {
    HttpClient httpClient = myHttpClient;
    if (httpClient == null) {
      return;
    }
    HttpConnectionManager manager = httpClient.getHttpConnectionManager();
    if (manager instanceof ExtendedHttpConnectionManager)
      ((ExtendedHttpConnectionManager) manager).closeConnection();
  }

  public void checkCancelled() throws HttpCancelledException {
    if (isCancelled())
      throw new HttpCancelledException();
  }

  public boolean isCancelled() {
    ScalarModel<Boolean> flag = getCancelFlag();
    if (flag == null)
      return false;
    Boolean b = flag.getValue();
    return b != null && b.booleanValue();
  }

  public String getCharset() {
    return myCharset;
  }

  public void setCharset(String charset) {
    myCharset = charset;
  }

  public void setLastServerResponseTime(long time) {
    myLastServerResponseTime = time;
  }

  public void setIgnoreProxy(boolean ignoreProxy) {
    myIgnoreProxy = ignoreProxy;
  }

  public String getUserAgent() {
    return myUserAgent;
  }

  public void setUserAgent(String userAgent) {
    myUserAgent = userAgent;
  }

  public void setQuiet(boolean quiet) {
    myQuiet = quiet;
  }

  public boolean isQuiet() {
    return myQuiet;
  }

  public long getLastServerResponseTime() {
    return myLastServerResponseTime;
  }

  public HttpLoader createLoader(String escapedUrl, HttpMethodFactory methodFactory) {
    return myLoaderFactory.createLoader(this, methodFactory, escapedUrl);
  }
}
