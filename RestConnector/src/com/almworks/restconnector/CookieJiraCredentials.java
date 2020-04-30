package com.almworks.restconnector;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraCredentialsRequiredException;
import com.almworks.jira.connector2.JiraException;
import com.almworks.restconnector.login.AuthenticationRegister;
import com.almworks.restconnector.login.JiraAccount;
import com.almworks.restconnector.operations.RestAuth1Session;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Function;
import com.almworks.util.http.WebCookieManager;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Cookie-based JIRA session.
 */
public class CookieJiraCredentials implements JiraCredentials {
  public static final long CHECK_INTERVAL = 5 * Const.MINUTE;
  /**
   * null - anonymous session, empty-string - authenticated session with unknown username, not-empty - known username
   */
  private final String myUsername;
  private volatile List<Cookie> myCookies;
  /**
   * This callback will notify when cookies are updated. This may happen due to:<br>
   * 1. Server response has Set-Cookie header. Thus session updates cookies and this callback inform about new cokies.<br>
   * 2. When new cookies provided from {@link #myReLogin}
   */
  @Nullable
  private final Consumer<CookieJiraCredentials> myUpdateCookiesCallback;
  private final boolean myCheckUsername;
  @Nullable
  private final Function<CookieJiraCredentials, List<Cookie>> myReLogin;
  @Nullable
  private final AuthenticationRegister myAuthenticationRegister;
  /**
   * Time of last successful cookie check. -1 means cookie are not checked yet.
   */
  private volatile long myLastSuccessfulCheck = -1;

  private CookieJiraCredentials(String username, List<Cookie> cookies, @Nullable Consumer<CookieJiraCredentials> updateCookiesCallback,
                                boolean checkUsername, @Nullable Function<CookieJiraCredentials, List<Cookie>> reLogin,
                                @Nullable AuthenticationRegister authenticationRegister) {
    myUsername = username;
    myCookies = Collections15.unmodifiableListCopy(cookies);
    myUpdateCookiesCallback = updateCookiesCallback;
    myCheckUsername = checkUsername;
    myReLogin = reLogin;
    myAuthenticationRegister = authenticationRegister;
  }

  /**
   * Creates credentials to access JIRA for the first time, when actual username is not know yet, thus it need not to be checked
   * @param cookies session cookies
   * @param authenticationRegister register to inform when session state is checked. Can be null when credentials are not used by configured connection
   * @return credential
   */
  public static CookieJiraCredentials establishConnection(List<Cookie> cookies, AuthenticationRegister authenticationRegister) {
    return new CookieJiraCredentials(null, cookies, null, false, null, authenticationRegister);
  }

  /**
   * Creates credentials for the known JIRA connection - when username is known (or known to be anonymous).
   * @param username connection username, null for anonymous
   * @param cookies session cookies
   * @param updateCookiesCallback this callback will be called if session cookie are updated
   * @param reLoginCallback this callback will be called if cookies expire. It should ask user for new pair [username, cookies]
   * @param authenticationRegister register to inform when session state is checked. Can be null when credentials are not used by configured connection
   * @return credential which check username of each response
   * @see #myUpdateCookiesCallback
   */
  public static CookieJiraCredentials connected(String username, List<Cookie> cookies, @Nullable Consumer<CookieJiraCredentials> updateCookiesCallback,
                                                @Nullable Function<CookieJiraCredentials, List<Cookie>> reLoginCallback, @Nullable AuthenticationRegister authenticationRegister) {
    return new CookieJiraCredentials(username, cookies, updateCookiesCallback, true, reLoginCallback, authenticationRegister);
  }

  public List<Cookie> getCookies() {
    return myCookies;
  }

  @NotNull
  @Override
  public String getUsername() {
    return Util.NN(myUsername);
  }

  @Override
  public boolean isAnonymous() {
    return myUsername == null;
  }

  @Override
  public void initNewSession(RestSession session) throws ConnectorException {
    session.setSessionCookies(myCookies);
    ensureLoggedIn(session, false);
  }

