package com.almworks.explorer.tree;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.QueryUrlInfo;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBFilter;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
class ParentResult implements QueryResult {
  private final GenericNode myNode;

  public ParentResult(GenericNode node) {
    myNode = node;
  }

  @Override
  public boolean isRunnable() {
    return false;
  }

  @Override
  @Nullable
  public ItemSource getItemSource() {
    return null;
  }

  @Override
  @Nullable
  public ItemCollectionContext getCollectionContext() {
    return null;
  }

  @Override
  @Nullable
  @ThreadAWT
  public Procedure2<ExplorerComponent, GenericNode> getRunLocallyProcedure() {
    return null;
  }

  @Override
  @Nullable
  @ThreadAWT
  public Procedure2<ExplorerComponent, GenericNode> getRunWithReloadProcedure() {
    return null;
  }

  @Override
  @ThreadAWT
  public void getQueryURL(@NotNull @ThreadAWT Procedure<QueryUrlInfo> urlConsumer) {
    urlConsumer.invoke(null);
  }

  @Override
  @ThreadAWT
  public boolean hasQueryUrl() {
    return false;
  }

  @Override
  public boolean canRunNow() {
    return getParentResult().canRunNow();
  }

  @Override
  public Constraint getValidConstraint() {
    return getParentResult().getValidConstraint();
  }

  @Override
  public DBFilter getDbFilter() {
    return getParentResult().getDbFilter();
  }

  @Override
  @Nullable
  public ItemHypercube getHypercube(boolean precise) {
    return getParentResult().getHypercube(precise);
  }

  @Override
  public Detach addAWTChangeListener(ChangeListener listener) {
    return getParentResult().addAWTChangeListener(listener);
  }

  @Override
  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    getParentResult().addAWTChangeListener(life, listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ChangeListener listener) {
    getParentResult().addChangeListener(life, listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    getParentResult().addChangeListener(life, gate, listener);
  }

  @NotNull
  private QueryResult getParentResult() {
    GenericNode parent = myNode.getParent();
    return parent != null ? parent.getQueryResult() : NO_RESULT;
  }

  @Override
  public long getVersion() {
    return getParentResult().getVersion();
  }

  @Override
  @NotNull
  public ItemHypercube getEncompassingHypercube() {
    return AlwaysReady.getEncompassingHypercube(this);
  }
}
