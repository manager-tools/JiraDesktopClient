package com.almworks.jira.connector2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraServerVersionInfo {
  private static final Pattern VERSION_PATTERN = Pattern.compile("^v?([0-9]+)(?:\\.([0-9]+))?(?:\\.([0-9]+))?");

  private String myVersion;

  private JiraServerVersionInfo(String version) {
    myVersion = version;
  }

  public String getVersion() {
    return myVersion;
  }

  public String toString() {
    return myVersion;
  }

  @NotNull
  public static JiraServerVersionInfo create(String version) {
    return new JiraServerVersionInfo(version);
  }

  /**
   * Checks if version equals or more specific than the parameter. For example:
   *  "3.7.1" isVersion("3");
   *  "3.7.1" isVersion("3.7");
   *  "3.7.1" isVersion("3.7.1");
   *  NOT: "3.7.1" isVersion("3.7.1.1");
   *  NOT: "3.7.1" isVersion("3.77");
   */
  public boolean isVersion(@NotNull String version, boolean safeDefault) {
    if (myVersion == null)
      return safeDefault;
    if (!myVersion.startsWith(version))
      return false;
    int k = version.length();
    if (myVersion.length() <= k)
      return true;
    char c = myVersion.charAt(k);
    return !Character.isLetterOrDigit(c);
  }

  public boolean isVersionOrLater(@NotNull String version, boolean safeDefault) {
    Boolean versionOrLater = isVersionOrLater(version);
    return versionOrLater != null ? versionOrLater : safeDefault;
  }

  /**
   * Checks if version equals or later than the parameter. For example:
   *  "3.7.1" isVersionOrLater("3");
   *  "3.7.1" isVersionOrLater("3.7");
   *  "3.7.1" isVersionOrLater("3.7.1");
   *  "3.7.1" isVersionOrLater("3.7.0");
   *  "3.7.1" isVersionOrLater("3.8");
   *  NOT: "3.7.1" isVersionOrLater("3.7.1.1");
   */
  @Nullable
  public Boolean isVersionOrLater(@NotNull String version) {
    if (myVersion == null)
      return null;
    int numeric = versionToNumber(myVersion);
    int requiredNumeric = versionToNumber(version);
    if (numeric < 0 || requiredNumeric < 0)
      return null;
    else
      return numeric >= requiredNumeric;
  }

  private int versionToNumber(String version) {
    final int[] ints = parseVersion(version);
    if(ints == null) {
      return -1;
    }
    return ints[0] * 10000 + ints[1] * 100 + ints[2];
  }

  public static int[] parseVersion(String jiraVersion) {
    final Matcher matcher = VERSION_PATTERN.matcher(jiraVersion);
    if(!matcher.find()) {
      return null;
    }

    final int[] result = { 0, 0, 0 };
    for(int i = 0; i < 3; i++) {
      final String str = matcher.group(i + 1);
      if(str != null && !str.isEmpty()) {
        result[i] = Integer.parseInt(str);
      }
    }

    return result;
  }
}
