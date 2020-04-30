package com.almworks.api.application;

import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.MapMedium;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.TreeUtil;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ItemCollectionContext {
  public static final DataRole<ItemCollectionContext> ROLE = DataRole.createRole(ItemCollectionContext.class);
  public static final String PATH_SEPARATOR = " - ";
  @Nullable
  private final GenericNode myQuery;
  private final Collection<? extends GenericNode> myContextNodes;
  @NotNull
  private final String myShortName;
  @NotNull
  private final String myTooltip;


  @Nullable
  private final Connection mySourceConnection;
  private final TabKey myQueryKey;

  private final boolean myUseShortNameForHighlight;
  private Configuration mySpecificConfig = null;

  private boolean myForceRerun = false;
  private static final String QUERY_CONTEXT = "queryContext";

  public boolean isUseShortNameForHighlight() {
    return myUseShortNameForHighlight;
  }


  public static ItemCollectionContext createQueryNode(GenericNode query, String shortName,
    @Nullable NodeTabKey queryKey)
  {
    if (queryKey == null)
      queryKey = new NodeTabKey(query);
    return new ItemCollectionContext(query, null, shortName, null, queryKey, contextNodes(query.getParent()), false);
  }

  public static ItemCollectionContext createRemoteQuery(GenericNode query, String name,
    @Nullable NodeTabKey queryKey)
  {
    if (queryKey == null)
      queryKey = new NodeTabKey(query);
    return new ItemCollectionContext(query, null, name, "Remote Query: " + name, queryKey, contextNodes(query.getParent()), false);
  }

  public static ItemCollectionContext createNoNode(@NotNull String shortName, @Nullable String tooltip,
    TabKey key)
  {
    assert key != null;
    return new ItemCollectionContext(null, null, shortName, tooltip, key, contextNodes(), false);
  }

  private static Collection<? extends GenericNode>  contextNodes(GenericNode... nodes) {
    boolean empty = true;
    for (GenericNode node : nodes) {
      if (node != null) {
        empty = false;
        break;
      }
    }
    if (empty)
      return Collections.emptyList();
    List<GenericNode> result = Collections15.arrayList(nodes.length);
    for (GenericNode node : nodes) {
      if (node != null) {
        result.add(node);
      }
    }
    return result;
  }

  public static ItemCollectionContext createSummary(@NotNull String constraint, @Nullable GenericNode node,
    TabKey key, Collection<? extends GenericNode> context)
  {
    String tooltip;
    if (node == null)
      tooltip = constraint;
    else
      tooltip = TextUtil.separate(node.getPathFromRoot(), PATH_SEPARATOR, GenericNode.GET_NAME) + ": " + constraint;
    return new ItemCollectionContext(null, getCommonConnection(context), constraint, tooltip, key, context, false);
  }

  public static ItemCollectionContext createGeneral(String shortName, @Nullable Connection sourceConnection) {
    return new ItemCollectionContext(null, sourceConnection, shortName, null, null, contextNodes(), false);
  }

  public static ItemCollectionContext createTextSearch(String text, Collection<? extends GenericNode> context,
    TabKey key)
  {
    String shortText;

    Collection<? extends GenericNode> path;
    String pathSeparator;
    if (context.size() == 1) {
      path = context.iterator().next().getPathFromRoot();
      pathSeparator = PATH_SEPARATOR;
    } else {
      path = context;
      pathSeparator = ", ";
    }
    String tooltip;
    if (path.size() < 2) {
      shortText = text;
      tooltip = path.isEmpty() ? text : text + " (in " + path.iterator().next().getName() + ")";
    } else {
      shortText = text.length() <= 30 ? text : text.substring(0, 30);
      tooltip = text + " (in " + TextUtil.separate(path, pathSeparator, GenericNode.GET_NAME) + ")";
    }
    return new ItemCollectionContext(null, getCommonConnection(context), shortText, tooltip, key, context, true);
  }

  private static Connection getCommonConnection(Collection<? extends GenericNode> context) {
    Connection commonConn = null;
    for(final GenericNode n : context) {
      final Connection conn = DBDataRoles.lookForConnectionInAncestors(n.getTreeNode());
      if(conn == null || (commonConn != null && conn != commonConn)) {
        commonConn = null;
        break;
      }
      commonConn = conn;
    }
    return commonConn;
  }

  public static ItemCollectionContext createLinked(ConnectionNode connection, String name, TabKey key,
    @Nullable ItemCollectionContext configSource) {
    ItemCollectionContext context = new ItemCollectionContext(null, connection.getConnection(), name, null, key,
      Collections.singleton(connection), false);
    Configuration config = null;
    if (configSource != null) config =  configSource.getContextConfig();
    if (config == null && connection != null) config = obtainContextConfig(connection);
    context.mySpecificConfig = config;
    return context;
  }

  private ItemCollectionContext(
    @Nullable GenericNode query, @Nullable Connection sourceConnection,
    @NotNull String shortName, @Nullable String tooltip, TabKey queryKey,
    Collection<? extends GenericNode> contextNodes, boolean useShortNameForHighlight)
  {
    myQuery = query;
    myShortName = shortName;
    myUseShortNameForHighlight = useShortNameForHighlight;
    myTooltip = tooltip == null ? shortName : tooltip;
    myQueryKey = queryKey;
    if(sourceConnection != null) {
      mySourceConnection = sourceConnection;
    } else {
      mySourceConnection = query != null ? DBDataRoles.lookForConnectionInAncestors(query.getTreeNode()) : null;
    }
    myContextNodes = contextNodes;
  }

  @NotNull
  public String getShortName() {
    // todo #1322
    return myShortName;
  }

  @Nullable
  public GenericNode getQuery() {
    return myQuery;
  }

  @Nullable
  public Connection getSourceConnection() {
    return mySourceConnection;
  }

  public TabKey getQueryKey() {
    return myQueryKey;
  }

  public boolean isReplaceTab(TabKey tabKey) {
    return myForceRerun || (myQueryKey != null && myQueryKey.isReplaceTab(tabKey));
  }

  public boolean isSameType(@Nullable TabKey key) {
    return myQueryKey != null && myQueryKey.equals(key);
  }

  public void forceRerun() {
    myForceRerun = true;
  }

  public String getTooltip() {
    if (myQuery == null)
      return myTooltip;
    List<GenericNode> path = myQuery.getPathFromRoot();
    return TextUtil.separate(path, PATH_SEPARATOR, GenericNode.GET_NAME);
  }

  @Nullable
  public Collection<? extends GenericNode> getContextNodes() {
    return myContextNodes;
  }

  /**
   * Return configution to store context related settings. Null means the context has no attached config, settings cannot be
   * persisted and defaults should be used. Not null config can happen to be empty, this means that no settings were ever
   * stored here so defaults should be copied in such case. 
   * @return persistent config or null
   */
  @Nullable
  public Configuration getContextConfig() {
    if (mySpecificConfig != null) return mySpecificConfig;
    GenericNode node = myQuery;
    boolean copyConfig = false;
    if (node == null && myContextNodes != null && !myContextNodes.isEmpty()) {
      node = TreeUtil.commonAncestor(myContextNodes, GenericNode.GET_PARENT_NODE);
      copyConfig = true;
    }
    if (node == null) return null;
    Configuration config = obtainContextConfig(node);
    if (copyConfig && config != null) {
      Configuration copy = MapMedium.createConfig();
      ConfigurationUtil.copyTo(config, copy);
      mySpecificConfig = copy;
      config = copy;
    }
    return config;
  }

  private static Configuration obtainContextConfig(GenericNode node) {
    Configuration queueConfig = node.getConfiguration();
    Configuration ownConfig = queueConfig.getOrCreateSubset(QUERY_CONTEXT);
    if (queueConfig.isEmpty()) return null; // Queue config still empty after child creation. Supposing empty config medium.
    if (!ownConfig.isEmpty()) return ownConfig;
    while (node != null) {
      Configuration config = node.getConfiguration();
      node = node.getParent();
      if (!config.isSet(QUERY_CONTEXT)) continue;
      Configuration subset = config.getSubset(QUERY_CONTEXT);
      if (subset.isEmpty()) continue;
      ConfigurationUtil.copyTo(subset, ownConfig);
      break;
    }
    return ownConfig;
  }
}