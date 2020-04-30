package com.almworks.api.search;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface TextSearchExecutor {
  @NotNull
  ItemSource executeSearch(Collection<? extends GenericNode> scope) throws CantPerformExceptionSilently;

  @NotNull
  TextSearchType getType();

  @NotNull
  String getSearchDescription();

  @NotNull
  Collection<GenericNode> getRealScope(@NotNull Collection<GenericNode> nodes);
}
