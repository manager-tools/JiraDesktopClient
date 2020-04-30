package com.almworks.restconnector;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraException;
import com.almworks.restconnector.login.JiraLoginInfo;
import com.almworks.restconnector.operations.LoadUserInfo;
import com.almworks.restconnector.operations.RestAuth1Session;
import com.almworks.util.LogHelper;
import org.almworks.util.Const;
import org.almworks.util.TypedKey;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.params.HostParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  private final String myAccountId;
  private final String myDisplayName;
  private final CheckRecentLogin myCheckRecentLogin = new CheckRecentLogin();
  /** This flag means that once it was detected that the API token was stale. So no additional checks are required */
  private AtomicBoolean myStaleToken = new AtomicBoolean(false);

  private BasicAuthCredentials(UsernamePasswordCredentials credentials, String accountId, String displayName) {
    myCredentials = credentials;
    myAccountId = accountId;
    myDisplayName = displayName;
  }

  /**
   * Creates credentials to access JiraCloud for the first time, when actual username is not know yet, thus it need not to be checked
   */
  public static BasicAuthCredentials establishConnection(String login, String password) {
    return new BasicAuthCredentials(new UsernamePasswordCredentials(login, password), null, null);
  }

  /**
   * Creates credentials for the known Jira Cloud user.
   */
  public static BasicAuthCredentials connected(String login, String password, String accountId, String displayName) {
    return new BasicAuthCredentials(new UsernamePasswordCredentials(login, password), accountId, displayName);
  }

  @Nullable("When anonymous")
  @Override
  public String getDisplayName() {
    return myDisplayName;
  }

  @Override
  public String getAccountId() {
    return myAccountId;
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
    LoadUserInfo userInfo = auth.getUserInfo();
    if (!auth.hasUsername() || userInfo == null)
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
    LoadUserInfo userInfo = auth.getUserInfo();
    if (!auth.hasUsername() || userInfo == null) throw auth.getFailureOr(wrongApiToken());
    return new BasicAuthCredentials(myCredentials, userInfo.getAccountId(), userInfo.getDisplayName());
  }

  public JiraLoginInfo toLoginInfo() {
    return new JiraLoginInfo(myCredentials.getUserName(), myCredentials.getPassword(), false, myAccountId, myDisplayName);
  }

  @NotNull
  private static JiraException wrongApiToken() {
    return new JiraException("Cannot access JiraCloud", "Cannot access JiraCloud", "Cannot access JiraCloud.\n" +
      "May be you need to re-configure your connection with new API token.\n" +
      "Please, edit connection and try new API token.", JiraException.JiraCause.ACCESS_DENIED);
  }

  /**
   * This convenient class tracks login state checks to reduce number of expensive requests.
   */
  public static class CheckRecentLogin {
    private static final long CHECK_TIME = 4* Const.MINUTE;
    private final TypedKey<Long> myLastCheckKey = TypedKey.create("lastCheckTime");

    /**
     * A client should notify when login state has been check and confirmed.
     */
    public void afterLogin(RestSession session) {
      session.getUserData().putUserData(myLastCheckKey, System.currentTimeMillis());
    }

    /**
     * A client notifies that current login state is unknown
     */
    public void resetLogin(RestSession session) {
      session.getUserData().putUserData(myLastCheckKey, null);
    }

    /**
     * @return true if authentication resources were recently successfully accessed (session auth state logged in or anonymous) was confirmed.
     */
    public boolean isJustLoggedIn(RestSession session) {
      Long time = session.getUserData().getUserData(myLastCheckKey);
      long now = System.currentTimeMillis();
      return time != null && now - time < CHECK_TIME;
    }
  }
}
