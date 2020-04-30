package com.almworks.util.model;

import com.almworks.util.commons.Lazy;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ArrayListCollectionModel <E> extends AbstractContentModel<
  CollectionModelEvent<E>, CollectionModel.Consumer<E>, CollectionModel<E>, CollectionModelSetter<E>
  >
  implements CollectionModel<E>, CollectionModelSetter<E> {

  private final List<E> myList;
  private final Lazy<Collection<E>> myCollection = new Lazy<Collection<E>>() {
    public Collection<E> instantiate() {
      return new WrappingCollection();
    }
  };
  private final Lazy<BasicScalarModel<Integer>> myCountModel = new Lazy<BasicScalarModel<Integer>>() {
    public BasicScalarModel<Integer> instantiate() {
      return BasicScalarModel.createWithValue(myList.size(), true);
    }
  };

  private ArrayListCollectionModel(boolean allowChanges, boolean fullyKnown, Collection<E> baseList) {
    super(fullyKnown, allowChanges, (Class) CollectionModel.Consumer.class);
    myList = baseList == null ? new ArrayList<E>() : new ArrayList<E>(baseList);
  }

  public static <E> ArrayListCollectionModel<E> create(boolean allowChanges, boolean isFullyKnown) {
    return new ArrayListCollectionModel<E>(allowChanges, isFullyKnown, null);
  }

  public static <E> ArrayListCollectionModel<E> create(boolean allowChanges, Collection<E> alreadyKnownList) {
    return new ArrayListCollectionModel<E>(allowChanges, true, alreadyKnownList);
  }

  public static <E> ArrayListCollectionModel<E> create(Collection<E> alreadyKnownList) {
    return new ArrayListCollectionModel<E>(false, true, alreadyKnownList);
  }

  public static <E> ArrayListCollectionModel<E> create() {
    return new ArrayListCollectionModel<E>(true, false, null);
  }

  public List<E> copyCurrent() {
    synchronized (myLock) {
      return Collections15.arrayList(myList);
    }
  }

  public int getCurrentCount() {
    synchronized (myLock) {
      return myList.size();
    }
  }

  public ScalarModel<Integer> getCountModel() {
    synchronized (myLock) {
      return myCountModel.get();
    }
  }

  public boolean containsCurrently(E e) {
    synchronized(myLock) {
      return myCollection.isInitialized() ? myCollection.get().contains(e) : false;
    }
  }

  private void updateCount() {
    BasicScalarModel<Integer> model;
    int count;
    synchronized(myLock) {
      model = myCountModel.isInitialized() ? myCountModel.get() : null;
      count = myList.size();
    }
    if (model != null)
      model.setValue(count);
  }

  public Collection<E> getFullCollectionBlocking() throws InterruptedException {
    synchronized (myLock) {
      myContentKnown.waitForValue(true);
      return copyCurrent();
    }
  }

  @NotNull
  public Collection<E> getWritableCollection() {
    return myCollection.get();
  }

  public CollectionModelEvent<E> createDefaultEvent() {
    return new CollectionModelEvent<E>(this, null);
  }

  public Object afterAddListenerWithLock(ThreadGate threadGate, Consumer<E> consumer, Object passThrough) {
    CollectionModelEvent[] events = new CollectionModelEvent[2];
    events[0] = CollectionModelEvent.create(this, (E[]) myList.toArray(new Object[myList.size()]));
    events[1] = isContentKnown() ? createDefaultEvent() : null;
    return events;
  }

  public void afterAddListenerWithoutLock(ThreadGate threadGate, final Consumer<E> consumer, Object passThrough) {

    final CollectionModelEvent[] events = (CollectionModelEvent[]) passThrough;
    threadGate.execute(new Runnable() {
      public void run() {
        if (events[0].getScalars().length > 0)
          consumer.onScalarsAdded(events[0]);
        if (events[1] != null)
          consumer.onContentKnown(events[1]);
      }
    });
  }

  /**
   * hack: actually, we need to rewrite model to have it based on any underlying collection class, LinkedHashSet in
   * this case
   */
  public boolean addIfNotContains(E element) {
    WrappingCollection c = (WrappingCollection) getWritableCollection();
    return c.add(element, true);
  }


  private class WrappingCollection extends AbstractCollection<E> {
    public int size() {
      synchronized (myLock) {
        return myList.size();
      }
    }

    public Iterator<E> iterator() {
      synchronized (myLock) {
        return new WrappingIterator(myList.iterator());
      }
    }

    public boolean add(E e) {
      return add(e, false);
    }

    private boolean add(E e, boolean onlyIfNotContains) {
      boolean changed = false;
      CollectionModelEvent<E> event = null;
      CollectionModel.Consumer<E> dispatcher = null;
      synchronized (myLock) {
        if (isContentKnown() && !isContentChangeable())
          throw new IllegalStateException("collection is fully known and changes are not allowed");
        if (onlyIfNotContains && myList.contains(e))
          return false;
        changed = myList.add(e);
        if (changed) {
          E[] array = (E[]) new Object[] {e};
          event = new CollectionModelEvent<E>(ArrayListCollectionModel.this, array);
          dispatcher = myEventSupport.getDispatcherSnapshot();
        }
      }
      if (dispatcher != null) {
        dispatcher.onScalarsAdded(event);
        updateCount();
      }
      return changed;
    }

    public boolean isEmpty() {
      synchronized (myLock) {
        return super.isEmpty();
      }
    }

    public boolean contains(Object o) {
      synchronized (myLock) {
        return super.contains(o);
      }
    }

    public <T> T[] toArray(T[] ts) {
      synchronized (myLock) {
        return super.toArray(ts);
      }
    }

    public Object[] toArray() {
      synchronized (myLock) {
        return super.toArray();
      }
    }

    public boolean remove(Object o) {
      synchronized (myLock) {
        if (!isContentKnown())
          throw new IllegalStateException("cannot remove element from not fully known collection");
        return super.remove(o);
      }
    }

    public boolean containsAll(Collection<?> c) {
      synchronized (myLock) {
        return super.containsAll(c);
      }
    }

    public boolean addAll(Collection<? extends E> es) {
      synchronized (myLock) {
        return super.addAll(es);
      }
    }

    public boolean removeAll(Collection<?> c) {
      synchronized (myLock) {
        if (!isContentKnown())
          throw new IllegalStateException("cannot remove element from not fully known collection");
        return super.removeAll(c);
      }
    }

    public boolean retainAll(Collection<?> c) {
      synchronized (myLock) {
        if (!isContentKnown())
          throw new IllegalStateException("cannot remove element from not fully known collection");
        return super.retainAll(c);
      }
    }

    public void clear() {
      synchronized (myLock) {
        if (!isContentKnown())
          throw new IllegalStateException("cannot remove element from not fully known collection");
        super.clear();
      }
    }

    public String toString() {
      synchronized (myLock) {
        return super.toString();
      }
    }
  }

  private class WrappingIterator implements Iterator<E> {
    private final Iterator<E> myDelegate;
    private E myLast;

    public WrappingIterator(Iterator<E> delegate) {
      myDelegate = delegate;
    }

    public boolean hasNext() {
      synchronized (myLock) {
        return myDelegate.hasNext();
      }
    }

    public E next() {
      synchronized (myLock) {
        myLast = myDelegate.next();
        return myLast;
      }
    }

    public void remove() {
      CollectionModelEvent<E> event = null;
      CollectionModel.Consumer<E> dispatcher = null;
      synchronized (myLock) {
        if (!isContentChangeable())
          throw new IllegalStateException("model is not changeable");
        if (!isContentKnown())
          throw new IllegalStateException("cannot remove element from not fully known collection");
        myDelegate.remove();
        E[] array = (E[]) new Object[]{myLast};
        event = CollectionModelEvent.create(ArrayListCollectionModel.this, array);
        dispatcher = myEventSupport.getDispatcherSnapshot();
      }
      if (dispatcher != null) {
        dispatcher.onScalarsRemoved(event);
        updateCount();
      }
    }
  }
}
