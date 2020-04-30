package com.almworks.api.http;

import com.almworks.api.http.auth.HttpAuthChallengeData;
import com.almworks.api.http.auth.HttpAuthCredentials;
import com.almworks.util.Pair;
import com.almworks.util.threads.CanBlock;
import org.jetbrains.annotations.Nullable;

public interface FeedbackHandler {
  /**
   * This method is called when credentials are required for method to succeed (server replied with 401 unauthorized)
   *
   * @param quiet if true, do not ask user interactively
   */
  @CanBlock
  @Nullable("No credentials provided")
  HttpAuthCredentials requestCredentials(HttpAuthChallengeData data, HttpAuthCredentials failedCredentials,
    boolean quiet) throws InterruptedException, HttpCancelledException;

  /**
   * This method is called by HttpLoader to ask for credentials before request is sent, so preliminary credentials
   * may be supplied to the server
   *
   * @return null if preliminary credentials not available, or pair of credentials and scheme
   */
  @CanBlock
  Pair<HttpAuthCredentials, String> requestPreliminaryCredentials(String host, int port, boolean proxy);
}
