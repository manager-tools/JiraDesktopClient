package com.almworks.itemsync;

import com.almworks.items.sync.SyncManager;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

public class SyncManagerModifiable implements Modifiable {
  private final SyncManager myManager;

  public SyncManagerModifiable(SyncManager manager) {
    myManager = manager;
    getModifiable();
  }

  private Modifiable getModifiable() {
    return myManager.getModifiable();
  }

  @Override
  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    getModifiable().addAWTChangeListener(life, listener);
  }

  @Override
  @Deprecated
  public Detach addAWTChangeListener(ChangeListener listener) {
    return getModifiable().addAWTChangeListener(listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    getModifiable().addChangeListener(life, gate, listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ChangeListener listener) {
    getModifiable().addChangeListener(life, listener);
  }
}
