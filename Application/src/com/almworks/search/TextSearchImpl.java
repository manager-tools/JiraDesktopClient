package com.almworks.search;

import com.almworks.api.application.*;
import com.almworks.api.application.tree.ConnectionNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryUtil;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.search.TextSearch;
import com.almworks.api.search.TextSearchExecutor;
import com.almworks.api.search.TextSearchType;
import com.almworks.util.collections.Containers;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TextSearchImpl implements TextSearch {
  private final ExplorerComponent myExplorerComponent;

  private final SortedSet<TextSearchType> mySearchTypes = Collections15.treeSet(new Comparator<TextSearchType>() {
    public int compare(TextSearchType o1, TextSearchType o2) {
      if (o1.equals(o2))
        return 0;
      int w1 = o1.getWeight();
      int w2 = o2.getWeight();
      if (w2 < w1) {
        return -1;
      } else if (w2 > w1) {
        return 1;
      } else {
        int h1 = o1.hashCode();
        int h2 = o2.hashCode();
        assert h1 != h2;
        return Containers.compareInts(h1, h2);
      }
    }
  });

  public TextSearchImpl(ExplorerComponent explorerComponent, TextSearchType[] types) {
    myExplorerComponent = explorerComponent;
    mySearchTypes.addAll(Arrays.asList(types));
  }

  @ThreadAWT
  public SearchResult search(String text, final Collection<? extends GenericNode> nodes, @Nullable TabKey tabKey) throws CantPerformExceptionSilently {
    Threads.assertAWTThread();
    final String searchText = Util.NN(text).trim();
    final TextSearchExecutor result = getSearchExecutor(searchText);
    if (result == null) {
      showNoResults(searchText, tabKey, nodes);
      return SearchResult.EMPTY;
    } else {
      ItemSource source = result.executeSearch(nodes);
      Collection<GenericNode> realNodes = Collections15.arrayList(nodes);
      realNodes = result.getRealScope(realNodes);
      if (realNodes.isEmpty()) realNodes = (Collection<GenericNode>) nodes;
      return myExplorerComponent.showItemsInTab(source, createContext(result.getSearchDescription(), tabKey, realNodes), true);
    }
  }

  @ThreadAWT
  public void search(String searchString, @Nullable TabKey tabKey) throws CantPerformExceptionSilently {
    RootNode root = myExplorerComponent.getRootNode();
    if (root != null) {
      search(searchString, Collections.singleton(root), tabKey);
    }
  }

  @ThreadAWT
  public SearchResult search(String searchString, @NotNull Connection connection, @Nullable TabKey tabKey) throws CantPerformExceptionSilently {
    ConnectionNode node = QueryUtil.findConnectionNode(myExplorerComponent, connection);
    if (node != null) return search(searchString, Collections.singleton(node), tabKey);
    else return SearchResult.EMPTY;
  }

  /**
   * Returns type of search and adjusted nodes
   * type is nullable
   * nodes are not nullable
   */
/*
  public Pair<String, Collection<GenericNode>> getTypeAndScopeAdjustmentForSearchString(String searchString, Collection<GenericNode> nodes) {
    TextSearchExecutor parser = getSearchExecutor(searchString);
    Collection<GenericNode> result = nodes;
    if (parser != null) {
      result = parser.getRealScope(nodes);
      if (result == null)
        result = nodes;
    }
    String type = parser == null ? null : parser.getType().getDisplayableShortName();
    return Pair.create(type, result);
  }
*/

/*
  public void performSearchOnConnections(String text, final Collection<Connection> connections) {
    Threads.assertAWTThread();
    Collection<GenericNode> nodes = Collections15.arrayList();
    RootNode node = myExplorerComponent.getRootNode();
    for (int i = 0; i < node.getChildrenCount(); i++) {
      GenericNode subnode = node.getChildAt(i);
      if (subnode instanceof ConnectionNode) {
        if (connections.contains(subnode.getConnection())) {
          nodes.add(subnode);
        }
      }
    }
    if (nodes.isEmpty()) {
      showNoResults(text);
    } else {
      performSearch(text, nodes);
    }
  }
*/
  private void showNoResults(String text, TabKey tabKey, Collection<? extends GenericNode> context) {
    myExplorerComponent.showItemsInTab(ItemSource.EMPTY, createContext(text, tabKey, context), false);
  }

  private ItemCollectionContext createContext(String text, TabKey tabKey, Collection<? extends GenericNode> context) {
    return ItemCollectionContext.createTextSearch(text, context, Util.NN(tabKey, SMART_SEARCH_KEY));
  }

  @ThreadAWT
  @Nullable
  public TextSearchExecutor getSearchExecutor(String searchString) {
    searchString = Util.NN(searchString).trim();
    for (TextSearchType searchType : mySearchTypes) {
      TextSearchExecutor result = searchType.parse(searchString);
      if (result != null)
        return result;
    }
    return null;
  }

  @ThreadAWT
  public void addTextSearchType(@NotNull Lifespan life, @NotNull final TextSearchType type) {
    if (life.isEnded())
      return;
    if (mySearchTypes.contains(type))
      return;
    mySearchTypes.add(type);
    life.add(new Detach() {
      protected void doDetach() {
        mySearchTypes.remove(type);
      }
    });
  }
}
