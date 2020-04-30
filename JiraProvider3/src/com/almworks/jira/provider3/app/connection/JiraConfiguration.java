package com.almworks.jira.provider3.app.connection;

import com.almworks.api.engine.CommonConfigurationConstants;
import com.almworks.api.http.HttpUtils;
import com.almworks.jira.provider3.app.connection.setup.weblogin.WebLoginConfig;
import com.almworks.restconnector.login.JiraLoginInfo;
import com.almworks.spi.provider.util.PasswordUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Provides accessors to JIRA connection configuration.
 * Connection may be configured in two modes: WebLogin and Credentials. These modes designates how JIRAClient authenticates.
 * Connection configuration may hold settings for both modes, however only one mode is active.
 */
public class JiraConfiguration {
  /** Type: string. Server base URL. */
  public static final String BASE_URL = "baseURL";
  /** Type: Multi-settings, string. Connection project filter. No setting - no filter (all projects). Each setting - included integer project ID*/
  public static final String FILTERED_PROJECTS = "projects";
  /** Credentials mode. Type: boolean. true means login/password is provided, false - anonymous connection. */
  private static final String AUTHENTICATE = "authenticate";
  /** Type: boolean. True - ignore global proxy settings for this connection */
  public static final String IGNORE_PROXY = "ignoreProxy";
  /** Credentials mode. Type: string. JIRA or SSO login */
  private static final String USERNAME = "username";
  /** Type: string. Expected JIRA username. Mandatory for WebLogin and SSO: null means anonymous connection.
   * In credentials mode usually equal to {@link #USERNAME login} */
  private static final String JIRA_USERNAME = "jiraUsername";
  /** WebLogin mode. Type: string. JSON-serialized {@link WebLoginConfig WebLogin configuration} */
  private static final String WEB_LOGIN = "webLogin";
  /** Type: integer ({@link #M_CREDENTIALS}, {@link #M_WEB_LOGIN}.
   * Designates connection mode (if connection has settings for both credentials and WebLogin mode.
   * If not specified the mode can be auto-detected from existing settings. By default assume credentials mode. */
  private static final String MODE = "mode";
  /** No mode is specified - default value for backward compatibility */
  private static final int M_UNDEFINED = 0;
  /** Connection is configured in Credentials mode */
  private static final int M_CREDENTIALS = 1;
  /** Connection is configured in WebLogin mode */
  private static final int M_WEB_LOGIN = 2;
  /** Connection to JiraCloud via BasicAuth */
  private static final int M_BASIC_AUTH = 3;

  @Nullable
  public static String getBaseUrl(ReadonlyConfiguration config) {
    return config != null ? config.getSetting(BASE_URL, null) : null;
  }

  public static String normalizeBaseUrl(String baseUrl) {
    try {
      return HttpUtils.normalizeBaseUrl(baseUrl);
    } catch (MalformedURLException e) {
      return null;
    }
  }

  public static boolean setBaseUrl(Configuration config, String baseUrl) {
    if (baseUrl == null) return false;
    config.setSetting(BASE_URL, baseUrl);
    return true;
  }

  /**
   * @param config connection configuration
   * @return WebLogin configuration even when connection is configured to Credentials mode.
   */
  @Nullable
  public static WebLoginConfig getWebLogin(ReadonlyConfiguration config) {
    String json = config.getSetting(WEB_LOGIN, null);
    if (json == null || json.isEmpty()) return null;
    return WebLoginConfig.fromJson(json);
  }

  /**
   * @param jiraUsername JIRA username for authenticated connections, or null for anonymous
   * @see #getJiraUsername(ReadonlyConfiguration) */
  public static void setJiraUsername(Configuration config, String jiraUsername) {
    if (jiraUsername != null && jiraUsername.isEmpty()) jiraUsername = null;
    config.setSetting(JIRA_USERNAME, jiraUsername);
  }

  /**
   * Replaces WebLogin configuration, and switches connection to WebLogin mode
   * @param config connection configuration
   * @param webLogin updated WebLogin configuration
   */
  public static void setWebLogin(Configuration config, WebLoginConfig webLogin) {
    if (webLogin == null) {
      LogHelper.error("Null webLogin");
      return;
    }
    config.setSetting(MODE, M_WEB_LOGIN);
    config.setSetting(WEB_LOGIN, webLogin.toJson());
  }

  /**
   * Test if current configuration is configured with WebLogin
   * @param config configuration to test
   * @return true if configuration has WebLogin settings
   */
  public static boolean isWebLogin(ReadonlyConfiguration config) {
    int mode = config.getIntegerSetting(MODE, M_UNDEFINED);
    if (mode == M_WEB_LOGIN) return true;
    if (mode == M_CREDENTIALS || mode == M_BASIC_AUTH) return false;
    String json = config.getSetting(WEB_LOGIN, null);
    return json != null && !json.isEmpty();
  }

