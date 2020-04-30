package com.almworks.jira.provider3.gui.textsearch;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.util.sources.CompositeItemSource;
import com.almworks.api.engine.Connection;
import com.almworks.api.search.TextSearchExecutor;
import com.almworks.api.search.TextSearchType;
import com.almworks.api.search.TextSearchUtils;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.source.IssuesByKeyItemSource;
import com.almworks.jira.provider3.services.JiraPatterns;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

public class SearchByIssueKeys implements TextSearchType {
  public static final Pattern JIRA_IDS = Pattern.compile(JiraPatterns.ISSUE_KEY_PATTERN + "([,; \t]+" +
    JiraPatterns.ISSUE_KEY_PATTERN + ")*");

  @Nullable
  public TextSearchExecutor parse(final String searchString) {
    if (searchString == null)
      return null;
    String s = Util.upper(searchString);
    return JIRA_IDS.matcher(s).matches() ? new MyExecutor(s) : null;
  }

  public String getDisplayableShortName() {
    return "issue key";
  }

  public int getWeight() {
    return Weight.ID_SEARCH;
  }


  private class MyExecutor implements TextSearchExecutor {
    private final String mySearchString;

    public MyExecutor(String searchString) {
      mySearchString = searchString;
    }

    @NotNull
    public ItemSource executeSearch(Collection<? extends GenericNode> scope) throws CantPerformExceptionSilently {
      Collection<GenericNode> nodes = TextSearchUtils.escalateToConnectionNodes(scope);
      final Collection<Connection> connections = TextSearchUtils.getAffectedConnections(nodes);
      final String[] keys = mySearchString.split(TextSearchUtils.WORD_DELIMITERS);
      return new CompositeItemSource(mySearchString, "") {
        protected void reloadingPrepare(ItemsCollector collector) {
          clear(collector);
          for (Connection c : connections) {
            JiraConnection3 connection = Util.castNullable(JiraConnection3.class, c);
            if (connection != null) add(collector, new IssuesByKeyItemSource(keys, connection), 2000);
          }
        }
      };
    }

    @NotNull
    public TextSearchType getType() {
      return SearchByIssueKeys.this;
    }

    @NotNull
    public String getSearchDescription() {
      return mySearchString;
    }

    @NotNull
    public Collection<GenericNode> getRealScope(@NotNull Collection<GenericNode> nodes) {
      return TextSearchUtils.escalateToConnectionNodes(nodes);
    }
  }
}
