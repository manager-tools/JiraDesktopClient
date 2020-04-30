package com.almworks.engine;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.engine.*;
import com.almworks.api.platform.ProductInformation;
import com.almworks.util.Env;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;

class PeriodicalSynchronization implements Startable {
  public static final Role<PeriodicalSynchronization> ROLE = Role.role("autosync");
  private static final long CHECK_DELAY = 10 * Const.SECOND;

  private static final String QUICK_PREFIX = "quick:";
  private static final String VERSION_PREFIX = "version:";

  private static long getFailurePause() {
    warnObsoleteParameter("auto.sync.period.failed");
    int p = Env.getInteger("full.sync.period.failed", 1, 3600, -1);
    if (p == -1)
      p = Env.getInteger("full.sync.period", 1, 3600, 720);
    return p * Const.MINUTE;
  }

  private static long getSuccessPause() {
    warnObsoleteParameter("auto.sync.period");
    return Env.getInteger("full.sync.period", 1, 360000, 720) * Const.MINUTE;
  }

  private static long getQuickPause() {
    return Env.getInteger("quick.sync.period", 10, 360000, 150) * Const.SECOND;
  }

  private static void warnObsoleteParameter(String parameter) {
    if (Env.getString(parameter) != null) {
      Log.warn("parameter " + parameter + " is not used anymore");
    }
  }

  private final Configuration myConfiguration;
  private final ApplicationLoadStatus myApplicationLoadStatus;
  private final ProductInformation myProductInformation;

  private final Engine myEngine;
  private final SynchronizedBoolean myRunning = new SynchronizedBoolean(false);
  private final Timer myTimer;

  private Map<String, Long> myLastNewVersionFullSyncAttempts;

  public PeriodicalSynchronization(Engine engine, Configuration configuration,
    ApplicationLoadStatus applicationLoadStatus, ProductInformation productInformation)
  {
    myEngine = engine;
    myConfiguration = configuration;
    myApplicationLoadStatus = applicationLoadStatus;
    myProductInformation = productInformation;

    final ThreadGate gate = ThreadGate.LONG(this);

    class DoJob implements Runnable, ActionListener {
      @Override
      public void actionPerformed(ActionEvent e) {
        gate.execute(this);
      }
      @Override
      public void run() {
        doJob();
      }
    }
    myTimer = new Timer((int) CHECK_DELAY, new DoJob());

    ModelUtils.whenTrue(
      myApplicationLoadStatus.getApplicationLoadedModel(), gate,
      new Runnable() {
        @Override
        public void run() {
          doJob();
          ThreadGate.AWT.execute(new Runnable() {
            @Override
            public void run() {
              myTimer.start();
            }
          });
        }
      });
  }

  public void start() {}

