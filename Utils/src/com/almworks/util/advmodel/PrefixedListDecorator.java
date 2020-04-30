package com.almworks.util.advmodel;

import com.almworks.util.TODO;
import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Detach;

/**
 * Prepends AListModel with a fixed number of elements
 * @deprecated should be replaced with {@link SegmentedListModel}
 */
@Deprecated
public class PrefixedListDecorator<T> extends AROList<T> {
  private final T[] myPrefixElements;
  private final AListModel<T> mySource;
  private final int myPrefix;

  public PrefixedListDecorator(T[] prefixElements, AListModel<T> source) {
    assert prefixElements != null;
    assert prefixElements.length > 0;
    mySource = source;
    myPrefixElements = (T[]) prefixElements.clone();
    myPrefix = myPrefixElements.length;
  }

  public static <T> PrefixedListDecorator<T> create(T prefix, AListModel<T> model) {
    return new PrefixedListDecorator<T>((T[])new Object[] {prefix}, model);
  }

  public Detach addListener(final Listener<? super T> listener) {
    return mySource.addListener(new MyAdjustedListener(myPrefix, listener));
  }


  public void removeFirstListener(final Condition<Listener> condition) {
    mySource.removeFirstListener(new Condition<Listener>() {
      public boolean isAccepted(Listener value) {
        if (!(value instanceof MyAdjustedListener))
          return false;
        return condition.isAccepted(((MyAdjustedListener) value).myListener);
      }
    });
  }

  public Detach addRemovedElementListener(final RemovedElementsListener<T> listener) {
    return mySource.addRemovedElementListener(new RemovedElementsListener<T>() {
      public void onBeforeElementsRemoved(RemoveNotice<T> elements) {
        listener.onBeforeElementsRemoved(elements.translateIndex(myPrefix));
      }
    });
  }

  public T getAt(int index) {
    if (index < 0)
      throw new IndexOutOfBoundsException(this + "; " + index);
    if (index < myPrefix)
      return myPrefixElements[index];
    else
      return mySource.getAt(index - myPrefix);
  }

  public int getSize() {
    return mySource.getSize() + myPrefix;
  }

  public void forceUpdateAt(int index) {
    if (index >= myPrefix)
      mySource.forceUpdateAt(index - myPrefix);
    else
      throw TODO.notImplementedYet();
  }

  private static class MyAdjustedListener implements Listener {
    private final int myPrefix;
    private final Listener myListener;

    public MyAdjustedListener(int prefix, Listener listener) {
      myPrefix = prefix;
      myListener = listener;
    }

    public void onInsert(int index, int length) {
      myListener.onInsert(myPrefix + index, length);
    }

    public void onRemove(int index, int length, RemovedEvent event) {
      myListener.onRemove(myPrefix + index, length, event.translateIndex(myPrefix));
    }

    public void onListRearranged(AListEvent event) {
      myListener.onListRearranged(event.translateIndex(myPrefix));
    }

    public void onItemsUpdated(UpdateEvent event) {
      myListener.onItemsUpdated(event.translateIndex(myPrefix));
    }
  }
}
