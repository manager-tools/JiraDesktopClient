package com.almworks.engine;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.engine.*;
import com.almworks.api.store.Store;
import com.almworks.items.api.Database;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.InterruptableRunnable;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.Startable;

import java.util.Collection;

public class EngineImpl implements Engine, Startable {
  private final ConnectionManagerImpl myConnectionManager;
  private final EngineViewsImpl myViews;
  private final ItemProvider[] myProviders;
  private final SynchronizerImpl mySynchronizer;

  private final OrderListModel<ConstraintDescriptor> myGlobalDescriptors = OrderListModel.create();
  private final ApplicationLoadStatus.StartupActivity myStartup;

  // NB! If you add Configuration to dependencies, consider changing the name of the role (Engine.ROLE). See EngineComponentDescriptor for more info.
  public EngineImpl(ItemProvider[] providers, ConnectionManagerImpl connectionManager, Store store, Database db, ApplicationLoadStatus startup) {
    assert providers != null;
    myProviders = providers;
    myViews = new EngineViewsImpl(providers, db);
    myConnectionManager = connectionManager;
    mySynchronizer = new SynchronizerImpl(myConnectionManager.getConnections(), store);
    myStartup = startup.createActivity("Engine");
  }

  public ConnectionManager getConnectionManager() {
    return myConnectionManager;
  }

  @Override
  public Database getDatabase() {
    return myViews.getDatabase();
  }

  @NotNull
  public Synchronizer getSynchronizer() {
    return mySynchronizer;
  }

  public EngineViews getViews() {
    return myViews;
  }

  @ThreadAWT
  public void registerGlobalDescriptor(ConstraintDescriptor descriptor) {
    myGlobalDescriptors.addElement(descriptor);
  }

  public AListModel<ConstraintDescriptor> getGlobalDescriptors() {
    return myGlobalDescriptors;
  }

  public void start() {
    UserDataHolder data = myViews.getDatabase().getUserData();
    if (!data.putIfAbsent(ROLE, this)) LogHelper.error("Another engine exists", this, data.getUserData(ROLE));
    if (myProviders != null)
      myConnectionManager.configureProviders(myProviders);
    myConnectionManager.loadConnections();
    mySynchronizer.start();
    watchStartup();
  }

  private void watchStartup() {
    ThreadGate.executeLong(new InterruptableRunnable() {
      @Override
      public void run() throws InterruptedException {
        Collection<Connection> connections = getConnectionManager().getConnections().getFullCollectionBlocking();
        for (final Connection connection : connections) {
          final ApplicationLoadStatus.StartupActivity startup = myStartup.createSubActivity(connection.getConnectionID());
          connection.getState().getEventSource().addStraightListener(startup.getLife(), new ScalarModel.Adapter<ConnectionState>() {
            @Override
            public void onScalarChanged(ScalarModelEvent<ConnectionState> event) {
              ConnectionState state = event.getNewValue();
              if (state.isReady() || state.isDegrading()) {
                startup.done();
              }
            }
          });
        }
        myStartup.done();
      }
    });
  }

  public void stop() {
  }
}

