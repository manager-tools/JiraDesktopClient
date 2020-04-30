package com.almworks.jira.connector2;

import com.almworks.util.i18n.text.LocalizedAccessor;

public class JiraCaptchaRequired extends JiraException {
  public static final LocalizedAccessor.Value SHORT = JiraEnv.I18N_LOCAL.getFactory("captcha.short");
  private static final LocalizedAccessor.Value LONG = JiraEnv.I18N_LOCAL.getFactory("captcha.long");

  public JiraCaptchaRequired() {
    super("jira login failed", SHORT.create(), LONG.create(), JiraCause.ACCESS_DENIED);
  }
}
