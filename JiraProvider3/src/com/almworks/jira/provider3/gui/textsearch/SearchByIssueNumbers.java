package com.almworks.jira.provider3.gui.textsearch;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.util.sources.CompositeItemSource;
import com.almworks.api.engine.Connection;
import com.almworks.api.search.TextSearchExecutor;
import com.almworks.api.search.TextSearchType;
import com.almworks.api.search.TextSearchUtils;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.source.IssuesByKeyItemSource;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Condition;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class SearchByIssueNumbers implements TextSearchType {
  private static final Pattern NUMBERS = Pattern.compile("\\d+([,; \t]+\\d+)*");
  private final GuiFeaturesManager myFeatures;

  public SearchByIssueNumbers(GuiFeaturesManager features) {
    myFeatures = features;
  }

  @Override
  public TextSearchExecutor parse(String searchString) {
    if(searchString != null && NUMBERS.matcher(searchString).matches()) {
      return new MyExecutor(myFeatures, this, searchString);
    }
    return null;
  }

  @Override
  public int getWeight() {
    return Weight.ID_SEARCH;
  }

  @Override
  public String getDisplayableShortName() {
    return "issue key";
  }

  private static class MyExecutor implements TextSearchExecutor {
    private final TextSearchType myType;
    private final String mySearchString;
    private final GuiFeaturesManager myFeatures;

    public MyExecutor(GuiFeaturesManager features, TextSearchType type, String searchString) {
      myFeatures = features;
      myType = type;
      mySearchString = searchString;
    }

    @NotNull
    @Override
    public TextSearchType getType() {
      return myType;
    }

    @NotNull
    @Override
    public String getSearchDescription() {
      return mySearchString;
    }

    @NotNull
    @Override
    public ItemSource executeSearch(Collection<? extends GenericNode> scope) throws CantPerformExceptionSilently {
      final Map<JiraConnection3, Set<ItemKey>> allProjects = extractProjects(scope);
      final String[] numbers = mySearchString.split(TextSearchUtils.WORD_DELIMITERS);
      return new MySource(mySearchString, numbers, allProjects);
    }

    private Map<JiraConnection3, Set<ItemKey>> extractProjects(Collection<? extends GenericNode> scope) {
      final Map<JiraConnection3, Set<ItemKey>> allProjects = Collections15.hashMap();
      EnumTypesCollector.Loaded project = myFeatures.getEnumTypes().getType(Project.ENUM_TYPE);
      if (project == null) {
        LogHelper.error("Missing projects");
        return allProjects;
      }

      for(final GenericNode node : scope) {
        Connection c = node.getConnection();
        if(c == null) return extractProjects(getAllConnectionNodes(node));
        JiraConnection3 connection = Util.castNullable(JiraConnection3.class, c);
        if (connection == null) continue;
        ItemHypercube cube = node.getHypercube(false);
        if(cube == null) continue;
        SortedSet<Long> aps = cube.getIncludedValues(Issue.PROJECT);
        List<LoadedItemKey> nodeProjects = aps == null ? project.getEnumValues(cube) : project.getResolvedItems(aps);
        if(nodeProjects.isEmpty()) continue;
        putProjects(allProjects, connection, nodeProjects);
      }

      return allProjects;
    }

    private Collection<GenericNode> getAllConnectionNodes(GenericNode someNode) {
      final RootNode root = someNode.getRoot();
      if(root == null) {
        assert false : someNode;
        return Collections15.emptyCollection();
      }
      return root.collectNodes(new Condition<GenericNode>() {
        @Override
        public boolean isAccepted(GenericNode value) {
          return value instanceof ConnectionNode;
        }
      });
    }

    private void putProjects(
      Map<JiraConnection3, Set<ItemKey>> allProjects, JiraConnection3 connection, Collection<LoadedItemKey> nodeProjects)
    {
      Set<ItemKey> set = allProjects.get(connection);
      if(set == null) {
        set = Collections15.hashSet();
        allProjects.put(connection, set);
      }
      set.addAll(nodeProjects);
    }

    @NotNull
    @Override
    public Collection<GenericNode> getRealScope(@NotNull Collection<GenericNode> nodes) {
      for(final GenericNode node : nodes) {
        if(node.getConnection() == null) {
          return Collections15.list((GenericNode)node.getRoot());
        }
      }
      return nodes;
    }
  }

  private static class MySource extends CompositeItemSource {
    private final Map<JiraConnection3, Set<ItemKey>> myProjects;
    private final String[] myNumbers;

    public MySource(String name, String[] numbers, Map<JiraConnection3, Set<ItemKey>> projects) {
      super(name, "");
      myProjects = projects;
      myNumbers = numbers;
    }

    @Override
    protected void reloadingPrepare(ItemsCollector collector) {
      clear(collector);
      for(final Map.Entry<JiraConnection3, Set<ItemKey>> e : myProjects.entrySet()) {
        final JiraConnection3 connection = e.getKey();
        final String[] issueKeys = getIssueKeys(myNumbers, e.getValue());
        addSourceForKeys(collector, connection, issueKeys);
      }
    }

    private String[] getIssueKeys(String[] nums, Set<ItemKey> projects) {
      final List<String> keys = Collections15.arrayList();
      for(final ItemKey p : projects) {
        final String key = p.getId();
        for(final String num : nums) {
          keys.add(key + "-" + num);
        }
      }
      return keys.toArray(new String[keys.size()]);
    }

    private void addSourceForKeys(ItemsCollector collector, JiraConnection3 connection, String[] keys) {
      if(keys.length > 0) {
        add(collector, new IssuesByKeyItemSource(keys, connection), 2000);
      }
    }
  }
}
