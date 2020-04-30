package com.almworks.jira.provider3.app.connection;

import com.almworks.api.http.FeedbackHandler;
import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpUtils;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.jira.provider3.app.connection.setup.weblogin.ReLogin;
import com.almworks.jira.provider3.app.connection.setup.weblogin.WebLoginConfig;
import com.almworks.restconnector.BasicAuthCredentials;
import com.almworks.restconnector.CookieJiraCredentials;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.login.JiraLoginInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.properties.Role;
import org.almworks.util.Util;
import org.apache.commons.httpclient.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ConnectionDescriptor {
  public static final Role<ConnectionDescriptor> ROLE = Role.role(ConnectionDescriptor.class);

  private final String myNormalizedUrl;
  private final UserDataHolder mySessionData = new UserDataHolder();
  @NotNull
  private final JiraCredentials myCredentials;
  private final Supplier<HttpMaterial> myMaterialSupplier;
  private final SNIErrorHandler mySNIErrorHandler;

  private ConnectionDescriptor(String normalizedUrl, @NotNull JiraCredentials credentials, Supplier<HttpMaterial> materialSupplier, SNIErrorHandler sniErrorHandler) {
    myNormalizedUrl = normalizedUrl;
    myCredentials = credentials;
    myMaterialSupplier = materialSupplier;
    mySNIErrorHandler = sniErrorHandler;
  }

  public static ConnectionDescriptor create(ReadonlyConfiguration config, JiraConfigHolder master) throws ConfigurationException {
    final String baseURL = JiraConfiguration.getBaseUrl(config);
    if (baseURL == null || baseURL.trim().length() == 0)
      throw new ConfigurationException("empty URL");
    String normalizedUrl;
    try {
      normalizedUrl = HttpUtils.normalizeBaseUrl(baseURL);
    } catch (MalformedURLException e) {
      throw new ConfigurationException("bad URL [" + baseURL + "]", e);
    }
    JiraCredentials credentials;
    WebLoginConfig webLogin = JiraConfiguration.getWebLogin(config);
    JiraLoginInfo loginInfo = JiraConfiguration.getLoginInfo(config);
    if (JiraConfiguration.isWebLogin(config) && webLogin != null) {
      String accountId = JiraConfiguration.getAccountId(config);
      credentials = CookieJiraCredentials.connected(accountId, webLogin.getCookies().getAllCookies(),
              cred -> updateConfig(master, cred), argument -> maybeWebReLogin(master, config), master.getAuthenticationRegister(), loginInfo.getDisplayName());
    } else if (JiraConfiguration.isBasicAuth(config)) {
      if (loginInfo.isAnonymous()) {
        LogHelper.error("Expected non-anonymous: ", loginInfo);
        credentials = JiraCredentials.ANONYMOUS;
      } else
        credentials = BasicAuthCredentials.connected(loginInfo.getLogin(), loginInfo.getPassword(), loginInfo.getAccountId(), loginInfo.getDisplayName());
    } else  {
      assert loginInfo.isAnonymous();
      credentials = JiraCredentials.ANONYMOUS;
    }

    HttpMaterialFactory materialFactory = master.getMaterialFactory();
    boolean ignoreProxy = JiraConfiguration.isIgnoreProxy(config);
    FeedbackHandler handler = master.getFeedbackHandler();
    return new ConnectionDescriptor(normalizedUrl, credentials, () -> materialFactory.create(handler, ignoreProxy,
            JiraProvider3.getUserAgent()), master.getConnection().getActor(SSLProblemHandler.ROLE).getSNIErrorHandler());
  }

  private static List<Cookie> maybeWebReLogin(JiraConfigHolder master, ReadonlyConfiguration config) {
    return new ReLogin(master, config).doReLogin();
  }

  private static void updateConfig(JiraConfigHolder master, CookieJiraCredentials credentials) {
    if (credentials == null) return;
    master.updateConfiguration(config -> {
      WebLoginConfig webLogin = JiraConfiguration.getWebLogin(config);
      if (webLogin == null) {
        LogHelper.warning("Missing webLogin config");
        return false;
      }
      String currentAccountId = JiraConfiguration.getAccountId(config);
      if (!Objects.equals(Util.NN(currentAccountId), credentials.getAccountId())) {
        LogHelper.warning("Config not updated, accounts do not match", currentAccountId, master.getConnection());
        return false;
      }
      if (!webLogin.updateCookies(credentials.getCookies())) return false;
      JiraConfiguration.setWebLogin(config, webLogin);
      return true;
    });
  }

  @Nullable
  public String getBaseUrl() {
    return myNormalizedUrl;
  }

  public RestSession createSession() {
    if (myNormalizedUrl == null) return null;
    HttpMaterial material = createHttpMaterial();
    JiraCredentials credentials = getCredentials();
    return RestSession.create(myNormalizedUrl, credentials, material, mySessionData, mySNIErrorHandler);
  }

  @NotNull
  public JiraCredentials getCredentials() {
    return myCredentials;
  }

  @NotNull
  public HttpMaterial createHttpMaterial() {
    return myMaterialSupplier.get();
  }
}
