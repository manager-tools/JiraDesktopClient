package com.almworks.util.events;

import com.almworks.util.exec.ThreadGate;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface AddListenerHook <L> {
  Object beforeAddListenerWithoutLock(ThreadGate threadGate, L listener);

  Object beforeAddListenerWithLock(ThreadGate threadGate, L listener, Object passThrough);

  Object afterAddListenerWithLock(ThreadGate threadGate, L listener, Object passThrough);

  void afterAddListenerWithoutLock(ThreadGate threadGate, L listener, Object passThrough);

  class Adapter <L> implements AddListenerHook<L> {
    public Object beforeAddListenerWithoutLock(ThreadGate threadGate, L listener) {
      return null;
    }

    public Object beforeAddListenerWithLock(ThreadGate threadGate, L listener, Object passThrough) {
      return passThrough;
    }

    public Object afterAddListenerWithLock(ThreadGate threadGate, L listener, Object passThrough) {
      return passThrough;
    }

    public void afterAddListenerWithoutLock(ThreadGate threadGate, L listener, Object passThrough) {
    }
  }
}
