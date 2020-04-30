package com.almworks.util.events;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

public abstract class EventSource <L> {
  public static final EventSource MUTE = new MuteEventSource();

  public boolean addStraightListener(Lifespan life, L listener) {
    return addListener(life, ThreadGate.STRAIGHT, listener);
  }

  public Detach addStraightListener(L listener) {
    DetachComposite life = new DetachComposite();
    addListener(life, ThreadGate.STRAIGHT, listener);
    return life;
  }

  public boolean addAWTListener(Lifespan life, L listener) {
    return addListener(life, ThreadGate.AWT, listener);
  }

  public Detach addAWTListener(L listener) {
    DetachComposite life = new DetachComposite();
    addListener(life, ThreadGate.AWT, listener);
    return life;
  }

  public Detach addListener(ThreadGate callbackGate, L listener) {
    DetachComposite life = new DetachComposite();
    addListener(life, callbackGate, listener);
    return life;
  }
  public abstract boolean addListener(Lifespan life, ThreadGate callbackGate, L listener);

  public abstract void removeListener(L listener);

  public abstract void addChainedSource(EventSource<L> chained);

  public abstract void removeChainedSource(EventSource<L> chained);


  public static class MuteEventSource extends EventSource {
    private MuteEventSource() {}

    public boolean addListener(Lifespan life, ThreadGate callbackGate, Object listener) {
      return false;
    }

    public void removeListener(Object listener) {
    }

    public void addChainedSource(EventSource chained) {
    }

    public void removeChainedSource(EventSource chained) {
    }
  }
}
