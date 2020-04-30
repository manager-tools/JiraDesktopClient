package com.almworks.dup.util;

import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EventProcessor is a helper class that is used by models to
 * provide EventSource. It has public fire() method that model uses
 * to publish events.
 */
public class EventProcessor<EE extends Event> implements EventSource<EE> {
  private final EventProcessorMaster myMaster;

  /**
   * Contains pairs - event class, listener.
   */
  private List<Object> myListeners;

  public EventProcessor(EventProcessorMaster master) {
    myMaster = master;
  }

  public void addListener(Lifespan life, EventListener<Event> listener) {
    addListener(life, Event.class, listener);
  }

  public <E extends Event> void addListener(Lifespan life, final Class<E> eventClass, final EventListener<E> listener) {
    assert EventQueue.isDispatchThread();
    if (life.isEnded())
      return;
    if (myListeners == null)
      myListeners = new ArrayList<Object>();
    myListeners.add(listener);
    myListeners.add(eventClass);
    life.add(new Detach() {
      protected void doDetach() {
        removeListener(eventClass, listener);
      }
    });
    if (myMaster != null)
      myMaster.afterListenerAdded(this, eventClass, listener, life);
  }

  public void removeListener(Class eventClass, EventListener listener) {
    assert EventQueue.isDispatchThread();
    if (myListeners == null)
      return;
    for (int i = 0; i < myListeners.size() - 1; i += 2) {
      Object obj = myListeners.get(i);
      if (listener.equals(obj)) {
        if (eventClass.equals(myListeners.get(i + 1))) {
          myListeners.remove(i);
          myListeners.remove(i);
          return;
        }
      }
    }
  }

  public void fireEvent(Event event) {
    assert EventQueue.isDispatchThread();
    assert event != null;
    if (myListeners == null || myListeners.size() == 0) {
      return;
    }
    Object[] listeners = myListeners.toArray();
    Class eventClass = event.getClass();
    for (int i = 0; i < listeners.length - 1; i += 2) {
      Class c = (Class) listeners[i + 1];
      if (c.isAssignableFrom(eventClass)) {
        ((EventListener)listeners[i]).onEvent(event);
      }
    }
  }
}
