package com.almworks.jira.provider3.services;

import com.almworks.util.Env;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains regular expressions for JIRA project key and issues. Those can be redefined with
 * system properties. It's possible to have these properties per connection - post a feature request when
 * somebody asks.
 *
 * @author Igor Sereda
 */
public class JiraPatterns {
  private static final String JIRA_PROJECTKEY_PATTERN_PROPERTY = "jira.projectkey.pattern";

  public static final String PROJECT_KEY_PATTERN;
  public static final String ISSUE_KEY_PATTERN;

  private static final Pattern ISSUE_KEY_PARSE_REGEXP;
  private static final Pattern ISSUE_KEY_MAYBE_REGEXP = Pattern.compile("(.+)-(\\d+)$");
  public static final Pattern ISSUE_URL_PARSE_REGEXP;

  private static final String DEFAULT_PROJECT_KEY_PATTERN = "[A-Z][A-Z_0-9]+";

  static {
    String pattern = readCustomProjectKeyPattern();
    if(pattern == null) {
      pattern = DEFAULT_PROJECT_KEY_PATTERN;
    }

    PROJECT_KEY_PATTERN = pattern;
    ISSUE_KEY_PATTERN = getIssueKeyCheckPattern(pattern);
    ISSUE_KEY_PARSE_REGEXP = Pattern.compile(getIssueKeyParsePattern(pattern));
    ISSUE_URL_PARSE_REGEXP = Pattern.compile(getIssueUrlParsePattern(pattern), Pattern.CASE_INSENSITIVE);
  }

  private static String readCustomProjectKeyPattern() {
    String pattern = Env.getString(JIRA_PROJECTKEY_PATTERN_PROPERTY);
    if (pattern != null) {
      Log.warn("trying non-standard project key pattern " + pattern);
      pattern = pattern.trim();
      if (pattern.startsWith("(") && pattern.endsWith(")"))
        pattern = pattern.substring(1, pattern.length() - 1);
      if (pattern.indexOf('(') >= 0) {
        Log.warn("jira project key pattern cannot contain '(' - " + pattern);
        pattern = null;
      } else {
        try {
          Pattern.compile(pattern);
          Pattern.compile(getIssueKeyCheckPattern(pattern));
          Pattern.compile(getIssueKeyParsePattern(pattern));
          Pattern.compile(getIssueUrlParsePattern(pattern));
        } catch (Exception e) {
          Log.warn("cannot compile pattern " + pattern, e);
          pattern = null;
        }
      }
    }
    return pattern;
  }

  private static String getIssueKeyCheckPattern(String projectKeyPattern) {
    return String.format("(?:%s)-\\d+", projectKeyPattern);
  }

  private static String getIssueKeyParsePattern(String projectKeyPattern) {
    return String.format("(%s)-(\\d+)", projectKeyPattern);
  }

  private static String getIssueUrlParsePattern(String projectKeyPattern) {
    return String.format("^(.+)/browse/(%s-\\d+)$", projectKeyPattern);
  }

  /** Use when false positives can be tolerated. */
  public static boolean canBeAnIssueKey(@Nullable String string) {
    return extractProjectKey0(string, false) != null;
  }

  @Nullable
  public static String extractProjectKeyNoLog(@Nullable String issueKey) {
    return extractProjectKey0(issueKey, false);
  }

  @Nullable
  private static String extractProjectKey0(@Nullable String issueKey, boolean reportError) {
    if (issueKey == null) return null;
    String projectKey = extractProjectKey1(issueKey.trim(), reportError);
    if (projectKey == null) return null;
    projectKey = projectKey.trim();
    return projectKey.isEmpty() ? null : projectKey;
  }

  private static String extractProjectKey1(@NotNull String issueKey, boolean reportError) {
    Matcher m1 = ISSUE_KEY_PARSE_REGEXP.matcher(issueKey);
    if (m1.matches()) {
      return m1.group(1);
    }

    Matcher m2 = ISSUE_KEY_MAYBE_REGEXP.matcher(issueKey);
    if (m2.matches()) {
      if (reportError) Log.warn("non-matching issue key [1]: " + issueKey);
      return m2.group(1);
    }

    if (reportError) Log.error("non-matching issue key [2]: " + issueKey);
    return null;
  }
}
