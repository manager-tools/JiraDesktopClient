package com.almworks.util.collections;

import org.almworks.util.detach.Detach;

import java.util.Collection;

// todo move this and addXXXListener(Lifespan, ...) to some common utility class near Lifespan class
public class CollectionRemove<E> extends Detach {
  private volatile Collection<E> myCollection;
  private volatile E myElement;
  private volatile Object myLock;

  public CollectionRemove(Collection<E> collection, E element, Object lock) {
    myCollection = collection;
    myElement = element;
    myLock = lock;
  }

  public static <E> CollectionRemove<E> create(Collection<E> collection, E element, Object lock) {
    return new CollectionRemove<E>(collection, element, lock);
  }

  public static <E> CollectionRemove<E> create(Collection<E> collection, E element) {
    return new CollectionRemove<E>(collection, element, null);
  }

  protected void doDetach() throws Exception {
    Collection<E> collection = myCollection;
    E element = myElement;
    Object lock = myLock;
    myCollection = null;
    myElement = null;
    myLock = null;
    if (collection == null)
      return;
    if (lock != null) {
      synchronized (lock) {
        collection.remove(element);
      }
    } else {
      collection.remove(element);
    }
  }
}
