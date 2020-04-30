package com.almworks.jira.provider3.issue.features;

import com.almworks.api.application.ItemWrapper;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.util.i18n.text.LocalizedAccessor;

import java.util.Collection;

public class DescriptorWindowTitle {
  private static final LocalizedAccessor.Value NEW_ISSUE = EditIssueFeature.I18N.getFactory("edit.screens.window.title.newIssue");

  private final String mySingle;
  private final LocalizedAccessor.MessageInt myMulti;

  public DescriptorWindowTitle(String single, LocalizedAccessor.MessageInt multi) {
    mySingle = single;
    myMulti = multi;
  }

  public static DescriptorWindowTitle create(String singleKey, String multiKey) {
    String single = EditIssueFeature.I18N.getString(singleKey);
    LocalizedAccessor.MessageInt multi = EditIssueFeature.I18N.messageInt(multiKey);
    return new DescriptorWindowTitle(single, multi);
  }

  public String createTitle(Collection<ItemWrapper> issues) {
    int issueCount = issues.size();
    if (issueCount != 1) return myMulti.formatMessage(issueCount);
    ItemWrapper issue = issues.iterator().next();
    if (issue == null) return mySingle;
    String title = mySingle;
    String issueKey = LoadedIssueUtil.getIssueKey(issue);
    if (issueKey == null || issueKey.isEmpty()) issueKey = NEW_ISSUE.create();
    return issueKey + " - " + title;
  }
}
