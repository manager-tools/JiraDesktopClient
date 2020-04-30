package com.almworks.restconnector;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraException;
import com.almworks.restconnector.login.JiraLoginInfo;
import com.almworks.restconnector.login.LoginJiraCredentials;
import com.almworks.restconnector.operations.RestAuth1Session;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.params.HostParams;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements JiraCredential for Jira Cloud login.
 * @author dyoma
 */
public class BasicAuthCredentials implements JiraCredentials {
  private final UsernamePasswordCredentials myCredentials;
  private final String myUsername;
  private final LoginJiraCredentials.CheckRecentLogin myCheckRecentLogin = new LoginJiraCredentials.CheckRecentLogin();
  /** This flag means that once it was detected that the API token was stale. So no additional checks are required */
  private AtomicBoolean myStaleToken = new AtomicBoolean(false);

  private BasicAuthCredentials(UsernamePasswordCredentials credentials, String username) {
    myCredentials = credentials;
    myUsername = username;
  }

  /**
   * Creates credentials to access JiraCloud for the first time, when actual username is not know yet, thus it need not to be checked
   */
  public static BasicAuthCredentials establishConnection(String login, String password) {
    return new BasicAuthCredentials(new UsernamePasswordCredentials(login, password), null);
  }

  /**
   * Creates credentials for the known Jira Cloud user.
   */
  public static BasicAuthCredentials connected(String login, String password, String username) {
    return new BasicAuthCredentials(new UsernamePasswordCredentials(login, password), username);
  }

  @NotNull
  @Override
  public String getUsername() {
    return Util.NN(myUsername);
  }

  @Override
  public boolean isAnonymous() {
    return false;
  }

  @Override
  public void initNewSession(RestSession session) {
    String credStr = myCredentials.getUserName() + ":" + myCredentials.getPassword();
    String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credStr.getBytes(StandardCharsets.UTF_8));
    session.getHttpClient().getParams().setParameter(HostParams.DEFAULT_HEADERS, Collections.singleton(new Header("Authorization", basicAuth)));
  }

  @Override
  public void ensureLoggedIn(RestSession session, boolean fastCheck) throws ConnectorException {
    if (myCheckRecentLogin.isJustLoggedIn(session)) return;
    if (myStaleToken.get()) { // Do not check API token if we already know that it is stale
      LogHelper.warning("Stale API token was detected earlier");
      throw wrongApiToken();
    }
    RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.FAILURE_ONLY, true);
    if (auth.getFailure() != null) throw auth.getFailure();
    String username = Util.NN(auth.getUsername()).trim();
    if (!auth.hasUsername() || username.isEmpty())
      throw wrongApiToken();
    myCheckRecentLogin.afterLogin(session);
  }

  @NotNull
  @Override
  public ResponseCheck checkResponse(RestSession session, RestSession.Job job, @NotNull RestResponse response) {
    int statusCode = response.getStatusCode();
    if (statusCode / 100 == 2) return ResponseCheck.success("BasicAuth-based");
    if (response.getLastUrl().endsWith("/rest/auth/1/session")) {
      LogHelper.assertError(statusCode == 401, "/rest/auth/1/session: Unexpected status code:", statusCode);
      myStaleToken.set(true);
      return ResponseCheck.fail("Failed to load '/rest/auth/1/session': " + statusCode);
    }
    return ResponseCheck.unsure("Failed with code: " + statusCode);
  }

  @Override
  public JiraCredentials createUpdated(RestSession session) throws ConnectorException {
    RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.SAFE_TO_RETRY, true);
    if (!auth.hasUsername()) throw auth.getFailureOr(wrongApiToken());
    String username = auth.getUsername();
    return new BasicAuthCredentials(myCredentials, username);
  }

  public JiraLoginInfo toLoginInfo() {
    return new JiraLoginInfo(myCredentials.getUserName(), myCredentials.getPassword(), false, myUsername);
  }

  @NotNull
  private static JiraException wrongApiToken() {
    return new JiraException("Cannot access JiraCloud", "Cannot access JiraCloud", "Cannot access JiraCloud.\n" +
      "May be you need to re-configure your connection with new API token.\n" +
      "Please, edit connection and try new API token.", JiraException.JiraCause.ACCESS_DENIED);
  }
}
