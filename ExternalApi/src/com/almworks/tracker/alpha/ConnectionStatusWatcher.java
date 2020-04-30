package com.almworks.tracker.alpha;

import com.almworks.dup.util.Event;
import com.almworks.dup.util.EventListener;
import com.almworks.dup.util.ValueModel;
import com.almworks.tracker.eapi.alpha.TrackerConnectionStatus;
import org.almworks.util.detach.Lifespan;

abstract class ConnectionStatusWatcher implements EventListener<Event> {
  private final ValueModel<TrackerConnectionStatus> myStatusModel;

  private int myLastConnectionId = 0;

  protected ConnectionStatusWatcher(ValueModel<TrackerConnectionStatus> statusModel) {
    myStatusModel = statusModel;
  }

  public void start(Lifespan life) {
    myStatusModel.events().addListener(life, this);
  }

  public void onEvent(Event event) {
    TrackerConnectionStatus lastStatus = myStatusModel.getValue();
    int lastId = myLastConnectionId;
    myLastConnectionId = lastStatus.isConnected() ? lastStatus.getTrackerConnectionId() : 0;
    if (lastId != myLastConnectionId) {
      if (myLastConnectionId == 0) {
        onDisconnect();
      } else {
        final int id = myLastConnectionId;
        onConnect(id);
      }
    }
  }

  protected void onDisconnect() {
  }

  protected void onConnect(int connectionId) {
  }
}
