package com.almworks.spi.provider;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.InitializationState;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBResult;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.EnumerableAttribute;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.Role;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class MockContext implements ConnectionContext {
  public static final Role<MockContext> ROLE = Role.role("jiracontext");
  
  private static final EnumerableAttribute<InitializationState> INIT_STATE = EnumerableAttribute.create(Connection.NS.string("connectionInitState"), InitializationState.class);
  private static final AttributeLoader<String> LAST_ERROR = AttributeLoader.create(Connection.NS.string("connectionInitError"));
  
  private final AbstractConnection2<?> myConnection;
  private final SaveTransaction mySave = new SaveTransaction();
  private final BasicScalarModel<InitializationState> myInitializationState = BasicScalarModel.createWithValue(null, true);
  private volatile String myLastInitializationError;

  public MockContext(AbstractConnection2<?> connection) {
    myConnection = connection;
  }

  @NotNull
  @Override
  public ComponentContainer getContainer() {
    return myConnection.getContainer();
  }

  @Override
  public Connection getConnection() {
    return myConnection;
  }

  @Override
  public String getLastInitializationError() {
    return myLastInitializationError;
  }

  @Override
  public ScalarModel<InitializationState> getInitializationState() {
    return myInitializationState;
  }

  @Override
  public void setInitializationInProgress() {
    if (!myInitializationState.commitValue(InitializationState.NOT_INITIALIZED, InitializationState.INITIALIZING)) {
      myInitializationState.commitValue(InitializationState.REINITIALIZATION_REQUIRED,
        InitializationState.REINITIALIZING);
    }
    requestSave();
  }

  @Override
  public void setInitializationResult(boolean success, String error) {
    if (success) {
      myLastInitializationError = null;
      myInitializationState.setValue(InitializationState.INITIALIZED);
    } else {
      myLastInitializationError = error;
      if (!myInitializationState.commitValue(InitializationState.INITIALIZING, InitializationState.NOT_INITIALIZED))
        myInitializationState.commitValue(InitializationState.REINITIALIZING,
          InitializationState.REINITIALIZATION_REQUIRED);
    }
    requestSave();
  }

  @Override
  public void requestReinitialization() {
    myInitializationState.commitValue(InitializationState.INITIALIZED, InitializationState.REINITIALIZATION_REQUIRED);
    requestSave();
  }

  @Override
  public void stop() {
    myConnection.doStop();
  }

  private void requestSave() {
    mySave.request();
  }

  public void loadInitState(DBReader reader) {
    // todo kludge: we have to force class initialization before restorePersistable
    Log.debug("kludge:" + InitializationState.NOT_INITIALIZED);
    InitializationState state = INIT_STATE.getValue(reader, myConnection.getConnectionObj());
    if (state == null) {
      state = InitializationState.NOT_INITIALIZED;
    } else if (state == InitializationState.INITIALIZING) {
      // strange - we are just loaded
      state = InitializationState.NOT_INITIALIZED;
    } else if (state == InitializationState.REINITIALIZING) {
      // strange - we are just loaded
      state = InitializationState.REINITIALIZATION_REQUIRED;
    }
    myLastInitializationError = LAST_ERROR.getValue(reader, myConnection.getConnectionObj());
    myInitializationState.setValue(state);
  }

  private class SaveTransaction implements DownloadProcedure<DBDrain> {
    private final AtomicBoolean myRequested = new AtomicBoolean(false);

    public void request()  {
      if (!myRequested.compareAndSet(false, true)) return;
      myConnection.getActor(SyncManager.ROLE).writeDownloaded(this);
    }

    @Override
    public void write(DBDrain drain) throws DBOperationCancelledException {
      myRequested.set(false);
      ItemVersionCreator connection = drain.changeItem(myConnection.getConnectionObj());
      INIT_STATE.setValue(connection, myInitializationState.getValue());
      LAST_ERROR.setValue(connection, myLastInitializationError);
    }

    @Override
    public void onFinished(DBResult<?> result) {
    }
  }
}
