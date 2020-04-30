package com.almworks.jira.connector2.html;

import com.almworks.util.LogPrivacyPolizei;
import org.jetbrains.annotations.NotNull;

public class RestLoginPrivacyPolizei implements LogPrivacyPolizei {
  public static final RestLoginPrivacyPolizei INSTANCE = new RestLoginPrivacyPolizei();
  @NotNull
  @Override
  public String examine(@NotNull String message) {
    // JSON string is always enclosed in quots; can contain escaped quot mark, \"
    return message.replaceFirst("(\"password\"\\s*:\\s*\")(?:(?:\\\\\")|(?:[^\"]))*\"", "$1***\"");
  }
}
