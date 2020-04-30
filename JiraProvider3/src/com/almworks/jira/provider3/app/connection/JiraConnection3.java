package com.almworks.jira.provider3.app.connection;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.order.Order;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldSubsetConstraint;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.download.DownloadOwner;
import com.almworks.api.engine.*;
import com.almworks.api.engine.util.MatchAllHelper;
import com.almworks.api.explorer.AdditionalHierarchies;
import com.almworks.api.store.Store;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.ConnectionModels;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.OrdersCollector;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.app.sync.JiraSynchronizer;
import com.almworks.jira.provider3.app.sync.QueryIssuesUtil;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.links.structure.ui.JiraComplexHierarchies;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.schema.*;
import com.almworks.jira.provider3.services.IssueUrl;
import com.almworks.jira.provider3.services.upload.queue.UploadQueue;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.spi.provider.AbstractConnection2;
import com.almworks.spi.provider.AbstractConnectionInitializer;
import com.almworks.spi.provider.DefaultConnectionInitializer;
import com.almworks.spi.provider.util.ServerSyncPoint;
import com.almworks.tags.TagsComponentImpl;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Lazy;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.UIComponentWrapper;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class JiraConnection3 extends AbstractConnection2<JiraProvider3> implements Connection {
  public static final Role<JiraConnection3> ROLE = Role.role(JiraConnection3.class);
  @NotNull
  private final JiraConfigHolder myConfigHolder;
  private final DetachComposite myLife = new DetachComposite();
  private final ConnectionModels myModels;
  private final Lazy<UIComponentWrapper> myInformationPanel = new Lazy<UIComponentWrapper>() {
    @NotNull
    public UIComponentWrapper instantiate() {
      return JiraConnectionPanel.getLazyWrapper(myContainer);
    }
  };
  private final UploadQueue myUploadQueue;
  private final ConnectorManager myConnectors;

  private final DownloadOwner myDownloadOwner;
  private final JiraUrlsSupport myUrlsSupport;
  private final MyReordersModel myReorders;
  private final ServerInfo myServerInfo;
  private final CustomFieldsComponent myCustomFields;

  public JiraConnection3(JiraProvider3 provider, MutableComponentContainer container, String connectionID, ReadonlyConfiguration configuration, Store store,
    CustomFieldsComponent customFields) throws ConfigurationException
  {

    super(provider, container, connectionID);
    myCustomFields = customFields;
    myServerInfo = new ServerInfo(this);
    myContainer.registerActor(ROLE, this);
    myContainer.registerActorClass(ConnectionSynchronizer.ROLE, JiraSynchronizer.class);
    myContainer.registerActorClass(AbstractConnectionInitializer.ROLE, DefaultConnectionInitializer.class);
    myContainer.registerActorClass(IssuePermissions.ROLE, IssuePermissions.class);
    myContainer.registerActorClass(AdditionalHierarchies.ROLE, JiraComplexHierarchies.class);
    myConfigHolder = JiraConfigHolder.create(this, configuration);
    myConnectors = new ConnectorManager(this);
    myConnectors.addServerCheck(new ServerVersionCheck(store.getSubStore("vs")));
    myModels = ConnectionModels.create(myLife, getGuiFeatures(), getConnectionObj(), Jira.JIRA_PROVIDER_ID, TagsComponentImpl.OWNER);
    myUploadQueue = new UploadQueue(this);
    myUrlsSupport = new JiraUrlsSupport(this);
    myReorders = new MyReordersModel(this);
    myDownloadOwner = new JiraDownloadOwner(this);
  }

  public ServerInfo getServerInfo() {
    return myServerInfo;
  }

  public CustomFieldsComponent getCustomFields() {
    return myCustomFields;
  }

  @Override
  public boolean isUploadAllowed() {
    return myConfigHolder.isAuthenticated();
  }

  @Override
  public String getItemUrl(ItemVersion localVersion) {
    String url = myUrlsSupport.getItemUrl(localVersion);
    return url != null ? url : super.getItemUrl(localVersion);
  }

  public boolean isItemUrl(String itemUrl) {
    return myUrlsSupport.isItemUrl(itemUrl);
  }

  @Override
  public String getItemIdForUrl(String itemUrl) {
    IssueUrl issueUrl = IssueUrl.parseUrl(itemUrl);
    return issueUrl != null ? issueUrl.getKey() : null;
  }

  @Override
  public QueryUrlInfo getQueryURL(Constraint constraint, DBReader reader) {
    return JqlQueryBuilder.buildQueryInfo(reader, constraint, this);
  }

  @Override
  public ItemSource getItemSourceForUrls(Iterable<String> urls) {
    return myUrlsSupport.getItemSourceForUrls(urls);
  }

  @NotNull
  @Override
  protected AListModel<? extends ConstraintDescriptor> createDescriptorsModel() {
    return myModels.getDescriptors();
  }

  @Override
  public AListModel<? extends TableColumnAccessor<LoadedItem, ?>> getMainColumns() {
    return myModels.getMainColumns();
  }

  @Override
  public ReadonlyConfiguration getConfiguration() {
    ScalarModel<ReadonlyConfiguration> model = getConfigurationModel();
    if (!model.isContentKnown()) {
      Log.error("Config model content is not ready");
      return null;
    }
    ReadonlyConfiguration configuration = model.getValue();
    assert configuration != null;
    return configuration;
  }

  @NotNull
  public JiraConfigHolder getConfigHolder() {
    return myConfigHolder;
  }

  @Override
  public void update(ReadonlyConfiguration configuration) throws ConfigurationException {
    myConfigHolder.configure(configuration);
  }

  @Override
  public ConnectionViews getViews() {
    return myConfigHolder.getConnectionViews();
  }

  @Override
  public UIComponentWrapper getConnectionStateComponent() {
    return myInformationPanel.get();
  }

  @Override
  public long getConnectionItem() {
    return myConfigHolder.getConnectionItem();
  }

  @Override
  public long getProviderItem() {
    return myProvider.getProviderItem();
  }

  public long getConnectionUser() {
    return myConfigHolder.getConnectionUser();
  }

  @Override
  public SyncTask synchronizeItemView(Constraint validRemoteQueryConstraint, @Nullable DBFilter view,
    @Nullable LongList localResult, String queryName, Procedure<SyncTask> runFinally)
  {
    return QueryIssuesUtil.synchronizeItemView(this, validRemoteQueryConstraint, view, localResult, queryName, runFinally);
  }

  @Override
  public void uploadItems(LongList items) {
    myUploadQueue.addToUpload(items);
  }

  @Override
  public SyncTask downloadItemDetails(LongList items) {
    ConnectionSynchronizer task = getActor(ConnectionSynchronizer.ROLE);
    task.synchronize(SyncParameters.syncItems(items.toList(), SyncType.RECEIVE_FULL));
    return task;
  }

  @Override
  public String buildDefaultQueriesXML() {
    return JiraDefaultQueriesBuilder.buildDefaultQueriesXML(this);
  }

  @Override
  public Date getItemTimestamp(ItemVersion version) {
    assert false;
    return null;
  }

  @Override
  public String getItemId(ItemVersion localVersion) {
    return localVersion.getValue(Issue.KEY);
  }

  @Override
  public String getItemSummary(ItemVersion version) {
    return version.getValue(Issue.SUMMARY);
  }

  @Override
  public String getItemShortDescription(ItemVersion version) {
    assert false;
    return null;
  }

  @Override
  public String getItemLongDescription(ItemVersion version) {
    assert false;
    return null;
  }

  @Override
  public String getDisplayableItemId(ItemWrapper wrapper) {
    return LoadedIssueUtil.getIssueKey(wrapper);
  }

  @Override
  public boolean matchAllWords(long issueItem, char[][] charWords, String[] stringWords, DBReader reader) {
    ItemVersion issue = SyncUtils.readTrunk(reader, issueItem);
    if (charWords.length == 0) return true;
    MatchAllHelper helper = new MatchAllHelper(charWords);
    if (helper.matchAttr(issue, Issue.SUMMARY)) return true;
    if (helper.matchAttr(issue, Issue.ENVIRONMENT)) return true;
    if (helper.matchHumanText(issue, Issue.DESCRIPTION)) return true;
    LongArray fields = CustomField.queryKnownKey(reader, getConnectionItem());
    List<String> keys = CustomField.KEY.collectValues(fields, reader);
    for (int i = 0; i < fields.size(); i++) {
      String key = keys.get(i);
      if (key == null) continue;
      DBAttribute<String> cfAttr = BadUtil.readScalarAttribute(String.class, reader, fields.get(i), CustomField.ATTRIBUTE);
      if (cfAttr == null) continue;
      if (helper.matchAttr(issue, cfAttr)) return true;
    }
    LongArray comments = issue.getSlaves(Comment.ISSUE);
    for (LongIterator comment : comments) {
      if (helper.matchHumanText(BranchSource.trunk(reader).forItem(comment.value()), Comment.TEXT)) return true;
    }
    return false;
  }

  @Override
  public ItemHypercube adjustHypercube(@NotNull ItemHypercube hypercube) {
    LongList projects = myConfigHolder.getCurrentProjects();
    return ItemHypercubeUtils.ensureValuesIncludedForAxis(hypercube, Issue.PROJECT, projects);
  }

  @Override
  public Constraint adjustConstraint(@NotNull Constraint constraint) {
    LongList projects = myConfigHolder.getCurrentProjects();
    return projects.isEmpty() ? constraint : CompositeConstraint.Simple
      .and(constraint, FieldSubsetConstraint.Simple.intersection(Issue.PROJECT, CollectionUtil.collectLongs(projects)));
  }

  @Override
  public void adjustView(@NotNull final DBFilter view, Lifespan life, @NotNull final Procedure<DBFilter> cont) {
    cont.invoke(view.filter(myConfigHolder.getProjectFilter()));
  }

  protected void longStart() throws InterruptedException {
    super.longStart();
    myServerInfo.ensureLoaded();
    myReorders.start(myLife);
    ConnectionSynchronizer synchronizer = myContainer.getActor(ConnectionSynchronizer.ROLE);
    if (synchronizer == null) {
      assert false;
    } else {
      ((JiraSynchronizer) synchronizer).longStart();
    }
    ((JiraComplexHierarchies) getActor(JiraComplexHierarchies.ROLE)).start();
  }

  public ScalarModel<ReadonlyConfiguration> getConfigurationModel() {
    return myConfigHolder.getConfigurationModel();
  }

  public ConnectorManager getIntegration() {
    return myConnectors;
  }

  public void reloadMetaInfo() {
    ThreadGate.LONG(this).execute(() -> {
      SyncParameters parameters = SyncParameters.initializeConnection(JiraConnection3.this);
      getConnectionSynchronizer().synchronize(parameters);
    });
  }

  @Nullable
  public static JiraConnection3 getInstance(LoadedItemServices lis) {
    return lis != null ? Util.castNullable(JiraConnection3.class, lis.getConnection()) : null;
  }

  @Override
  public DBIdentifiedObject getConnectionObj() {
    return myServerInfo.getConnectionObj();
  }

  public Pair<String, Boolean> getCredentialState() {
    JiraCredentials credentials = myConfigHolder.getJiraCredentials();
    if (credentials == null || credentials.isAnonymous()) return Pair.create(null, false);
    String displayName = credentials.getDisplayName();
    return Pair.create(displayName, true);
  }

  public ServerSyncPoint getSyncState() {
    return myConfigHolder.getSyncState();
  }

  /**
   * @see com.almworks.jira.provider3.app.connection.JiraConfiguration#getProjectsFilter(ReadonlyConfiguration)
   */
  @Nullable
  public Set<Integer> getProjectsFilter() {
    return JiraConfiguration.getProjectsFilter(getConfiguration());
  }

  public void updateSyncState(ServerSyncPoint syncPoint) {
    myConfigHolder.setSyncState(syncPoint);
  }

  public void doStop() {
    myUploadQueue.stop();
    myLife.detach();
  }

  public GuiFeaturesManager getGuiFeatures() {
    return myProvider.getFeaturesManager();
  }

  @Override
  protected void doInitDB(DBDrain drain, FireEventSupport<Procedure<Boolean>> finishListeners) {
    long connection = myConfigHolder.initDB(drain, finishListeners);
    Resolution.initDB(drain, connection);
    ConnectionProperties.migrateFrom3_0_1(drain, connection);
    getActor(IssuePermissions.ROLE).load(drain);
  }

  @NotNull
  public DownloadOwner getDownloadOwner() {
    return myDownloadOwner;
  }

  public AListModel<? extends Order> getReordersModel() {
    return myReorders.getModel();
  }


  private static class MyReordersModel extends Condition<OrdersCollector.LoadedReorder> implements Runnable {
    private final FilteringListDecorator<OrdersCollector.LoadedReorder> myReorders;
    private final JiraConnection3 myConnection;

    public MyReordersModel(JiraConnection3 connection) {
      myConnection = connection;
      myReorders = FilteringListDecorator.createNotStarted(connection.getGuiFeatures().getReorders());
    }

    @Override
    public boolean isAccepted(OrdersCollector.LoadedReorder reorder) {
      return reorder != null && reorder.applicable(myConnection);
    }

    public void start(Lifespan life) {
      life.add(myReorders.getDetach());
      ThreadGate.AWT.execute(this);
    }

    @Override
    public void run() {
      myReorders.setFilter(this);
    }

    public AListModel<? extends Order> getModel() {
      return myReorders;
    }
  }
}
