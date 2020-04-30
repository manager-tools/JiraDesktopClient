package com.almworks.api.engine;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.download.DownloadOwner;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.engine.gui.attachments.Attachment;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.DBNamespace;
import com.almworks.spi.provider.ConnectionContext;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.SetHolder;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

/**
 * @author sereda
 */
public interface Connection {
  DBNamespace NS = Engine.NS.subNs("connection");
  DBAttribute<Long> USER = NS.link("user", "User", false);

  ReadonlyConfiguration getConfiguration();

  void update(ReadonlyConfiguration configuration) throws ConfigurationException;

  ConnectionSynchronizer getConnectionSynchronizer();

  public ConnectionContext getContext();

  ItemProvider getProvider();

  ConnectionViews getViews();

  // todo remove this method - but provide a way to connect bugzilla gui classes to bugzilla provider
  // todo :refactoring: observation: returned container delegates to getContext().getContainer(); remove that also?
  ComponentContainer getConnectionContainer();

  UIComponentWrapper getConnectionStateComponent();

  SetHolder<SyncTask> getSyncTasks();

  ScalarModel<InitializationState> getInitializationState();

  void requestReinitialization();

  /**
   * Call only for READY connection.
   */
  long getConnectionItem();

  /**
   * @return item of this connection type (connection factory)
   */
  long getProviderItem();

  String getConnectionID();

  public boolean isUploadAllowed();

  @NotNull
  BasicScalarModel<AutoSyncMode> getAutoSyncMode();

  @Nullable
  CollectionModel<RemoteQuery> getRemoteQueries();

  @Nullable
  ScalarModel<Collection<RemoteQuery2>> getRemoteQueries2();

  /** Removes connection and clears all connection items.
   * After corresponding write transaction finishes successfully, runs the callback in AWT. */
  void removeConnection(@NotNull Runnable onConnectionItemsCleared);

  void startConnection();

  void stopConnection();

  /**
   * In state {@link ConnectionState#READY} connection item (see {@link #getConnectionItem()}) must be materialized.
   */
  ScalarModel<ConnectionState> getState();

  SyncTask synchronizeItemView(Constraint validRemoteQueryConstraint, @Nullable DBFilter view,
    @Nullable LongList localResult, String queryName, Procedure<SyncTask> runFinally);

  void uploadItems(LongList items);

  @ThreadAWT
  @Nullable
  SyncTask downloadItemDetails(LongList items);

  @Nullable
  String buildDefaultQueriesXML();

  @Nullable
  @CanBlock
  QueryUrlInfo getQueryURL(Constraint constraint, DBReader reader) throws InterruptedException;

  @ThreadAWT
  @NotNull
  Pair<DBFilter, Constraint> getViewAndConstraintForUrl(String url) throws CantPerformExceptionExplained;

  /**
   * @return ItemSource that delivers items with URLs from the specified Iterable. If any of the URLs is not recognized by this connection, this method returns null.
   * If connection is not ready, returns null.
   * */
  @ThreadAWT
  @Nullable
  ItemSource getItemSourceForUrls(Iterable<String> urls);

  @Nullable
  @CanBlock
  String getItemUrl(ItemVersion localVersion);

  @ThreadAWT
  boolean isItemUrl(String itemUrl);

  @ThreadAWT @Nullable
  String getItemIdForUrl(String itemUrl);

  @Deprecated
  @CanBlock
  @Nullable
  Date getItemTimestamp(ItemVersion version);

  @Deprecated
  @CanBlock
  @Nullable
  String getItemShortDescription(ItemVersion version);

  @Deprecated
  @CanBlock
  @Nullable
  String getItemLongDescription(ItemVersion version);

  @Deprecated
  @CanBlock
  @Nullable
  String getItemId(ItemVersion version);

  @Deprecated
  @CanBlock
  @Nullable
  String getItemSummary(ItemVersion version);

  @Nullable
  @CanBlock
  String getExternalIdSummaryString(ItemVersion version);

  @Nullable
  @CanBlock
  String getExternalIdString(ItemVersion version);

  @Nullable
  @ThreadSafe
  String getDisplayableItemId(ItemWrapper wrapper);

  /**
   * @return loaded attachments for the specified primary item
   * @deprecated in JC3 all meaningful work is done by own attachment model key
   */
  @CanBlock
  @NotNull
  @Deprecated
  Collection<? extends Attachment> getItemAttachments(ItemVersion primaryItem);

  /**
   * Main model of columns applicable to this Connection. Does not contain auxiliary columns (distinction of main and auxiliary columns is implementation-specific).
   * To get all columns, unite the model returned by this method with the model returned by {@link #getAuxiliaryColumns()}.
   * Should return the same instance.
   */
  @ThreadAWT
  AListModel<? extends TableColumnAccessor<LoadedItem, ?>> getMainColumns();

  /**
   * Comparator for columns returned by {@link #getMainColumns}
   */
  @NotNull
  Comparator<? super TableColumnAccessor<LoadedItem, ?>> getMainColumnsOrder();

  /**
   * Auxiliary columns applicable to this Connection. Which columns are auxiliary is specific to the implementation.
   * To get all columns, unite the model returned by this method with the model returned by {@link #getMainColumns()}.
   * Should return the same instance.
   * @return null means "implementation does not contemplate any auxiliary columns at all"
   */
  @ThreadAWT
  @Nullable("implementation does not contemplate any auxiliary columns at all")
  AListModel<? extends TableColumnAccessor<LoadedItem, ?>> getAuxiliaryColumns();

  /**
   * Comparator for columns returned by {@link #getAuxiliaryColumns}
   */
  @Nullable("if #getAuxiliaryColumns is null")
  Comparator<? super TableColumnAccessor<LoadedItem, ?>> getAuxColumnsOrder();

  @NotNull
  AListModel<? extends ConstraintDescriptor> getDescriptors();

  boolean matchAllWords(long item, char[][] charWords, String[] stringWords, DBReader reader);

  @Nullable
  @ThreadSafe
  ConstraintDescriptor getDescriptorByIdSafe(String id);

  /**
   * Adjusts the specified hypercube to additional connection limitations (such as products/projects for which this connection is configured).
   */
  @ThreadAWT
  ItemHypercube adjustHypercube(@NotNull ItemHypercube hypercube);

  /**
   * Adjusts the specified constraint to additional connection limitations (such as products/projects for which this connection is configured).
   */
  @ThreadAWT
  Constraint adjustConstraint(@NotNull Constraint constraint);

  /**
   * Adjusts the specified items view to additional connection limitations (such as products/projects for which this connection is configured).
   * @param view readonly
   * @param life span in which there is sense in calling cont, when it's ended, no calls will be made
   * @param cont when view is adjusted, receives it
   */
  @ThreadAWT
  void adjustView(@NotNull DBFilter view, Lifespan life, @NotNull Procedure<DBFilter> cont);

  @NotNull
  Configuration getConnectionConfig(String... subsets);

  DBIdentifiedObject getConnectionObj();

  @NotNull
  DownloadOwner getDownloadOwner();

  <T> T getActor(Role<T> role);

  enum AutoSyncMode {
    AUTOMATIC("AUTOMATIC"),
    MANUAL("MANUAL");

    private final String myId;

    AutoSyncMode(String id) {
      myId = id;
    }

    public String getId() {
      return myId;
    }

    public static AutoSyncMode forId(String id) {
      if (AUTOMATIC.getId().equals(id)) return AUTOMATIC;
      if (MANUAL.getId().equals(id)) return MANUAL;
      LogHelper.warning("Unknown mode", id);
      return AUTOMATIC;
    }
  }
}