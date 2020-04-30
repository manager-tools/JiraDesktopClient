package com.almworks.application;

import com.almworks.api.application.ItemKeyCache;
import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemProvider;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.util.advmodel.AListAggregator;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.MapListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Lazy;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.CollectionModelEvent;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author dyoma
 */
public class NameResolverImpl implements NameResolver {
  private final Engine myEngine;
  private final ItemKeyCache myCache = new ItemKeyCache();

  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private boolean myModifiableSubscribed = false;

  private final MapListModel<ConstraintDescriptor, String> myGlobalDescriptorsCache;

  @ThreadAWT
  private final Lazy<AListModel<ConstraintDescriptor>> myAllDescriptorsModel =
    new Lazy<AListModel<ConstraintDescriptor>>() {
      @NotNull
      protected AListModel<ConstraintDescriptor> instantiate() {
        return createAggregateDescriptorModel(Lifespan.FOREVER, new ItemHypercubeImpl());
      }
    };

  public NameResolverImpl(Engine engine) {
    myEngine = engine;
    myGlobalDescriptorsCache = MapListModel.create(engine.getGlobalDescriptors(), ConstraintDescriptor.TO_ID);
  }

  @ThreadAWT
  @NotNull
  public AListModel<ConstraintDescriptor> getConstraintDescriptorModel(final Lifespan life, ItemHypercube cube) {
    if (cube.getAxisCount() == 0) {
      // empty hypercube
      return getAllDescriptorsModel();
    } else {
      return createAggregateDescriptorModel(life, cube);
    }
  }

  @NotNull
  @Override
  public AListModel<ConstraintDescriptor> getAllDescriptorsModel() {
    return myAllDescriptorsModel.get();
  }

  @ThreadAWT
  @NotNull
  private AListModel<ConstraintDescriptor> createAggregateDescriptorModel(final Lifespan life, final ItemHypercube cube) {
    final Collection<Long> connectionItems = ItemHypercubeUtils.getIncludedConnections(cube);
    final AListAggregator<ConstraintDescriptor> aggregator = AListAggregator.create();
    AListModel<ConstraintDescriptor> globalDescriptors = myGlobalDescriptorsCache;
    if (globalDescriptors != null) {
      aggregator.add(life, "global", globalDescriptors);
    }
    CollectionModel<Connection> connectionsModel = myEngine.getConnectionManager().getReadyConnectionsModel();
    connectionsModel.getEventSource().addAWTListener(life, new CollectionModel.Adapter<Connection>() {
      public void onScalarsAdded(CollectionModelEvent<Connection> event) {
        for (Connection connection : event.getCollection()) {
          if (connectionItems.isEmpty() || connectionItems.contains(connection.getConnectionItem())) {
            AListModel<? extends ConstraintDescriptor> descriptorsModel = connection.getDescriptors();
            if (descriptorsModel == null) {
              assert false : connection;
              continue;
            }
            aggregator.add(life, connection, descriptorsModel);
          }
        }
      }

      public void onScalarsRemoved(CollectionModelEvent<Connection> event) {
        for (Connection connection : event.getCollection()) {
          aggregator.remove(connection);
        }
      }
    });
    return aggregator.getImage();
  }

  @ThreadSafe
  @Nullable
  public ConstraintDescriptor getConditionDescriptor(final String id, @NotNull ItemHypercube cube) {
    if (id == null)
      return null;
    ConstraintDescriptor descriptor = myGlobalDescriptorsCache.get(id);
    if (descriptor != null) {
      return descriptor;
    }
    final List<Connection> connections = getConnections(cube);
    for (int i = 0; i < connections.size(); i++) {
      Connection connection = connections.get(i);
      if (connection == null) {
        assert false : id;
        continue;
      }
      descriptor = connection.getDescriptorByIdSafe(id);
      if (descriptor != null) {
        assert descriptor.getId().equals(id);
        return descriptor;
      }
    }
    return null;
  }

  @ThreadAWT
  @NotNull
  public Modifiable getModifiable() {
    Threads.assertAWTThread();
    subscribe();
    return myModifiable;
  }

  @ThreadAWT
  private void subscribe() {
    Threads.assertAWTThread();
    if (myModifiableSubscribed)
      return;

    final AListModel<ConstraintDescriptor> descriptorsModel = getAllDescriptorsModel();

    descriptorsModel.addChangeListener(Lifespan.FOREVER, myModifiable);
    for (ConstraintDescriptor descriptor : descriptorsModel.toList()) {
      descriptor.getModifiable().addChangeListener(Lifespan.FOREVER, myModifiable);
    }

    descriptorsModel.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        Threads.assertAWTThread();
        for (int k = 0; k < length; k++) {
          descriptorsModel.getAt(index + k).getModifiable().addChangeListener(Lifespan.FOREVER, myModifiable);
        }
      }
    });

    descriptorsModel.addRemovedElementListener(new AListModel.RemovedElementsListener<ConstraintDescriptor>() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<ConstraintDescriptor> elements) {
        Threads.assertAWTThread();
        List<ConstraintDescriptor> list = elements.getList();
        for (ConstraintDescriptor descriptor : list) {
          descriptor.getModifiable().removeChangeListener(myModifiable);
        }
      }
    });

    myModifiableSubscribed = true;
  }

  public ItemKeyCache getCache() {
    return myCache;
  }

//  public ScalarModel<Boolean> getEnumDescriptomyModifiablersReady() {
//    return myEnumDescriptorsReady;
//  }

  private Collection<ItemProvider> getProviders(ItemHypercube cube) {
    Collection<Long> connections = ItemHypercubeUtils.getIncludedConnections(cube);
    ConnectionManager cm = myEngine.getConnectionManager();
    if (connections.isEmpty())
      return cm.getProviders();
    Set<ItemProvider> result = Collections15.hashSet();
    for (long pointer : connections) {
      Connection connection = cm.findByItem(pointer);
      if (connection != null)
        result.add(connection.getProvider());
    }
    return result;
  }

  private List<Connection> getConnections(ItemHypercube cube) {
    Collection<Long> connections = ItemHypercubeUtils.getIncludedConnections(cube);
    ConnectionManager cm = myEngine.getConnectionManager();
    if (connections.isEmpty())
      return cm.getReadyConnectionsModel().copyCurrent();
    List<Connection> result = Collections15.arrayList();
    for (long pointer : connections) {
      Connection connection = cm.findByItem(pointer, false, true);
      if (connection != null && !result.contains(connection))
        result.add(connection);
    }
    return result;
  }

  public interface Listener {
    void onDescriptorRegistered(ConstraintDescriptor descriptor, ItemProvider provider);

    void onDescriptorDerestered(ConstraintDescriptor descriptor, ItemProvider provider);
  }
}
