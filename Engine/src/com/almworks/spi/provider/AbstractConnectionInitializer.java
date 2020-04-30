package com.almworks.spi.provider;

import com.almworks.api.engine.ConnectionState;
import com.almworks.api.engine.InitializationState;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Bottleneck;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.picocontainer.Startable;

public abstract class AbstractConnectionInitializer<C extends ConnectionContext> implements Startable {
  public static final Role<AbstractConnectionInitializer> ROLE = Role.role(AbstractConnectionInitializer.class);
  protected static final long PAUSE_BETWEEN_FAILED_INITIALIZATIONS = 60 * 1000;

  protected final C myContext;
  private final DetachComposite myDetach = new DetachComposite();

  private final Bottleneck myCheckState = new Bottleneck(1000, ThreadGate.LONG(
    this), new Runnable() {
      public void run() {
        checkState();
      }
    });

  protected final Bottleneck myInitialize = new Bottleneck(1000, ThreadGate.LONG(
    new Object()), new Runnable() {
    public void run() {
      initialize();
    }
  });

  protected AbstractConnectionInitializer(C context) {
    myContext = context;
    myInitialize.delay();
  }

  public void start() {
    myDetach.add(myContext.getInitializationState().getEventSource().addStraightListener(new ScalarModel.Adapter<InitializationState>() {
      public void onScalarChanged(ScalarModelEvent<InitializationState> event) {
        if (event.getNewValue().isInitializing())
          return;
        myCheckState.requestDelayed();
      }
    }));
  }

  private void checkState() {
    ConnectionState connectionState = myContext.getConnection().getState().getValue();
    if (connectionState == null || !connectionState.isReady()) {
      // wait until connection is ready
      myCheckState.requestDelayed();
      return;
    }

    InitializationState state = myContext.getInitializationState().getValue();
    if (state == null) {
      // wait until BugzillaContextImpl is ready
      myCheckState.requestDelayed();
      return;
    }

    if (state.isInitializationRequired())
      myInitialize.request();
  }

  protected synchronized void initialize() {
    getSyncTask().clearProblems();

    final boolean[] finished = {false};
    Procedure2<Boolean, String> runOnFinish = new Procedure2<Boolean, String>() {
      public void invoke(Boolean success, String error) {
        synchronized (AbstractConnectionInitializer.this) {
          updateInitializedState(success, error);
          synchronized (finished) {
            finished[0] = true;
          }
        }
      }
    };

    boolean started = startInitialization(runOnFinish);
    if (!started) {
      myInitialize.requestDelayed();
    } else {
      synchronized (finished) {
        if (!finished[0])
          myContext.setInitializationInProgress();
      }
      Log.debug("Initializing connection " + myContext.getConnection());
    }
  }

  public void stop() {
    myCheckState.abort();
    myInitialize.abort();
    myDetach.detach();
  }

  public void requestInitializationNow() {
    myInitialize.clearBacklog();
    myInitialize.run();
  }

  protected void updateInitializedState(boolean success, String error) {
    Log.debug(this + " result: " + success);
    if (!success)
      myInitialize.delay(PAUSE_BETWEEN_FAILED_INITIALIZATIONS);
    myContext.setInitializationResult(success, error);
  }

  protected abstract AbstractSyncTask getSyncTask();

  protected abstract boolean startInitialization(Procedure2<Boolean, String> runOnFinish);
}
