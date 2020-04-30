package com.almworks.util.advmodel;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Equality;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
public abstract class AROList<T> implements AListModel<T> {
  public Iterator<T> iterator() {
    return toList().iterator();
  }

  public final List<T> getAt(int[] indices) {
    if (indices.length == 0)
      return Collections15.emptyList();
    List<T> result = Collections15.arrayList();
    for (int i = 0; i < indices.length; i++) {
      int index = indices[i];
      result.add(getAt(index));
    }
    return result;
  }

  public final List<T> subList(int fromIndex, int exclusiveToIndex) {
    if (fromIndex >= exclusiveToIndex)
      return Collections15.emptyList();
    return toList().subList(fromIndex, exclusiveToIndex);
  }

  public int detectIndex(Condition<? super T> condition) {
    for (int i = 0; i < getSize(); i++)
      if (condition.isAccepted(getAt(i)))
        return i;
    return -1;
  }

  @Nullable
  public T detect(Condition<? super T> condition) {
    for (int i = 0; i < getSize(); i++) {
      T element = getAt(i);
      if (condition.isAccepted(element))
        return element;
    }
    return null;
  }

  public Detach addListStructureListener(final ChangeListener listener) {
    return addListStructureListener(listener, this);
  }

  static Detach addListStructureListener(final ChangeListener listener, AListModel<?> model) {
    return model.addListener(new AListModel.Listener() {
      public void onInsert(int index, int length) {
        listener.onChange();
      }

      public void onRemove(int index, int length, RemovedEvent event) {
        listener.onChange();
      }

      public void onListRearranged(AListEvent event) {
        listener.onChange();
      }

      public void onItemsUpdated(UpdateEvent event) {
      }
    });
  }

  public void addChangeListener(Lifespan life, final ChangeListener listener) {
    doAddChangeListener(life, listener, this, ThreadGate.STRAIGHT);
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    doAddChangeListener(life, listener, this, gate);
  }

  public Detach addAWTChangeListener(ChangeListener listener) {
    DetachComposite life = new DetachComposite();
    addChangeListener(life, listener);
    return life;
  }

  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.AWT, listener);
  }

  static void doAddChangeListener(Lifespan life, final ChangeListener listener, AListModel<?> model,
    final ThreadGate gate)
  {
    life.add(model.addListener(new ChangeListenerAdapter(gate, listener)));
  }

  public List<T> toList() {
    return new AbstractList<T>() {
      public T get(int index) {
        return getAt(index);
      }

      public int size() {
        return getSize();
      }
    };
  }

  public int indexOf(Object element) {
    return indexOf(element, 0, getSize());
  }

  public int indexOf(T element, Equality<? super T> equality) {
    return indexOf(element, 0, getSize(), equality);
  }

  public int indexOf(Object element, int from, int to) {
    for (int i = from; i < to; i++)
      if (Util.equals(element, getAt(i)))
        return i;
    return -1;
  }

  public int indexOf(T element, int from, int to, Equality<? super T> equality) {
    for (int i = from; i < to; i++)
      if (equality.areEqual(element, getAt(i)))
        return i;
    return -1;
  }

  public int[] indeciesOf(Collection<?> elements) {
    int[] result = new int[elements.size()];
    int index = 0;
    for (Object element : elements) {
      result[index] = indexOf(element);
      index++;
    }
    if (index < elements.size()) {
      int[] tmp = new int[index];
      System.arraycopy(result, 0, tmp, 0, tmp.length);
      result = tmp;
    }
    return result;
  }

  public boolean containsAny(Collection<? extends T> elements) {
    for (int i = 0; i < getSize(); i++) {
      if (elements.contains(getAt(i)))
        return true;
    }
    return false;
  }

  public boolean contains(T element) {
    return indexOf(element) >= 0;
  }

  public static UpdateEvent updateRangeEvent(int index1, int index2) {
    return new DefaultUpdateEvent(index1, index2);
  }

  public static <T> void fireRemove(RemovedElementsListener<T> before, Listener after, List<T> elements,
    int firstIndex)
  {
    if (elements.size() == 0)
      return;
    RemoveNotice<T> notice = RemoveNotice.create(firstIndex, elements);
    before.onBeforeElementsRemoved(notice);
    after.onRemove(firstIndex, elements.size(), notice.createPostRemoveEvent());
  }

  public static class ChangeListenerAdapter implements Listener {
    private final ThreadGate myGate;
    private final ChangeListener myListener;

    public ChangeListenerAdapter(ThreadGate gate, ChangeListener listener) {
      myGate = gate;
      myListener = listener;
    }

    public void onInsert(int index, int length) {
      fireChanged();
    }

    public void onRemove(int index, int length, RemovedEvent event) {
      fireChanged();
    }

    public void onListRearranged(AListEvent event) {
      fireChanged();
    }

    public void onItemsUpdated(UpdateEvent event) {
      fireChanged();
    }

    protected final void fireChanged() {
      if (ThreadGate.isRightNow(myGate))
        myListener.onChange();
      else
        myGate.execute(new Runnable() {
          public void run() {
            myListener.onChange();
          }
        });
    }
  }


  private static class DefaultUpdateEvent extends UpdateEvent {
    public DefaultUpdateEvent(int index0, int index1) {
      super(index0, index1);
    }

    protected boolean privateIsUpdated(int index) {
      return true;
    }

    public DefaultUpdateEvent translateIndex(int diff) {
      return new DefaultUpdateEvent(getLowAffectedIndex() + diff, getHighAffectedIndex() + diff);
    }
  }
}
