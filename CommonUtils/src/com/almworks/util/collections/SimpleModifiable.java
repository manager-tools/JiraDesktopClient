package com.almworks.util.collections;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import java.util.ArrayList;

/**
 * @author : Dyoma
 */
public class SimpleModifiable implements RemoveableModifiable, ChangeListener {
  private Object myListeners = null;
  private Object myGate = null;

  public final void addChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.STRAIGHT, listener);
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, final ChangeListener listener) {
    if (!life.isEnded()) {
      synchronized (getLock()) {
        assert checkGateAndListeners();
        if (myListeners == null) {
          myListeners = listener;
          myGate = gate;
        } else if (myListeners.getClass() == ArrayList.class) {
          addListenerToList(gate, listener);
        } else {
          Object prevListener = myListeners;
          assert prevListener instanceof ChangeListener : prevListener;
          myListeners = new ArrayList(2);
          addListenerToList((ThreadGate) myGate, (ChangeListener) prevListener);
          addListenerToList(gate, listener);
        }
      }
      if (life != Lifespan.FOREVER)
        life.add(new Detach() {
          protected void doDetach() {
            removeChangeListener(listener);
          }
        });
    }
  }

  private void addListenerToList(ThreadGate gate, ChangeListener listener) {
    assert gate != null;
    assert listener != null;
    assert myListeners.getClass() == ArrayList.class : myListeners.getClass();
    ArrayList<ChangeListener> listenersList = (ArrayList<ChangeListener>) myListeners;
    listenersList.add(listener);
    if (gate == myGate)
      return;
    assert myGate != null;
    if (myGate.getClass() == ArrayList.class) {
      //noinspection OverlyStrongTypeCast
      ((ArrayList<ThreadGate>) myGate).add(gate);
    } else {
      ThreadGate prevGate = (ThreadGate) myGate;
      ArrayList<ThreadGate> gatesList = new ArrayList<ThreadGate>(listenersList.size());
      myGate = gatesList;
      for (int i = 0; i < listenersList.size() - 1; i++)
        gatesList.add(prevGate);
      gatesList.add(gate);
    }
  }

  @SuppressWarnings({"OverlyStrongTypeCast"})
  private boolean checkGateAndListeners() {
    if (myGate == null)
      assert myListeners == null : myListeners;
    else {
      assert myListeners != null;
      if (myGate instanceof ArrayList<?>) {
        assert myListeners instanceof ArrayList<?> : myListeners;
        int listenersNumber = ((ArrayList<?>) myListeners).size();
        int gatesNumber = ((ArrayList<?>) myGate).size();
        assert listenersNumber == gatesNumber : "listeners: " + listenersNumber + " gates:";
      } else
        assert myGate instanceof ThreadGate : myGate;
    }
    return true;
  }

  public void removeChangeListener(ChangeListener listener) {
    synchronized (getLock()) {
      assert checkGateAndListeners();
      if (myListeners == null)
        return;
      if (myListeners == listener) {
        myListeners = null;
        myGate = null;
        return;
      }
      if (myListeners.getClass() != ArrayList.class) return;
      ArrayList<ChangeListener> listenersList = ((ArrayList<ChangeListener>) myListeners);
      int index = listenersList.indexOf(listener);
      if (index < 0)
        return;
      listenersList.remove(index);
      if (myGate.getClass() != ArrayList.class)
        return;
      //noinspection OverlyStrongTypeCast
      ((ArrayList<ThreadGate>) myGate).remove(index);
    }
  }

  public final void addStraightListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.STRAIGHT, listener);
  }

  public final Detach addAWTChangeListener(ChangeListener listener) {
    DetachComposite life = new DetachComposite();
    addChangeListener(life, ThreadGate.AWT, listener);
    return life;
  }

  public final void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.AWT, listener);
  }

  public void fireChanged() {
    ChangeListener singleListener = null;
    ThreadGate singleGate = null;
    ChangeListener[] changeListeners = null;
    ThreadGate[] gates = null;
    synchronized (getLock()) {
      if (myListeners == null)
        return;
      if (myListeners.getClass() != ArrayList.class) {
        singleListener = (ChangeListener) myListeners;
        singleGate = (ThreadGate) myGate;
      } else {
        ArrayList<ChangeListener> listenersList = ((ArrayList<ChangeListener>) myListeners);
        changeListeners = listenersList.toArray(new ChangeListener[listenersList.size()]);
        if (myGate.getClass() == ArrayList.class) {
          ArrayList<ThreadGate> threadGates = (ArrayList<ThreadGate>) myGate;
          gates = threadGates.toArray(new ThreadGate[threadGates.size()]);
        } else {
          singleGate = (ThreadGate) myGate;
        }
      }
    }
    if (singleListener != null && singleGate != null) {
      fireChanged(singleGate, singleListener);
    } else {
      if (changeListeners == null) {
        assert false;
        return;
      }
      for (int i = 0; i < changeListeners.length; i++) {
        ChangeListener changeListener = changeListeners[i];
        if (gates != null)
          fireChanged(gates[i], changeListener);
        else
          fireChanged(singleGate, changeListener);
      }
    }
  }

  private void fireChanged(ThreadGate gate, final ChangeListener listener) {
    if (ThreadGate.isRightNow(gate))
      listener.onChange();
    else
      gate.execute(new Runnable() {
        public void run() {
          listener.onChange();
        }
      });
  }

  public void onChange() {
    fireChanged();
  }

  public void dispose() {
    synchronized (getLock()) {
      myListeners = null;
      myGate = null;
    }
  }

  protected Object getLock() {
    return this;
  }

  public int getListenerCount() {
    synchronized (getLock()) {
      if (myListeners == null) {
        return 0;
      } else if (myListeners.getClass() != ArrayList.class) {
        return 1;
      } else {
        ArrayList<ChangeListener> listenersList = ((ArrayList<ChangeListener>) myListeners);
        return listenersList.size();
      }
    }
  }
}
