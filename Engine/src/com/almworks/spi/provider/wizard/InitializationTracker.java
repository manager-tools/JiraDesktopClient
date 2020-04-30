package com.almworks.spi.provider.wizard;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionState;
import com.almworks.api.engine.InitializationState;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.progress.ProgressData;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import java.util.List;

public class InitializationTracker {
  private final Lifecycle myUpdateCycle = new Lifecycle();

  private volatile Connection myConnection;
  private volatile boolean myInitEverStarted;
  private final BasicScalarModel<Boolean> myRetryEnabled = BasicScalarModel.createModifiable(false);
  private final AnAction myRetryAction = new RetryAction();

  private ConnectionState myLastCState;
  private InitializationState myLastIState;

  public void attachTo(Lifespan life, final Connection connection) {
    myConnection = connection;
    myInitEverStarted = false;
    myRetryEnabled.setValue(false);

    final ScalarModel.Consumer updater = new ScalarModel.Adapter() {
      @Override
      public void onScalarChanged(ScalarModelEvent objectScalarModelEvent) {
        updateState(connection);
      }
    };
    connection.getState().getEventSource().addAWTListener(life, updater);
    connection.getInitializationState().getEventSource().addAWTListener(life, updater);
  }

  private void updateState(Connection connection) {
    myUpdateCycle.cycle();

    final ConnectionState cState = connection.getState().getValue();
    final InitializationState iState = connection.getInitializationState().getValue();
    if(cState == null || iState == null) {
      return;
    }

    stateTransition(cState, iState);

    if(cState.isReady()) {
      if(iState.isInitializing()) {
        final ProgressSource source = connection.getConnectionSynchronizer().getProgressSource();
        final ChangeListener listener = new ChangeListener() {
          @Override
          public void onChange() {
            final ProgressData progress = source.getProgressData();
            progress(progress.getProgress());
            final String error = getErrorText(progress);
            if(error != null) {
              display(true, error);
            } else {
              display(false, getActivityText(progress));
            }
          }
        };
        listener.onChange();
        source.getModifiable().addAWTChangeListener(myUpdateCycle.lifespan(), listener);
      } else if(iState != InitializationState.INITIALIZED) {
        final String error = connection.getContext().getLastInitializationError();
        if(error != null) {
          display(true, error);
        }
      }
    }

    myRetryEnabled.setValue(myInitEverStarted && iState.isInitializationRequired() && cState.isReady());
  }

  private void stateTransition(ConnectionState cState, InitializationState iState) {
    showStateText(cState, iState);
    myLastCState = cState;
    myLastIState = iState;
  }

  private void showStateText(ConnectionState cState, InitializationState iState) {
    final boolean cChanged = !Util.equals(cState, myLastCState);
    final boolean iChanged = !Util.equals(iState, myLastIState);
    if(!cChanged && !iChanged) {
      return;
    }

    if(cChanged) {
      if(cState.isGettingReady()) {
        return;
      }
      if(cState.isDegrading()) {
        display(false, "Connection is " + Util.lower(cState.getName()) + ".");
        return;
      }
    }

    if(cState.isReady()) {
      if((myLastIState == null || myLastIState.isInitializationRequired()) && iState.isInitializing()) {
        myInitEverStarted = true;
      } else if(myLastIState != null && myLastIState.isInitializing() && !iState.isInitializing()) {
        if(iState.isInitializationRequired()) {
          display(true, "Initialization failed.");
        }
      }
    }
  }

  private String getActivityText(ProgressData progress) {
    if(progress.isDone()) {
      return null;
    }
    final Object activity = progress.getActivity();
    if(activity != null) {
      return activity.toString();
    }
    return null;
  }

  private String getErrorText(ProgressData progress) {
    final List<String> errors = progress.getErrors();
    if(errors.isEmpty()) {
      return null;
    }
    return errors.get(0);
  }

  public AnAction getRetryAction() {
    return myRetryAction;
  }

  protected void display(boolean error, String message) {}

  protected void progress(double progress) {}

  private class RetryAction extends SimpleAction {
    public RetryAction() {
      super("Retry Initialization");
      updateOnChange(myRetryEnabled);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(myRetryEnabled.getValue());
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      if(myRetryEnabled.getValue() && myConnection != null) {
        myConnection.requestReinitialization();
      }
    }
  }

}
