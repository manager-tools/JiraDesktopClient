package com.almworks.restconnector.login;

import com.almworks.api.http.HttpUtils;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;

import java.net.MalformedURLException;
import java.util.Objects;

/**
 * JIRA account is an username at the specific server.<br>
 * Account can be:<br>
 * <ul>
 *   <li>authenticated - when username is provided</li>
 *   <li>anonymous - the anonymous access to the JIRA server</li>
 *   <li>unknown authenticated - not-anonymous access to the JIRA server, but actual username is not known yet</li>
 * </ul>
 * This objects supports equality. The two objects are equal when both relate to the same server and both are either anonymous
 * or has the same known username. The unknown authenticated account is equal to itself only (the same java-instance).
 */
public class JiraAccount {
  /**
   * Normalized JIRA base URL
   */
  private final String myBaseUrl;
  /**
   * Not empty string means JIRA account<br>
   * null means anonymous<br>
   * empty string means "account is unknown". The instance of unknown account cannot be equal to any other instance
   */
  private final String myAccountId;

  private JiraAccount(String baseUrl, String accountId) {
    myBaseUrl = baseUrl;
    myAccountId = accountId;
  }

  public String getBaseUrl() {
    return myBaseUrl;
  }

  /**
   * @return true if the object points to a known account or anonymous access
   */
  public boolean isKnow() {
    return myAccountId == null || !myAccountId.isEmpty();
  }

  /**
   * Creates new instance with normalized base URL
   * @param baseUrl JIRA base URL. Can be not-normalized
   * @param accountId specifies account, anonymous access or unknown authenticated. See {@link #myAccountId} for details
   * @return new instance
   */
  public static JiraAccount create(String baseUrl, String accountId) {
    String normalized = normalize(baseUrl);
    return new JiraAccount(normalized, accountId);
  }

  public static String normalize(String baseUrl) {
    String normalized;
    try {
      normalized = HttpUtils.normalizeBaseUrl(baseUrl);
    } catch (MalformedURLException e) {
      LogHelper.error(e, baseUrl);
      normalized = baseUrl;
    }
    return normalized;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (myAccountId != null && myAccountId.length() == 0) return false;
    JiraAccount other = Util.castNullable(JiraAccount.class, obj);
    return other != null && Objects.equals(myBaseUrl, other.myBaseUrl) && Objects.equals(myAccountId, other.myAccountId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myBaseUrl, myAccountId);
  }

  @Override
  public String toString() {
    return String.format("JiraAccount[%s at %s]", isKnow() ? (myAccountId == null ? "<anonymous>" : myAccountId) : "<Unknown>", myBaseUrl);
  }
}
