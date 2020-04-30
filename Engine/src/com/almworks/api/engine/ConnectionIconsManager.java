package com.almworks.api.engine;

import com.almworks.api.download.DownloadOwner;
import com.almworks.api.image.RemoteIconManager;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.LightScalarModel;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadSafe;
import org.picocontainer.Startable;

import javax.swing.*;
import java.net.MalformedURLException;
import java.util.List;

public class ConnectionIconsManager implements Startable {
  public static final Role<ConnectionIconsManager> ROLE = Role.role("connectionIconsManager");

  private final ConnectionManager myConnections;
  private final RemoteIconManager myRemoteIconManager;
  private final ConnectionsListener myListener = new ConnectionsListener();
  // guarded by myLock {
  private final MultiMap<Long, Request> myRequestsByConnection = MultiMap.create();
  // }
  private final Object myLock = new Object();
  private final ThreadGate myGate = ThreadGate.LONG(this);

  public ConnectionIconsManager(ConnectionManager connections, RemoteIconManager remoteIconManager) {
    myConnections = connections;
    myRemoteIconManager = remoteIconManager;
  }

  @ThreadSafe
  @CanBlock
  public Icon getIcon(long connectionItem, String iconUrl) throws MalformedURLException, InterruptedException {
    Connection connection;
    try {
      connection = myConnections.findByItem(connectionItem, false, true);
    } catch (RuntimeException e) {
      LogHelper.warning(e);
      connection = null;
    }
    if (connection != null) {
      return myRemoteIconManager.getRemoteIcon(iconUrl, connection.getDownloadOwner(), iconUrl);
    } else {
      Request request = new Request(iconUrl);
      synchronized (myLock) {
        myRequestsByConnection.add(connectionItem, request);
      }
      return myRemoteIconManager.getRemoteIcon(iconUrl, myGate, request.getModel());
    }
  }

  @Override
  public void start() {
    myConnections.addConnectionChangeListener(myGate, myListener);
  }

  @Override
  public void stop() {
  }

  private class ConnectionsListener implements ConnectionChangeListener {
    @Override
    public void onChange(Connection connection, ConnectionState oldState, ConnectionState newState) {
      if (newState.isReady()) {
        List<Request> requests;
        synchronized (myLock) {
          requests = myRequestsByConnection.getAll(connection.getConnectionItem());
        }
        if (requests != null) {
          DownloadOwner downloadOwner = connection.getDownloadOwner();
          for (Request request : requests) {
            request.setToModel(downloadOwner);
          }
        }
      }
    }
  }

  private class Request {
    private final LightScalarModel<Pair<DownloadOwner, String>> myModel = LightScalarModel.create();
    private final String myUrl;

    public Request(String url) {
      myUrl = url;
    }

    public LightScalarModel<Pair<DownloadOwner, String>> getModel() {
      return myModel;
    }

    public void setToModel(DownloadOwner owner) {
      myModel.setValue(Pair.create(owner, myUrl));
    }
  }
}