  public static boolean isBasicAuth(ReadonlyConfiguration config) {
    int mode = config.getIntegerSetting(MODE, M_UNDEFINED);
    return mode == M_BASIC_AUTH;
  }

  /**
   * @param config connection configuration
   * @return Credentials mode configuration, even when connection in WebLogin mode
   */
  @Contract("!null -> !null")
  public static JiraLoginInfo getLoginInfo(ReadonlyConfiguration config) {
    if (config == null) return null;
    boolean auth = config.getBooleanSetting(AUTHENTICATE, true);
    if (!auth) return JiraLoginInfo.ANONYMOUS;
    String login = config.getSetting(USERNAME, null);
    String password = PasswordUtil.getPassword(config);
    String username = config.getSetting(JIRA_USERNAME, ""); // Get empty username: Prevent startup failure caused by a Cloud connection config
    return new JiraLoginInfo(login, password, false, username);
  }

  @Nullable
  private static String getLogin(ReadonlyConfiguration config) {
    return config != null ? config.getSetting(USERNAME, null) : null;
  }

  public static boolean isIgnoreProxy(ReadonlyConfiguration config) {
    return config != null && config.getBooleanSetting(IGNORE_PROXY, false);
  }

  /**
   * @return set of configured project ids or null if no filter configured (all projects accepted)
   */
  @Nullable
  public static Set<Integer> getProjectsFilter(ReadonlyConfiguration configuration) {
    Set<Integer> result = Collections15.hashSet();
    List<String> list = configuration.getAllSettings(FILTERED_PROJECTS);
    if (list.isEmpty())
      return null;
    for (String id : list) {
      try {
        result.add(Integer.parseInt(id));
      } catch(NumberFormatException e) {
        Log.error("Wrong project id: " + id);
      }
    }
    return result.isEmpty() ?  null : result;
  }

  public static String getConnectionName(ReadonlyConfiguration config) {
    return Util.NN(config != null ? config.getSetting(CommonConfigurationConstants.CONNECTION_NAME, null) : null, "unnamed");
  }

  /**
   * Updates Credential mode settings and switches connection to Credentials mode
   * @param config connection configuration
   * @param userInfo credentials mode settings
   */
  public static void setLoginPassword(Configuration config, JiraLoginInfo userInfo) {
    config.setSetting(JiraConfiguration.AUTHENTICATE, !userInfo.isAnonymous());
    config.setSetting(JiraConfiguration.USERNAME, userInfo.getLogin());
    PasswordUtil.setPassword(config, userInfo.getPassword());
    config.setSetting(JIRA_USERNAME, userInfo.getJiraUsername());
    config.setSetting(MODE, M_CREDENTIALS);
  }

  public static void setBasicAuth(Configuration config, JiraLoginInfo info) {
    if (info.isAnonymous() || info.getJiraUsername().isEmpty()) {
      LogHelper.error("Wrong BasicAuth info:", info);
      setLoginPassword(config, JiraLoginInfo.ANONYMOUS);
    } else {
      config.setSetting(JiraConfiguration.USERNAME, info.getLogin());
      PasswordUtil.setPassword(config, info.getPassword());
      config.setSetting(JIRA_USERNAME, info.getJiraUsername());
      config.setSetting(MODE, M_BASIC_AUTH);
    }
  }

  /**
   * Extracts expected JIRA username if it is specified.
   * @param config connection configuration
   * @return JIRA username if the connection is authenticated and username is specified, otherwise null
   */
  @Nullable
  public static String getJiraUsername(ReadonlyConfiguration config) {
    if (config == null) return null;
    String username = config.getSetting(JIRA_USERNAME, null);
    if (username == null) username = Util.NN(getLogin(config));
    return username.isEmpty() ? null : username;
  }

  /**
   * Compares synchronization settings of two configurations. Takes into account only settings which affects synchronization.<br>
   * Synchronization can be affected by these settings:
   * <ul>
   *   <li>{@link #JIRA_USERNAME JIRA username}</li>
   *   <li>{@link #FILTERED_PROJECTS}</li>
   * </ul>
   * @param config1 configuration 1
   * @param config2 configuration 2
   * @return true if synchronization is not affected by the difference
   */
  public static boolean isSyncSettingEqual(ReadonlyConfiguration config1, ReadonlyConfiguration config2) {
    if (config1 == config2) return true;
    if (config1 == null || config2 == null) return false;
    if (!Objects.equals(getBaseUrl(config1), getBaseUrl(config2))) return false;
    if (!Objects.equals(getJiraUsername(config1), getJiraUsername(config2))) return false;
    Set<Integer> projects1 = getProjectsFilter(config1);
    Set<Integer> projects2 = getProjectsFilter(config2);
    return Objects.equals(projects1, projects2);
  }
}
