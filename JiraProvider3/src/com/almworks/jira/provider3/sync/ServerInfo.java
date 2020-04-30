package com.almworks.jira.provider3.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.api.*;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.entities.dbwrite.downloadstage.DownloadStageMark;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.jira.connector2.JiraServerVersionInfo;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.schema.ConnectionProperties;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.jira.provider3.sync.download2.process.util.DownloadIssueUtil;
import com.almworks.jira.provider3.sync.download2.rest.RestOperations;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class ServerInfo {
  private static final TypedKey<String> CONNECTION_ID = TypedKey.create("connectionId");
  private static final TypedKey<DBIdentity> CONNECTION = TypedKey.create("connection");
  private static final TypedKey<Set<TypedKey<?>>> MESSAGES = TypedKey.create("errorMessages");
  private static final TypedKey<ServerInfo> SELF = TypedKey.create("serverInfo");
  
  private final JiraConnection3 myConnection;
  private final DBIdentifiedObject myConnectionObj;
  private final Object myLock = new Object();
  // guarded by myLock {
  @NotNull
  private ConnectionProperties myCurrent = ConnectionProperties.createEmpty();
  private ConnectionProperties myActual = null;
  // }

  public ServerInfo(JiraConnection3 connection) {
    myConnection = connection;
    myConnectionObj = Jira.createConnectionObject(connection.getConnectionID());
  }

  private void updateConnectionProperties(EntityTransaction transaction) {
    byte[] updateBytes;
    EntityHolder connection = changeConnection(transaction);
    boolean alreadyChanged = !ConnectionProperties.restore(connection).isEmpty();
    synchronized (myLock) {
      updateBytes = alreadyChanged || (myActual != null && !myCurrent.equals(myActual)) ? myCurrent.serialize() : null;
    }
    if (updateBytes != null) {
      if (alreadyChanged) connection.overrideValue(ConnectionProperties.ENTITY_KEY, updateBytes);
      else connection.setValue(ConnectionProperties.ENTITY_KEY, updateBytes);
    }
  }

  public EntityTransaction createTransaction() {
    EntityTransaction transaction = priCreateTransaction(myConnection.getConnectionID(), DBIdentity.fromDBObject(myConnectionObj));
    transaction.getUserData().putUserData(SELF, this);
    return transaction;
  }

  public static EntityTransaction priCreateTransaction(String connectionID, DBIdentity connectionIdentity) {
    EntityTransaction transaction = new EntityTransaction();
    DownloadStageMark.ensureInstalled(transaction, ServerIssue.TYPE);
    UserDataHolder data = transaction.getUserData();
    data.putUserData(CONNECTION_ID, connectionID);
    data.putUserData(CONNECTION, connectionIdentity);
    return transaction;
  }

  public void load(DBReader reader) {
    long connectionItem = reader.findMaterialized(myConnectionObj);
    if (connectionItem <= 0) return;
    ConnectionProperties actual = ConnectionProperties.load(reader, connectionItem);
    synchronized (myLock) {
      myActual = actual;
      myCurrent.updateMissingValuesFrom(actual);
    }
  }

  public void ensureHasData(RestSession session) throws ConnectorException {
    if (hasAllData(true)) return;
    ensureLoaded();
    if (hasAllData(false)) return;
    ensureDownloaded(session);
  }

  private volatile boolean myDownloaded = false;
  private void ensureDownloaded(RestSession session) throws ConnectorException {
    if (myDownloaded) return;
    final EntityTransaction transaction = createTransaction();
    load(session, transaction);
    EntityHolder connectionRo = changeConnection(transaction);
    myConnection.getSyncManager().writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        EntityWriter writer = DownloadIssueUtil.prepareWrite(transaction, drain);
        Collection<EntityHolder> problems = writer.getUncreatable();
        if (problems.isEmpty()) writer.write();
        else {
          LogHelper.error("Failed to write server info", problems);
          throw new DBOperationCancelledException();
        }
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    });
    ConnectionProperties properties = ConnectionProperties.restore(connectionRo);
    if (properties.getTimeZoneId() == null && (!properties.isEmpty() || myCurrent.getTimeZoneId() == null)) // properties are empty if loaded info is equal to current (do not warn in the case)
      LogHelper.warning("No server time zone on download, falling back to default", properties, myConnection.getConnectionID());
    myDownloaded = true;
  }

  public static void load(RestSession session, EntityTransaction transaction) throws ConnectorException {
    RestServerInfo serverInfo = RestServerInfo.get(session);
    TimeZone userTimeZone = RestOperations.getUserTimeZone(session);
    updateConnection(transaction, serverInfo.getVersion(), userTimeZone);
  }

  @SuppressWarnings("SimplifiableIfStatement")
  private boolean hasAllData(boolean checkActual) {
    synchronized (myLock) {
      if (!myCurrent.hasAllData()) return false;
      if (!checkActual) return true;
      return myActual != null && myActual.equals(myCurrent);
    }
  }

  public void ensureLoaded() {
    synchronized (myLock) {
      if (myActual != null && myCurrent.equals(myActual)) return;
    }
    myConnection.getSyncManager().enquireRead(DBPriority.FOREGROUND, new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        load(reader);
        return null;
      }
    }).waitForCompletion();
  }

  @NotNull
  public JiraConnection3 getConnection() {
    return myConnection;
  }

  @NotNull
  public SyncManager getSyncManager() {
    return myConnection.getSyncManager();
  }

  @NotNull
  public static String getConnectionId(EntityTransaction transaction) {
    return getMandatory(transaction, CONNECTION_ID, "missingConnectionId");
  }

  @Contract("_,_,!null -> !null")
  private static <T> T getMandatory(EntityTransaction transaction, TypedKey<T> key, T nullValue) {
    UserDataHolder data = transaction.getUserData();
    T value = data.getUserData(key);
    if (value == null) {
      Set<TypedKey<?>> messages = data.getUserData(MESSAGES);
      if (messages == null) {
        messages = Collections15.hashSet();
        data.putUserData(MESSAGES, messages);
      }
      if (!messages.contains(key)) {
        LogHelper.error("Missing value", key);
        messages.add(key);
      }
      return nullValue;
    }
    return value;
  }

  @Nullable
  public static DBIdentity getConnection(EntityTransaction transaction) {
    //noinspection ConstantConditions
    return getMandatory(transaction, CONNECTION, null);
  }

  public static EntityHolder changeConnection(EntityTransaction transaction) {
    DBIdentity connection = getConnection(transaction);
    return connection != null ? transaction.addIdentifiedObject(connection) : null;
  }

  public DBIdentifiedObject getConnectionObj() {
    return myConnectionObj;
  }

  public static EntityWriter prepareWrite(DBDrain drain, EntityTransaction transaction) {
    DBIdentity connection = getConnection(transaction);
    return transaction.prepareWrite(drain, ServerJira.NS, connection);
  }
  
  public static void updateConnection(EntityTransaction transaction, JiraServerVersionInfo version, TimeZone tz) {
    ServerInfo self = getSelf(transaction);
    if (self == null || (version == null && tz == null)) return;
    synchronized (self.myLock) {
      if (tz != null) self.myCurrent.setTimeZone(tz);
      if (version != null) self.myCurrent.setJiraVersion(version);
      if (tz != null) LogHelper.debug(self.myConnection.getConnectionID(), "server time zone set to", tz.getID());
      self.updateConnectionProperties(transaction);
    }
  }

  public static void updateSearchableCustomFields(EntityTransaction transaction, List<String> fieldIds) {
    ServerInfo self = getSelf(transaction);
    if (self == null || fieldIds == null) return;
    synchronized (self.myLock) {
      self.myCurrent.setSearchableCustomFields(fieldIds);
      self.updateConnectionProperties(transaction);
    }
  }

  @Nullable
  private static ServerInfo getSelf(EntityTransaction transaction) {
    ServerInfo self = transaction.getUserData().getUserData(SELF);
    LogHelper.assertError(self != null, "Wrong transaction");
    return self;
  }
}