  @Override
  public void ensureLoggedIn(RestSession session, boolean fastCheck) throws ConnectorException {
    if (checkSession(session)) return; // Session is authenticated
    if (myReLogin == null) {
      LogHelper.debug("No Relogin", session.getBaseUrl());
      throw new JiraCredentialsRequiredException(); // Probably never had a good session, so report generic message
    }
    if (fastCheck) return;
    List<Cookie> update = myReLogin.invoke(this);
    if (update == null) {
      LogHelper.debug("Relogin returned nothing");
      throw new SessionExpiredException();
    }
    update = Collections15.unmodifiableListCopy(update);
    LogHelper.debug("New cookies provided", session.getBaseUrl(), myUsername);
    session.setSessionCookies(update);
    RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.FAILURE_ONLY, true);
    if (!auth.hasUsername()) {
      LogHelper.warning("Failed to check new cookies", session.getBaseUrl(), myUsername);
      session.setSessionCookies(myCookies);
      if (myAuthenticationRegister != null)
        myAuthenticationRegister.markFailed(session.getBaseUrl(), update);
      throw auth.getFailureOr(new SessionExpiredException());
    }
    if (myUsername == null || !myUsername.isEmpty()) {
      if (!Objects.equals(myUsername, auth.getUsername())) {
        LogHelper.warning("Session restored to wrong account. Session is rejected.", session.getBaseUrl(), myUsername, auth.getUsername());
        session.setSessionCookies(myCookies);
        // Do not notify myAuthenticationRegister about failure - the session may be valid, it is just connected to a wrong account
        throw auth.getFailureOr(new SessionExpiredException());
      }
    }
    LogHelper.debug("New cookies checked", session.getBaseUrl(), myUsername);
    myLastSuccessfulCheck = System.currentTimeMillis();
    myCookies = update;
    if (myUpdateCookiesCallback != null) myUpdateCookiesCallback.accept(this);
  }

  private JiraAccount getAccount(RestSession session) {
    return JiraAccount.create(myUsername, session.getBaseUrl());
  }

  @Override
  public String toString() {
    List<String> cookies = new ArrayList<>();
    for (Cookie c : myCookies) {
      cookies.add(String.format("Cookie(name='%s',exp=%s,domain=%s,path=%s", c.getName(), c.getExpiryDate(), c.getDomain(), c.getPath()));
    }
    return String.format("Cookie(user='%s', check=%s, lastCheck=%s, cookies=[%s])",
            myUsername, myCheckUsername, System.currentTimeMillis() - myLastSuccessfulCheck, TextUtil.separateToString(cookies, ", "));
  }

  /**
   * Checks response. If the response is surely comes from JIRA and right authenticated - update cookies (since they may be updated by server).
   * If response seems valid, but not sure - treats it as right, but do not update cookies.
   * @param session current session
   * @param job current job with not-null response
   * @param response server response to check
   * @return true if response seems comes from JIRA and is properly authenticated. false if session seems expired
   */
  @NotNull
  @Override
  public ResponseCheck checkResponse(RestSession session, RestSession.Job job, @NotNull RestResponse response) {
    ResponseCheck checkResult = doCheckResponse(session, job, response);
    if (checkResult.isFailed()) return checkResult;
    if (!checkResult.isSuccess()) return checkResult.assumeSuccess("Seems OK"); // Seems OK, but don't update session
    // Maybe update session cookies
    if (!job.isAuxiliary()) {
      List<Cookie> newCookies = Arrays.asList(session.getCookies());
      if (!WebCookieManager.areSame(myCookies, newCookies)) {
        myCookies = newCookies;
        myLastSuccessfulCheck = -1;
        if (myUpdateCookiesCallback != null) myUpdateCookiesCallback.accept(this);
      }
    }
    return checkResult;
  }

  /**
   * Checks that response from the server is associated with right JIRA account
   * @return false - the response is surely associated with wrong account (session has expired)<br>
   *         null - the response is associated with wrong account, but this may be OK. The response should be treated as successful, but no guarantee<br>
   *         true - the response is associated with surely right account, caller may safely update session cookies
   */
  @NotNull
  private ResponseCheck doCheckResponse(RestSession session, RestSession.Job job, @NotNull RestResponse response) {
    String username = response.getResponseHeader(RestResponse.X_AUSERNAME);
    boolean hasUsername = username != null;
    boolean recentlyChecked = myLastSuccessfulCheck > 0 && System.currentTimeMillis() - myLastSuccessfulCheck < CHECK_INTERVAL + Const.SECOND * 20;
    if (username == null && response.getStatusCode() / 100 != 2) {
      // Some not-successful responses (such as 404) may return empty USERNAME header
      // If session is recently checked assume it's OK, but do not update cookies
      if (recentlyChecked) // Return "surely" valid if the session is expected to be anonymous, return "unsure" otherwise since response was not checked well
        return myUsername == null ? ResponseCheck.success("Anonymous connection confirmed") : ResponseCheck.unsure("Missing username. Status: " + response.getStatusCode());
    }
    String wrongUsername;
    if (username == null) {
      try {
        URI lastURI = response.getHttpResponse().getLastURI();
        if (lastURI != null) {
          URI baseUri = new URI(Util.lower(session.getBaseUrl()), true, StandardCharsets.UTF_8.name());
          if (!Util.lower(lastURI.getHost()).equals(baseUri.getHost())
            || !Util.lower(lastURI.getPath()).startsWith(baseUri.getPath())) return ResponseCheck.unsure("Redirected out of Jira: " + lastURI); // The response does not come from JIRA, so we cannot check username
        }
      } catch (URIException e) {
        LogHelper.debug(e);
      }
      wrongUsername = "Response misses username";
    } else {
      wrongUsername = checkUsername(username);
    }
    if (myCheckUsername && wrongUsername != null) { // Sometimes wrong username does not mean expired session for sure
      RestSession.Request request = response.getRequest();
      String requestedUrl = Util.lower(request.getUrl());
      String responseUrl = Util.lower(response.getLastUrl());
      String baseUrl = Util.lower(session.getBaseUrl());
      boolean returnUnderBase = responseUrl.startsWith(baseUrl);
      boolean requestedUnderBase = requestedUrl.startsWith(baseUrl);
      if (requestedUnderBase && !returnUnderBase) // Resource loaded not from JIRA server
        return ResponseCheck.fail(String.format("Response does comes from Jira with wrong username: %s. Expected base: '%s', request: '%s', response: '%s'", wrongUsername, baseUrl, requestedUrl, responseUrl));
      if (response.getResponseHeader("ETag") != null) return ResponseCheck.unsure("Wrong username, but response is cacheable."); // Cacheable resources may contain wrong username
      if (!hasUsername && recentlyChecked) return ResponseCheck.unsure("Missing username but has checked the session recently"); // Response misses username, so assume the session is still valid
      return ResponseCheck.fail("Username check failed: " + wrongUsername);
    }
    return wrongUsername != null ? ResponseCheck.unsure(wrongUsername) : ResponseCheck.success("Username matches"); // Return "unsure" if username does not match
  }

  /**
   * Checks if the username equals to the expected username
   * @param username username to check
   * @return null if username is equal. Or non-null message if it is not
   */
  @Nullable
  private String checkUsername(String username) {
    if (RestResponse.ANONYMOUS_USER.equals(username)) username = null;
    if (Objects.equals(myUsername, username)) return null;
    if (username != null) // Maybe username is right, but it is URL-encoded
      try {
        String decoded = URLDecoder.decode(username, StandardCharsets.UTF_8.name());
        if (Objects.equals(myUsername, decoded)) return null;
      } catch (UnsupportedEncodingException e) {
        // ignore
      }
    return String.format("Expected username: '%s' but was: '%s'", myUsername, username);
  }

  private boolean checkSession(RestSession session) throws ConnectorException {
    long lastCheck = myLastSuccessfulCheck;
    long now = System.currentTimeMillis();
    if (lastCheck > 0 && (now - myLastSuccessfulCheck)  < CHECK_INTERVAL) return true;
    boolean complete = false;
    try {
      RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.FAILURE_ONLY, true);
      if (auth.getFailure() != null) throw auth.getFailure();
      if (!auth.hasUsername()) return false;
      String username = auth.getUsername();
      if (myCheckUsername) {
        if (RestResponse.ANONYMOUS_USER.equals(username)) username = null;
        if (!Objects.equals(myUsername, username)) {
          LogHelper.debug("CookieCredentials: login check failed due to username mismatch", myUsername, username);
          return false;
        }
      }
      complete = true;
      myLastSuccessfulCheck = now;
      if (myAuthenticationRegister != null) myAuthenticationRegister.markConfirmed(JiraAccount.create(session.getBaseUrl(), username), myCookies);
      return true;
    } finally {
      if (!complete) {
        myLastSuccessfulCheck = -1;
        if (myAuthenticationRegister != null) // Probably session is expired. Even if not, server is not accessible - no reason to reuse the session.
          myAuthenticationRegister.markFailed(session.getBaseUrl(), myCookies);
      }
    }
  }

  @Override
  public CookieJiraCredentials createUpdated(RestSession session) throws ConnectorException {
    RestAuth1Session auth = RestAuth1Session.get(session, RequestPolicy.SAFE_TO_RETRY, true);
    if (!auth.hasUsername()) throw auth.getFailureOr(new SessionExpiredException());
    String username = auth.getUsername();
    return new CookieJiraCredentials(username, myCookies, myUpdateCookiesCallback, true, myReLogin, myAuthenticationRegister);
  }

  /**
   * Special WebLogin "not-authorised" message
   */
  private static class SessionExpiredException extends JiraException {
    public SessionExpiredException() {
      super("Session expired", "Session Expired", "Cannot connect to your Jira account.\n" +
              "This likely caused by expired session.\n" +
              "Please, edit connection settings and log into your Jira server.", JiraCause.ACCESS_DENIED);
    }
  }
}
