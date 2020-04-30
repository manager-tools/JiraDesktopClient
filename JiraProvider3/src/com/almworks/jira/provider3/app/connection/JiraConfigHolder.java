package com.almworks.jira.provider3.app.connection;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionViews;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.util.SimpleConnectionViews;
import com.almworks.api.http.FeedbackHandler;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.remotedata.issue.fields.JsonUserParser;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.User;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.login.AuthenticationRegister;
import com.almworks.spi.provider.util.BasicHttpAuthHandler;
import com.almworks.spi.provider.util.ServerSyncPoint;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class JiraConfigHolder {
  private final Object myLock = new Object();
  private final JiraConnection3 myConnection;
  private final BasicScalarModel<ReadonlyConfiguration> myConfiguration = BasicScalarModel.create(true);
  private final AtomicReference<ServerSyncPoint>  mySyncPoint = new AtomicReference<>(ServerSyncPoint.unsynchronized());
  private ConnectionDescriptor myDescriptor;
  private volatile long myLastAskLogin = 0;
  private final FeedbackHandler myFeedbackHandler;
  private final AtomicLong myConnectionItem = new AtomicLong(0);
  private final AtomicLong myConnectionUser = new AtomicLong(0);
  private final ProjectFilter myProjectFilter;
  private final SimpleConnectionViews myViews;

  private JiraConfigHolder(JiraConnection3 connection) {
    myConnection = connection;
    myFeedbackHandler = connection.getContainer().instantiate(BasicHttpAuthHandler.class);
    myProjectFilter = new ProjectFilter(connection.getConnectionObj());
    myViews = new SimpleConnectionViews(connection.getConnectionObj(), myProjectFilter.getModifiable(), connection.getDatabase(), Issue.STRUCTURE);
  }

  public static JiraConfigHolder create(JiraConnection3 connection, ReadonlyConfiguration configuration) throws ConfigurationException {
    JiraConfigHolder holder = new JiraConfigHolder(connection);
    holder.configure(configuration);
    return holder;
  }

  public FeedbackHandler getFeedbackHandler() {
    return myFeedbackHandler;
  }

  public JiraConnection3 getConnection() {
    return myConnection;
  }

  public void configure(ReadonlyConfiguration newConfiguration) throws ConfigurationException {
    assert newConfiguration != null;
    newConfiguration = ConfigurationUtil.copy(newConfiguration); // Fix configured config, avoid change of JDOM based config
    ReadonlyConfiguration oldConfiguration = myConfiguration.isContentKnown() ? myConfiguration.getValue() : null;
    synchronized (myLock) {
      myDescriptor = ConnectionDescriptor.create(newConfiguration, this);
    }
    myLastAskLogin = 0;
    myConfiguration.setValue(newConfiguration);
    boolean resetDownloadStatus = false;
    if (oldConfiguration != null) {
      if (JiraConfiguration.isSyncSettingEqual(oldConfiguration, newConfiguration)) {
        // Do nothing if changes don't affect sync. Such changes may happen due to updated cookies from the server.
        // And cookie update may happen frequently.
//        ThreadGate.LONG(myConnection).execute(() -> {
//          SyncParameters parameters = SyncParameters.downloadChanges(Collections.singleton(myConnection));
//          myConnection.getConnectionSynchronizer().synchronize(parameters);
//        });
        return;
      }
      resetDownloadStatus = !Objects.equals(JiraConfiguration.getAccountId(oldConfiguration), JiraConfiguration.getAccountId(newConfiguration));
      myConnection.clearSyncRegistry();
    }
    updateDBConnection(resetDownloadStatus, JiraConfiguration.getProjectsFilter(newConfiguration));
    if (resetDownloadStatus) myConnection.reloadMetaInfo();
  }

  /**
   * Updates current configuration and updates connection.
   * @param updater callback to perform actual modifications.<br>
   *                Returns true if something has been changed. Returns false if no changes are needed - throw away if any.
   */
  public void updateConfiguration(Predicate<Configuration> updater) {
    Configuration newConfig = ConfigurationUtil.copy(getActualConfiguration());
    if (!updater.test(newConfig)) return;
    myConnection.getActor(Engine.ROLE).getConnectionManager().updateConnection(myConnection, newConfig);
  }

  public ReadonlyConfiguration getActualConfiguration() {
    return myConnection.getConfiguration();
  }

  private void updateDBConnection(final boolean resetDowloadStatus, Set<Integer> projectsFilter) {
    final EntityTransaction transaction = myConnection.getServerInfo().createTransaction();
    projectsFilter = Util.NN(projectsFilter, Collections.emptySet());
    final List<EntityHolder> prjFilter = Collections15.arrayList(projectsFilter.size());
    for (Integer prjId : projectsFilter) {
      EntityHolder project = transaction.addEntity(ServerProject.TYPE, ServerProject.ID, prjId);
      if (project != null) prjFilter.add(project);
    }
    myConnection.getSyncManager().writeDownloaded(new DownloadProcedure<DBDrain>() {
      private long myUser = -1;
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        EntityWriter writer = ServerInfo.prepareWrite(drain, transaction);
        writer.write();
        ItemVersionCreator connection = drain.changeItem(writer.getConnectionItem());
        if (resetDowloadStatus) resetDownloadStatus(drain);
        myUser = updateConnectionUser(connection);
        myProjectFilter.update(drain, writer, prjFilter);
      }

      @Override
      public void onFinished(DBResult<?> result) {
        if (result.isSuccessful()) myConnectionUser.set(myUser);
      }
    }).finallyDoWithResult(ThreadGate.STRAIGHT, arg -> {
      if (!arg.isSuccessful())
        Log.error("Failed to update DB");
    });
  }

  private void resetDownloadStatus(DBDrain drain) {
    DBIdentifiedObject connectionObj = myConnection.getConnectionObj();
    LongArray fullyDownloaded = drain.getReader()
      .query(DPEqualsIdentified.create(SyncAttributes.CONNECTION, connectionObj).and(DPEquals.create(SyncAttributes.ITEM_DOWNLOAD_STAGE, ItemDownloadStage.FULL.getDbValue())))
      .copyItemsSorted();
    for (ItemVersionCreator item : drain.changeItems(fullyDownloaded))
      item.setValue(SyncAttributes.ITEM_DOWNLOAD_STAGE, ItemDownloadStage.QUICK.getDbValue());
    ServerSyncPoint syncPoint = ServerSyncPoint.unsynchronized();
    mySyncPoint.set(syncPoint);
    syncPoint.setValue(drain.changeItem(connectionObj));
  }

  private long updateConnectionUser(ItemVersionCreator connection) {
    JiraCredentials credentials = getJiraCredentials();
    if (credentials == null || credentials.isAnonymous()) return 0;
    String accountId = credentials.getAccountId();
    assert accountId != null && !accountId.isEmpty();
    long userItem = User.userByAccountId(myConnection.getConnectionObj(), accountId).findOrCreate(connection);
    ItemVersionCreator user = connection.changeItem(userItem);
    user.setValue(User.ACCOUNT_ID, accountId);
    user.setValue(User.NAME, credentials.getDisplayName());
    connection.setValue(Connection.USER, userItem);
    return userItem;
  }

  @Nullable
  public RestSession createSession() {
    ConnectionDescriptor descriptor;
    synchronized (myLock) {
      descriptor = myDescriptor;
    }
    if (descriptor == null) return null;
    return descriptor.createSession();
  }

  @Nullable
  public ConnectionDescriptor getConnectionDescriptor() {
    ConnectionDescriptor descriptor;
    synchronized (myLock) {
      descriptor = myDescriptor;
    }
    return descriptor;
  }

  public ScalarModel<ReadonlyConfiguration> getConfigurationModel() {
    return myConfiguration;
  }

  public ServerSyncPoint getSyncState() {
    return mySyncPoint.get();
  }

  public long initDB(DBDrain drain, final FireEventSupport<Procedure<Boolean>> finishListeners) {
    ItemVersionCreator connection = drain.changeItem(myConnection.getConnectionObj());
    if (connection == null) throw new DBException();
    final long user = updateConnectionUser(connection);
    ServerSyncPoint syncPoint = ServerSyncPoint.loadSyncPoint(connection);
    ServerSyncPoint current = mySyncPoint.get();
    mySyncPoint.compareAndSet(current, syncPoint);
    final long connectionItem = connection.getItem();
    myProjectFilter.initDB(drain, connectionItem);
    finishListeners.addStraightListener(arg -> {
      myConnectionItem.compareAndSet(0, connectionItem);
      myConnectionUser.compareAndSet(0, user);
    });
    return connectionItem;
  }

  public long getConnectionItem() {
    long item = myConnectionItem.get();
    if (item <= 0) Log.error("Connection item not initialized yet");
    return item;
  }

  public long getConnectionUser() {
    return myConnectionUser.get();
  }

  public String getBaseUrl() {
    synchronized (myLock) {
      return myDescriptor != null ? myDescriptor.getBaseUrl() : null;
    }
  }

  /**
   * Intended to be called before asking user to check/update credentials.
   * Remembers when was asked the last time, and blocks disturbing questions.
   * @return true if now is right time to ask, false - do not disturb user.
   */
  public boolean shouldAskReLogin() {
    long now = System.currentTimeMillis();
    if (now - myLastAskLogin < Const.HOUR) return false;
    myLastAskLogin = now;
    return true;
  }


  public void setSyncState(ServerSyncPoint syncPoint) {
    while (true) {
      ServerSyncPoint point = getSyncState();
      if (!point.isValidSuccessorState(syncPoint)) {
        Log.warn("bad successor sync state: " + point + " => " + syncPoint);
        return;
      }
      if (mySyncPoint.compareAndSet(point, syncPoint)) break;
    }
    requestUpdateDB();
  }

  private void requestUpdateDB() {
    myConnection.getSyncManager().writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        ItemVersionCreator connection = drain.changeItem(myConnection.getConnectionObj());
        mySyncPoint.get().setValue(connection);
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    });
  }

  /**
   * @return true if JIRA authentication data is configured
   */
  public boolean isAuthenticated() {
    synchronized (myLock) {
      return myDescriptor != null && !myDescriptor.getCredentials().isAnonymous();
    }
  }

  @Nullable
  public Entity getConnectionUserEntity() {
    JiraCredentials credentials = getJiraCredentials();
    if (credentials == null || credentials.isAnonymous()) return null;
    Entity entity = ServerUser.create(credentials.getAccountId());
    if (entity != null) entity.fix();
    return entity;
  }

  @Nullable
  public JsonUserParser.LoadedUser getConnectionLoadedUser() {
    JiraCredentials credentials = getJiraCredentials();
    if (credentials == null || credentials.isAnonymous()) return null;
    return new JsonUserParser.LoadedUser(credentials.getDisplayName(), credentials.getAccountId());
  }

  @Nullable("When not configured")
  public JiraCredentials getJiraCredentials() {
    ConnectionDescriptor descriptor = getConnectionDescriptor();
    return descriptor != null ? descriptor.getCredentials() : null;
  }

  public ConnectionViews getConnectionViews() {
    return myViews;
  }

  public LongList getCurrentProjects() {
    return myProjectFilter.getCurrentProjects();
  }

  public BoolExpr<DP> getProjectFilter() {
    return myProjectFilter.getFilter();
  }

  public HttpMaterialFactory getMaterialFactory() {
    return myConnection.getActor(HttpMaterialFactory.ROLE);
  }

  public AuthenticationRegister getAuthenticationRegister() {
    return myConnection.getActor(AuthenticationRegister.ROLE);
  }
}
