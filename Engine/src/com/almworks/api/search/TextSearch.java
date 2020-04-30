package com.almworks.api.search;

import com.almworks.api.application.ReplaceTabKey;
import com.almworks.api.application.SearchResult;
import com.almworks.api.application.TabKey;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * The implementation will catch up all TextSearchTypes from container and
 * use them.
 */
public interface TextSearch {
  Role<TextSearch> ROLE = Role.role(TextSearch.class);
  TabKey SMART_SEARCH_KEY = new ReplaceTabKey("smartSearch");

//  Pair<String, Collection<GenericNode>> getTypeAndScopeAdjustmentForSearchString(String string, Collection<GenericNode> nodes);

  /**
   * Executes text search. Returns false if no searcher has been found.
   * Exception means search was aborted by the user
   */
  @ThreadAWT
  SearchResult search(String searchString, Collection<? extends GenericNode> scope, @Nullable TabKey tabKey) throws CantPerformExceptionSilently;

  @ThreadAWT
  void search(String searchString, @Nullable TabKey tabKey) throws CantPerformExceptionSilently;

  @ThreadAWT
  SearchResult search(String searchString, @NotNull Connection connection, @Nullable TabKey tabKey) throws CantPerformExceptionSilently;

  @ThreadAWT
  @Nullable
  TextSearchExecutor getSearchExecutor(String searchString);

  @ThreadAWT
  void addTextSearchType(@NotNull Lifespan life, @NotNull TextSearchType type);
}
