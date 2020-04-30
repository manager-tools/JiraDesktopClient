package com.almworks.spi.provider;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.*;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.Threads;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

public class ConnectionSetupUtil {
  public static void createConnection(
    @NotNull final ComponentContainer container, @NotNull final Configuration configuration,
    @NotNull final NewConnectionSink sink)
  {
    ThreadGate.LONG(ConnectionSetupUtil.class).execute(new Runnable() {
      public void run() {
        try {
          doCreateConnection(configuration, container, sink);
        } catch (final ConfigurationException e) {
          Log.error(e);
        }
      }
    });
  }

  private static void doCreateConnection(
    @NotNull final Configuration configuration, @NotNull final ComponentContainer container,
    @NotNull final NewConnectionSink sink) throws ConfigurationException
  {
    Threads.assertLongOperationsAllowed();
    Engine engine = container.getActor(Engine.ROLE);
    if (engine == null)
      throw new ConfigurationException("no engine");
    ItemProvider provider = container.getActor(ItemProvider.class);

    final Connection connection;
    try {
      connection = engine.getConnectionManager().createConnection(provider, configuration);
      if (connection == null) {
        // no access
        sink.connectionCreated(null);
        return;
      }
    } catch (ProviderDisabledException e) {
      Log.warn(e);
      sink.connectionCreated(null);
      return;
    }

    final DetachComposite detach = new DetachComposite(true);
    detach.add(connection.getState().getEventSource().addStraightListener(new ScalarModel.Adapter<ConnectionState>() {
      public void onScalarChanged(ScalarModelEvent<ConnectionState> event) {
        ConnectionState state = event.getNewValue();
        if (state != null && state.isStable()) {
          detach.detach();
          if (state.isReady()) {
            afterConnectionCreated(connection, sink);
          }
        }
      }
    }));
  }

  private static void afterConnectionCreated(@NotNull final Connection connection, final NewConnectionSink sink) {
    showConnectionInfo(connection, sink);
    final DetachComposite detach = new DetachComposite(true);
    detach.add(connection.getInitializationState()
      .getEventSource()
      .addStraightListener(new ScalarModel.Adapter<InitializationState>() {
        public void onScalarChanged(ScalarModelEvent<InitializationState> event) {
          InitializationState state = event.getNewValue();
          if (state != null && state.isInitialized()) {
            detach.detach();
            afterConnectionInitialized(connection, sink);
          }
        }
      }));
  }

  private static void showConnectionInfo(final Connection connection, final NewConnectionSink sink) {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        sink.connectionCreated(connection);
        assert connection != null;
      }
    });
  }

  private static void afterConnectionInitialized(final Connection connection, final NewConnectionSink sink) {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        final ComponentContainer container = connection.getConnectionContainer();
        final ConnectionContext context = container.getActor(ConnectionContext.class);
        final ExplorerComponent explorer = container.getActor(ExplorerComponent.ROLE);
        if (context == null || explorer == null) {
          assert context != null : connection;
          //noinspection ConstantConditions
          assert explorer != null : connection;
          return;
        }
        sink.showMessage(L.content("Creating default " + Terms.queries));
        explorer.createDefaultQueries(connection, new Runnable() {
          public void run() {
            explorer.expandConnectionNode(connection, false);
            sink.initializationComplete();
          }
        });
      }
    });
  }

  public static void updateConnection(ComponentContainer container, Connection connection, Configuration configuration) {
    final Engine engine = container.getActor(Engine.ROLE);
    if(engine != null) {
      engine.getConnectionManager().updateConnection(connection, configuration);
    } else {
      Log.error("no engine");
    }
  }
}
