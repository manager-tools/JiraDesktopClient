package com.almworks.explorer;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.install.Setup;
import com.almworks.gui.ArtifactTableColumns;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.advmodel.UniqueListModel;
import com.almworks.util.components.ATable;
import com.almworks.util.components.ColumnsConfiguration;
import com.almworks.util.components.tables.SortingTableHeaderController;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.JDOMConfigurator;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.CollectionModelEvent;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.util.Comparator;
import java.util.Map;

/**
 * @author dyoma
 */
public class ColumnsCollector implements Startable {
  public static final Role<ColumnsCollector> ROLE = Role.role("ColumnsCollector", ColumnsCollector.class);
  private final Engine myEngine;
  private AllColumns myAllColumns = new AllColumns();
  private final Map<Connection, ConnectionColumns> myConnectionColumns = Collections15.hashMap();
  private Detach myWorktime = Detach.NOTHING;
  private static final String DEFAULT_COLUMNS = "default";
  private final Configuration myConfig;

  public ColumnsCollector(Engine engine, Configuration configuration) {
    myEngine = engine;
    myConfig = configuration;
    if (!myConfig.isSet(DEFAULT_COLUMNS) || myConfig.getSubset(DEFAULT_COLUMNS).isEmpty()) {
      copyDefaultColumns(myConfig.getOrCreateSubset(DEFAULT_COLUMNS));
    }
  }

  public void copyDefaultColumns(Configuration dest) {
    ReadonlyConfiguration defaultColumns =
      JDOMConfigurator.parse(getClass().getClassLoader(), "com/almworks/rc/Columns_" + Setup.getProductId() + ".xml");
    ConfigurationUtil.copyTo(defaultColumns, dest);
  }

  /**
   * @return if connection is null, returns columns for all connections
   */
  @ThreadSafe
  public ArtifactTableColumns<LoadedItem> getColumns(@Nullable Connection connection) {
    if (connection == null) return myAllColumns;
    synchronized (myConnectionColumns) {
      ConnectionColumns columns = myConnectionColumns.get(connection);
      if (columns != null) {
        return columns;
      }
    }
    return myAllColumns;
  }

  public void start() {
    assert myWorktime == Detach.NOTHING : myWorktime;
    CollectionModel<Connection> connections = myEngine.getConnectionManager().getReadyConnectionsModel();
    myWorktime =
      connections.getEventSource().addListener(ThreadGate.AWT_QUEUED, new CollectionModel.Adapter<Connection>() {
        public void onScalarsAdded(CollectionModelEvent<Connection> event) {
          for (Connection connection : event.getCollection()) {
            ConnectionColumns columns = myAllColumns.createConnectionColumns(connection);
            synchronized (myConnectionColumns) {
              myConnectionColumns.put(connection, columns);
            }
          }
        }

        public void onScalarsRemoved(CollectionModelEvent<Connection> event) {
          for (Connection connection : event.getCollection()) {
            myAllColumns.stopListen(connection);
            synchronized (myConnectionColumns) {
              myConnectionColumns.remove(connection);
            }
          }
        }
      });
  }

