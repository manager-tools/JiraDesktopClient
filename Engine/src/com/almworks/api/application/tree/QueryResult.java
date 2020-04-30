package com.almworks.api.application.tree;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.QueryUrlInfo;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.items.api.DBFilter;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
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
public interface QueryResult extends Modifiable {
  QueryResult NO_RESULT = new NoResult();

  @Nullable
  DBFilter getDbFilter();

  @Nullable
  @ThreadAWT
  Procedure2<ExplorerComponent, GenericNode> getRunLocallyProcedure();

  @Nullable
  @ThreadAWT
  Procedure2<ExplorerComponent, GenericNode> getRunWithReloadProcedure();

  /**
   * @return valid {@link Constraint} or null
   */
  @Nullable
  Constraint getValidConstraint();

  boolean isRunnable();

  @ThreadAWT
  boolean canRunNow();

  long getVersion();

  @ThreadAWT
  boolean hasQueryUrl();

  /**
   * Obtains query URL and invokes urlConsumer if url is obtained. If url cannot be obtained for any reason consumer is not called at all.
   */
  @ThreadAWT
  void getQueryURL(@NotNull @ThreadAWT Procedure<QueryUrlInfo> urlConsumer);

  @Nullable
  ItemHypercube getHypercube(boolean precise);

  @NotNull
  ItemHypercube getEncompassingHypercube();

  @Nullable
  ItemSource getItemSource();

  @Nullable
  ItemCollectionContext getCollectionContext();

  abstract class AlwaysReady implements QueryResult {
    public Detach addAWTChangeListener(ChangeListener listener) {
      return Detach.NOTHING;
    }

    public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void addChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    }

    public Constraint getValidConstraint() {
      return null;
    }

    public boolean isRunnable() {
      return true;
    }

    public boolean canRunNow() {
      return true;
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

    @ThreadAWT
    public void getQueryURL(@NotNull @ThreadAWT Procedure<QueryUrlInfo> urlConsumer) {
      urlConsumer.invoke(null);
    }

    @ThreadAWT
    public boolean hasQueryUrl() {
      return false;
    }

    @NotNull
    public ItemHypercube getEncompassingHypercube() {
      return AlwaysReady.getEncompassingHypercube(this);
    }

    public static ItemHypercube getEncompassingHypercube(QueryResult queryResult) {
      ItemHypercube hypercube = queryResult.getHypercube(false);
      if (hypercube == null) {
        assert false : queryResult;
        hypercube = new ItemHypercubeImpl();
      }
      return hypercube;
    }
  }


  public static final class NoResult implements QueryResult {
    private NoResult() {}

    public Constraint getValidConstraint() {
      return null;
    }

    public boolean isRunnable() {
      return false;
    }

    @Nullable
    public ItemSource getItemSource() {
      return null;
    }

    @Nullable
    public ItemCollectionContext getCollectionContext() {
      return null;
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

    @ThreadAWT
    public void getQueryURL(@NotNull @ThreadAWT Procedure<QueryUrlInfo> urlConsumer) {
      urlConsumer.invoke(null);
    }

    @ThreadAWT
    public boolean hasQueryUrl() {
      return false;
    }

    public void addChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    }

    public Detach addAWTChangeListener(ChangeListener listener) {
      return Detach.NOTHING;
    }

    public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    }

    public DBFilter getDbFilter() {
      return null;
    }

    public boolean canRunNow() {
      return false;
    }

    public long getVersion() {
      return 0;
    }

    @Nullable
    public ItemHypercube getHypercube(boolean precise) {
      return getEncompassingHypercube();
    }

    @NotNull
    public ItemHypercube getEncompassingHypercube() {
      return new ItemHypercubeImpl();
    }
  }
}
