package com.almworks.spi.provider;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.SyncParameters;
import com.almworks.util.commons.Procedure2;

public class DefaultConnectionInitializer extends AbstractConnectionInitializer<MockContext> {
  private final AbstractConnectionSynchronizer mySynchronizer;
  private final Connection myConnection;

  public DefaultConnectionInitializer(AbstractConnection<?, MockContext> connection, AbstractConnectionSynchronizer synchronizer) {
    super(connection.getContext());
    myConnection = connection;
    mySynchronizer = synchronizer;
  }

  protected boolean startInitialization(Procedure2<Boolean, String> runOnFinish) {
    SyncParameters parameters = SyncParameters.initializeConnection(myConnection);
    return mySynchronizer.doSynchronize(parameters, runOnFinish);
  }

  protected AbstractSyncTask getSyncTask() {
    return mySynchronizer;
  }
}