  public void stop() {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        myTimer.stop();
      }
    });
  }

  @Nullable
  private SyncParameters getConnectionSyncParameters(Connection connection, final long currentTime) {
    ScalarModel<SyncTask.State> syncStateModel = connection.getConnectionSynchronizer().getState();
    SyncTask.State syncState = syncStateModel.getValue();
    if (syncState == null || syncState == SyncTask.State.WORKING || syncState == SyncTask.State.SUSPENDED) {
      return null;
    }

    String connectionID = connection.getConnectionID();
    String lastVersion = myConfiguration.getSetting(VERSION_PREFIX + connectionID, null);
    if (lastVersion == null) {
      markConnectionFullSyncSuccess(connectionID);
      return null;
    }
    String thisVersion = myProductInformation.getVersion();
    long nextFullSync = 0;
    long nextQuickSync = 0;

    if(Util.equals(lastVersion, thisVersion)) {
      nextFullSync = myConfiguration.getLongSetting(connectionID, -1);
      if (nextFullSync < 0) {
        markConnectionFullSyncSuccess(connectionID); // New connection is just initialized. Mark it for next full sync
        nextFullSync = myConfiguration.getLongSetting(connectionID, 0);
      }
      nextQuickSync = myConfiguration.getLongSetting(QUICK_PREFIX + connectionID, 0);
      long maxFuture = Math.max(getSuccessPause(), getFailurePause());
      if (nextFullSync - currentTime > maxFuture) {
        nextFullSync = 0;
      }
      if (nextQuickSync - currentTime > maxFuture) {
        nextQuickSync = 0;
      }
    } else if(isTooEarlyForNextNewVersionFullSyncAttempt(connectionID, currentTime)) {
      return null;
    }

    if (currentTime < nextFullSync && currentTime < nextQuickSync) {
      return null;
    }

    boolean quick = currentTime < nextFullSync;
    SyncParameters parameters = SyncParameters.synchronizeConnection(connection, SyncType.RECEIVE_ONLY);
    parameters = parameters.merge(quick ? SyncParameters.downloadChanges() : SyncParameters.downloadChangesAndMeta());

    if (quick) {
      myConfiguration.setSetting(QUICK_PREFIX + connectionID, currentTime + getQuickPause());
      Log.debug(this + ": sync " + connectionID + " [Q]");
    } else {
      // set success pause only on successful sync
      myConfiguration.setSetting(connectionID, currentTime + getFailurePause());
      myConfiguration.setSetting(QUICK_PREFIX + connectionID, currentTime + getQuickPause());
      Log.debug(this + ": sync " + connectionID);
      trackSyncProgressAndSetNextFullSyncTime(syncStateModel, connectionID);
    }

    return parameters;
  }

  private boolean isTooEarlyForNextNewVersionFullSyncAttempt(String connectionID, long currentTime) {
    if(myLastNewVersionFullSyncAttempts == null) {
      myLastNewVersionFullSyncAttempts = Collections15.hashMap();
    }

    final long lastAttempt = Util.NN(myLastNewVersionFullSyncAttempts.get(connectionID), 0L);
    if(currentTime - lastAttempt < getQuickPause()) {
      return true;
    }

    myLastNewVersionFullSyncAttempts.put(connectionID, currentTime);
    return false;
  }

  private void trackSyncProgressAndSetNextFullSyncTime(ScalarModel<SyncTask.State> syncStateModel, final String connectionID) {
    final int progress[] = {0};
    final DetachComposite detach = new DetachComposite(true);
    detach.add(syncStateModel.getEventSource().addStraightListener(new ScalarModel.Adapter<SyncTask.State>() {
      public void onScalarChanged(ScalarModelEvent<SyncTask.State> event) {
        SyncTask.State state = event.getNewValue();
        if (progress[0] == 0 && state == SyncTask.State.WORKING) {
          progress[0] = 1;
        } else if (progress[0] == 1) {
          if (state == SyncTask.State.DONE) {
            progress[0] = 2;
            detach.detach();
            long successNextTime = markConnectionFullSyncSuccess(connectionID);
            Log.debug(this + " autosynced " + connectionID + " (S:" + successNextTime + ")");
          } else if (state != SyncTask.State.WORKING && state != SyncTask.State.SUSPENDED) {
            progress[0] = 2;
            detach.detach();
            Log.debug(this + " autosync failed");
          }
        }
      }
    }));
  }

  private long markConnectionFullSyncSuccess(String connectionID) {
    long currentTime = System.currentTimeMillis();
    long nextFullSync = currentTime + getSuccessPause();
    myConfiguration.setSetting(connectionID, nextFullSync);
    myConfiguration.setSetting(VERSION_PREFIX + connectionID, myProductInformation.getVersion());
    myConfiguration.setSetting(QUICK_PREFIX + connectionID, currentTime + getQuickPause());
    return nextFullSync;
  }

  private synchronized void doJob() {
    if (!Boolean.TRUE.equals(myApplicationLoadStatus.getApplicationLoadedModel().getValue())) {
      return;
    }

    if (!myRunning.commit(false, true)) {
      return;
    }

    try {
      long currentTime = System.currentTimeMillis();
      Collection<Connection> connections = myEngine.getConnectionManager().getReadyConnectionsModel().copyCurrent();
      for (Connection connection : connections) {
        ConnectionState state = connection.getState().getValue();
        if (state == null || !state.isReady()) {
          continue;
        }

        InitializationState initializationState = connection.getInitializationState().getValue();
        if (initializationState == null || !initializationState.isInitialized()) {
          continue;
        }

        if(connection.getAutoSyncMode().getValue() != Connection.AutoSyncMode.AUTOMATIC) {
          continue;
        }

        SyncParameters parameters = getConnectionSyncParameters(connection, currentTime);
        if (parameters != null) {
          assert !parameters.isEmpty() : parameters;
          myEngine.getSynchronizer().synchronize(parameters);
        }
      }
    } finally {
      myRunning.set(false);
    }
  }

  public String toString() {
    return "PSYNC";
  }
}
