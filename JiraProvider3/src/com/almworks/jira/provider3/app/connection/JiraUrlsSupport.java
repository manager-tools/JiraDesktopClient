package com.almworks.jira.provider3.app.connection;

import com.almworks.api.application.ItemSource;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.gui.actions.source.IssuesByKeyItemSource;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.IssueUrl;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Set;

class JiraUrlsSupport {
  private final JiraConnection3 myConnection;

  JiraUrlsSupport(JiraConnection3 connection) {
    myConnection = connection;
  }

  public JiraConfigHolder getConfigHolder() {
    return myConnection.getConfigHolder();
  }

  public String getConnectionUrl() {
    return getConfigHolder().getBaseUrl();
  }

  public String getItemUrl(ItemVersion localVersion) {
    String key = localVersion.getValue(Issue.KEY);
    if (key == null) return null;
    String baseUrl = getConnectionUrl();
    if (baseUrl == null) {
      LogHelper.error("Missing baseUrl");
      return null;
    }
    return IssueUrl.getIssueUrlFromNormalizedBaseUrl(baseUrl, key);
  }

  public boolean isItemUrl(String itemUrl) {
    String baseUrl = getConnectionUrl();
    return baseUrl != null && Util.equals(baseUrl, IssueUrl.getNormalizedBaseUrl(itemUrl));
  }

  public ItemSource getItemSourceForUrls(Iterable<String> urls) {
    Set<String> ids = Collections15.linkedHashSet();
    for (String url : urls) {
      IssueUrl issueUrl = IssueUrl.parseUrl(url);
      if (issueUrl != null) ids.add(issueUrl.getKey());
    }
    return ids.isEmpty() ? null : IssuesByKeyItemSource.create(myConnection, ids);
  }
}
