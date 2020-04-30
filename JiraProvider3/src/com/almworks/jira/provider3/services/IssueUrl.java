package com.almworks.jira.provider3.services;

import com.almworks.api.http.HttpUtils;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.util.regex.Matcher;

public class IssueUrl {
  private final String myBaseUrl;
  private String myNormalizedBaseUrl;
  private final String myKey;

  private IssueUrl(String baseUrl, String key) {
    myBaseUrl = baseUrl;
    myKey = key;
  }

  @NotNull
  public String getBaseUrl() {
    return myBaseUrl;
  }

  @NotNull
  public String getNormalizedBaseUrl() throws MalformedURLException {
    if (myNormalizedBaseUrl == null) myNormalizedBaseUrl = HttpUtils.normalizeBaseUrl(myBaseUrl);
    return myNormalizedBaseUrl;
  }

  @NotNull
  public String getKey() {
    return myKey;
  }

  @Nullable
  public static IssueUrl parseUrl(String url) {
    url = Util.NN(url).trim();
    if (url.isEmpty()) return null;
    Matcher m = JiraPatterns.ISSUE_URL_PARSE_REGEXP.matcher(url);
    if (!m.find()) return null;
    return new IssueUrl(m.group(1), Util.upper(m.group(2)));
  }

  @Nullable
  public static String getNormalizedBaseUrl(String url) {
    IssueUrl issueUrl = parseUrl(url);
    try {
      return issueUrl != null ? issueUrl.getNormalizedBaseUrl() : null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  /** Use this method when false positives are OK. */
  @Nullable
  public static String getIssueUrlFromNormalizedBaseUrl(String baseUrl, String key) {
    if (key == null || !key.contains("-")) return null;
    StringBuilder builder = new StringBuilder(baseUrl);
    if (!baseUrl.endsWith("/")) builder.append("/");
    builder.append("browse/").append(key);
    return builder.toString();
  }

  @Nullable
  public static String getIssueUrl(String baseUrl, String issueKey) {
    try {
      baseUrl = HttpUtils.normalizeBaseUrl(baseUrl);
    } catch (MalformedURLException e) {
      Log.warn("Invalid issue URL: '" + baseUrl + "' " + issueKey);
      return null;
    }
    return getIssueUrlFromNormalizedBaseUrl(baseUrl, issueKey);
  }
}
