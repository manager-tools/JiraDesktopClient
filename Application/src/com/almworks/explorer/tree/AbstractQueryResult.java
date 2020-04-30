package com.almworks.explorer.tree;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.engine.QueryUrlInfo;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractQueryResult extends SimpleModifiable implements QueryResult {
  public boolean canRunNow() {
    return true;
  }

  @ThreadAWT
  public boolean hasQueryUrl() {
    return false;
  }

  @ThreadAWT
  public void getQueryURL(@NotNull @ThreadAWT Procedure<QueryUrlInfo> urlConsumer) {
    urlConsumer.invoke(null);
  }

  @Nullable
  @ThreadAWT
  public Procedure2<ExplorerComponent, GenericNode> getRunLocallyProcedure() {
    return null;
  }

  @Nullable
  @ThreadAWT
  public Procedure2<ExplorerComponent, GenericNode> getRunWithReloadProcedure() {
    return null;
  }

  @NotNull
  public ItemHypercube getEncompassingHypercube() {
    return AlwaysReady.getEncompassingHypercube(this);
  }
}
