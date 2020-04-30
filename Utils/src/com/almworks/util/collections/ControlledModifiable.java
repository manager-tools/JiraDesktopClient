package com.almworks.util.collections;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

public class ControlledModifiable extends SimpleModifiable {
  private final Controller myController;

  public ControlledModifiable(@NotNull Controller controller) {
    myController = controller;
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    super.addChangeListener(life, gate, listener);
    myController.onListenerAdded(this);
  }

  public void removeChangeListener(ChangeListener listener) {
    super.removeChangeListener(listener);
    myController.onListenerRemoved(this);
  }

  public static interface Controller {
    void onListenerAdded(ControlledModifiable modifiable);

    void onListenerRemoved(ControlledModifiable modifiable);
  }
}
