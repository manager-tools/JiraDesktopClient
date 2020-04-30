package com.almworks.api.http;

import com.almworks.util.model.ScalarModel;
import org.almworks.util.detach.Lifespan;
import org.apache.commons.httpclient.HttpClient;

public interface HttpMaterial {
  void checkCancelled() throws HttpCancelledException;

  HttpLoader createLoader(String escapedUrl, HttpMethodFactory methodFactory);

  void dispose();

  String getCharset();

  FeedbackHandler getFeedbackHandler();

  HttpClient getHttpClient();

  long getLastServerResponseTime();

  boolean isCancelled();

  void setCancelFlag(Lifespan lifespan, ScalarModel<Boolean> cancelFlag);

  void setCharset(String charset);

  void setFeedbackHandler(FeedbackHandler feedbackHandler);

  void setLastServerResponseTime(long time);

  void setIgnoreProxy(boolean ignoreProxy);

  String getUserAgent();

  void setUserAgent(String userAgent);

  /**
   * If true, the parameter "quiet" requests that no use interaction is involved (e.g. for authorization),
   * if necessary - failing the requests.
   */
  void setQuiet(boolean quiet);

  boolean isQuiet();
}
