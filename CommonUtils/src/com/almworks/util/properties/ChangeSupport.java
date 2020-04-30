package com.almworks.util.properties;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.events.EventSource;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class ChangeSupport {
  private final Object myBean;
  private volatile FireEventSupport<ChangeListener> myChangeListeners = null;
  private volatile FireEventSupport<PropertyChangeListener.Any> myAnyPropertyChangeListeners = null;
  private volatile Map<TypedKey<?>, Collection<PropertyChangeListener<?>>> myListeners = null;

  public ChangeSupport(Object bean) {
    myBean = bean;
  }

  public synchronized Detach addListener(ChangeListener listener) {
    DetachComposite life = new DetachComposite();
    getChangeListeners().addStraightListener(life, listener);
    return life;
  }

  public synchronized void removeListener(ChangeListener listener) {
    if (myChangeListeners == null)
      return;
    myChangeListeners.removeListener(listener);
  }

  private synchronized EventSource<ChangeListener> getChangeListeners() {
    if (myChangeListeners == null) {
      myChangeListeners = FireEventSupport.createSynchronized(ChangeListener.class);
    }
    return myChangeListeners;
  }

  public synchronized Detach addListener(ChangeListener listener, ThreadGate gate) {
    DetachComposite life = new DetachComposite();
    getChangeListeners().addListener(life, gate, listener);
    return life;
  }

  public synchronized <T> void fireChanged(TypedKey<T> key, T oldValue, T newValue) {
    Map<TypedKey<?>, Collection<PropertyChangeListener<?>>> map = myListeners;
    if (map != null) {
      Collection<PropertyChangeListener<?>> listeners = map.get(key);
      if (listeners != null)
        //noinspection unchecked,RedundantCast,RawUseOfParameterizedType
        fireChanged((Collection<? extends PropertyChangeListener<T>>)(Collection) listeners, key, oldValue, newValue);
    }
    FireEventSupport<PropertyChangeListener.Any> anyListeners = myAnyPropertyChangeListeners;
    if (anyListeners != null) {
      anyListeners.getDispatcher().propertyChanged(key);
    }
    fireChanged();
  }

  public void fireChanged() {
    FireEventSupport<ChangeListener> changeListeners = myChangeListeners;
    if (changeListeners != null)
      changeListeners.getDispatcher().onChange();
  }

  private <T> void fireChanged(Collection<? extends PropertyChangeListener<T>> listeners, TypedKey<T> key, T oldValue,
    T newValue)
  {
    for (PropertyChangeListener<T> listener : listeners)
      listener.propertyChanged(key, myBean, oldValue, newValue);
  }

  public <T> Detach addListener(final TypedKey<? extends T> key,
    final PropertyChangeListener<? super T> listener)
  {
    doAddListener(key, listener);
    return createRemoveDetach(key, listener);
  }

  private <T> void doAddListener(TypedKey<? extends T> key, PropertyChangeListener<? super T> listener) {
    synchronized (this) {
      if (myListeners == null)
        myListeners = Collections15.hashMap();
      Collection<PropertyChangeListener<?>> listeners = myListeners.get(key);
      if (listeners == null) {
        listeners = Collections15.arrayList();
        myListeners.put(key, listeners);
      }
      listeners.add(listener);
    }
  }

  public <T> void addListener(Lifespan life, TypedKey<T> property, PropertyChangeListener<T> listener) {
    if (life.isEnded())
      return;
    doAddListener(property, listener);
    if (life != Lifespan.FOREVER)
      life.add(createRemoveDetach(property, listener));
  }

  private <T> Detach createRemoveDetach(final TypedKey<? extends T> key, final PropertyChangeListener<? super T> listener) {
    return new Detach() {
      protected void doDetach() {
        removeListener(key, listener);
      }
    };
  }

  public synchronized <T> void removeListener(TypedKey<? extends T> key, PropertyChangeListener<? super T> listener) {
    Map<TypedKey<?>, Collection<PropertyChangeListener<?>>> map = myListeners;
    if (map == null)
      return;
    Collection<PropertyChangeListener<?>> listeners = map.get(key);
    if (listeners == null)
      return;
    listeners.remove(listener);
    if (listeners.size() == 0)
      myListeners.remove(key);
  }

  public static ChangeSupport create(Object bean) {
    return new ChangeSupport(bean);
  }

  public <M, T> void fireChangedOn(final PropertyKey<M, T> key, Modifiable modifiable) {
    modifiable.addChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        fireChanged(key.getValueKey(), null, null);
      }
    });
  }

  public void addListener(PropertyChangeListener.Any listener) {
    synchronized (this) {
      if (myAnyPropertyChangeListeners == null)
        myAnyPropertyChangeListeners = FireEventSupport.createSynchronized(PropertyChangeListener.Any.class);
    }
    myAnyPropertyChangeListeners.addStraightListener(Lifespan.FOREVER, listener);
  }
}
