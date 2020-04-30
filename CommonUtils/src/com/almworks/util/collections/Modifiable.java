package com.almworks.util.collections;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

/**
 * @author : Dyoma
 */
public interface Modifiable {
  RemoveableModifiable NEVER = new NeverModifiable();

  /**
   * @deprecated {@link #addAWTChangeListener(org.almworks.util.detach.Lifespan, ChangeListener)}
   */
  @Deprecated
  Detach addAWTChangeListener(ChangeListener listener);

  void addChangeListener(Lifespan life, ChangeListener listener);

  void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener);

  void addAWTChangeListener(Lifespan life, ChangeListener listener);


  public static class NeverModifiable implements RemoveableModifiable {
    public Detach addAWTChangeListener(ChangeListener listener) {
      return Detach.NOTHING;
    }

    public void addChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    }

    public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void removeChangeListener(ChangeListener listener) {
    }
  }
}