  public void stop() {
    assert myWorktime != Detach.NOTHING;
    myWorktime.detach();
    myWorktime = Detach.NOTHING;
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        myAllColumns.clear();
      }
    });
  }

  public void configureColumns(Lifespan life, @Nullable Configuration config, SortingTableHeaderController<LoadedItem> header) {
    if (config == null) config = myConfig.getSubset(DEFAULT_COLUMNS);
    else {
      config = config.getOrCreateSubset("columns");
      if (config.isEmpty()) ConfigurationUtil.copyTo(myConfig.getSubset(DEFAULT_COLUMNS), config);
    }
    ColumnsConfiguration.install(life, config, false, header);
  }

  public void configureColumns(Lifespan life, ATable<? extends LoadedItem> table, SubsetModel<? extends TableColumnAccessor<?, ?>> columnSubset,
    Configuration config) {
    ColumnsConfiguration.install(life, config, table, columnSubset, false);
  }

  public static class ConnectionColumns implements ArtifactTableColumns<LoadedItem> {
    private final AListModel<? extends TableColumnAccessor<LoadedItem, ?>> myAllColumns;
    private final ColumnsSet<LoadedItem> myMain;
    @Nullable
    private final ColumnsSet<LoadedItem> myAux;

    public ConnectionColumns(ColumnsSet<LoadedItem> main, ColumnsSet<LoadedItem> aux) {
      myMain = main;
      myAux = aux;
      myAllColumns = aux == null ? main.model : SegmentedListModel.create(main.model, aux.model);
    }

    @NotNull
    public AListModel<? extends TableColumnAccessor<LoadedItem, ?>> getAll() {
      return myAllColumns;
    }

    @NotNull
    @Override
    public ColumnsSet<LoadedItem> getMain() {
      return myMain;
    }

    @Nullable
    @Override
    public ColumnsSet<LoadedItem> getAux() {
      return myAux;
    }
  }

  public static class AllColumns implements ArtifactTableColumns<LoadedItem> {
    private final UniqueListModel<TableColumnAccessor<LoadedItem, ?>> myMainColumns = UniqueListModel.create();
    @Nullable
    private UniqueListModel<TableColumnAccessor<LoadedItem, ?>> myAuxColumns;
    private final SegmentedListModel<TableColumnAccessor<LoadedItem, ?>> myAllColumns = SegmentedListModel.create(myMainColumns);
    private ColumnsSet<LoadedItem> myMain = null;
    private ColumnsSet<LoadedItem> myAux = null;

    public ConnectionColumns createConnectionColumns(@NotNull Connection connection) {
      AListModel<? extends TableColumnAccessor<LoadedItem, ?>> mainColumns = connection.getMainColumns();
      myMainColumns.listenModel(mainColumns);
      AListModel<? extends TableColumnAccessor<LoadedItem, ?>> auxColumns = connection.getAuxiliaryColumns();
      if (auxColumns != null) {
        if (myAuxColumns == null) {
          myAuxColumns = UniqueListModel.create();
          myAllColumns.addSegment(myAuxColumns);
        }
        //noinspection ConstantConditions
        myAuxColumns.listenModel(auxColumns);
      }

      Comparator<? super TableColumnAccessor<LoadedItem, ?>> mainOrder = connection.getMainColumnsOrder();
      Comparator<? super TableColumnAccessor<LoadedItem, ?>> auxOrder = connection.getAuxColumnsOrder();

      if (myMain == null) {
        myMain = new ColumnsSet<LoadedItem>(myMainColumns, mainOrder);
      } else assert myMain.order == mainOrder;
      if (myAux == null && myAuxColumns != null) {
        myAux = new ColumnsSet<LoadedItem>(myAuxColumns, auxOrder);
      } else if (myAux != null) assert myAux.order == auxOrder;

      return new ConnectionColumns(new ColumnsSet<LoadedItem>(mainColumns, mainOrder), auxColumns != null ? new ColumnsSet<LoadedItem>(auxColumns, auxOrder) : null);
    }

    public void stopListen(Connection connection) {
      myMainColumns.stopListen(connection.getMainColumns());
      AListModel<? extends TableColumnAccessor<LoadedItem, ?>> aux = connection.getAuxiliaryColumns();
      if (myAuxColumns != null && aux != null) {
        myAuxColumns.stopListen(aux);
      }
    }

    public void clear() {
      myMainColumns.clear();
      if (myAuxColumns != null) myAuxColumns.clear();
    }

    @NotNull
    @Override
    public AListModel<? extends TableColumnAccessor<LoadedItem, ?>> getAll() {
      return myAllColumns;
    }

    @NotNull
    @Override
    public ColumnsSet<LoadedItem> getMain() {
      return myMain != null ? myMain : ColumnsSet.<LoadedItem>empty();
    }

    @Override
    public ColumnsSet<LoadedItem> getAux() {
      return myAux;
    }
  }
}
