package com.almworks.util.components;

import com.almworks.util.Pair;
import com.almworks.util.collections.*;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.ObjInt2Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author : Dyoma
 * 
 */
public abstract class SelectionAccessor <T> implements Modifiable {
  private final List<Listener<? super T>> myListeners = Collections15.arrayList(4);
  private final SimpleModifiable mySelectedItemsListeners = new SimpleModifiable();
  private boolean myEventsInhibited = false;
  private static final ObjInt2Procedure<SelectionAccessor<?>> ADD_INTERVAL =
    new ObjInt2Procedure<SelectionAccessor<?>>() {
      @Override
      public void invoke(SelectionAccessor<?> o, int a, int b) {
        o.addSelectedRange(a, b);
      }
    };

  private final ObjInt2Procedure<SelectionAccessor<?>> REMOVE_INTERVAL =
    new ObjInt2Procedure<SelectionAccessor<?>>() {
      @Override
      public void invoke(SelectionAccessor<?> o, int a, int b) {
        o.removeSelectedRange(a, b);
      }
    };

  public Detach addListener(final Listener<? super T> listener) {
    synchronized(myListeners) {
      myListeners.add(listener);
    }
    return Containers.synchonizedRemove(myListeners, listener);
  }

  public void removeListener(Listener<? super T> listener) {
    synchronized(myListeners) {
      myListeners.remove(listener);
    }
  }

  @Nullable
  public abstract T getSelection();

  protected void fireSelectionChanged() {
    if (myEventsInhibited)
      return;
    Listener<? super T>[] listeners;
    synchronized(myListeners) {
      listeners = new Listener[myListeners.size()];
      myListeners.toArray(listeners);
    }
    T selection = getSelection();
    for (Listener<? super T> listener : listeners)
      listener.onSelectionChanged(selection);
  }

  public abstract boolean hasSelection();

  @NotNull
  public abstract List<T> getSelectedItems();

  /**
   * @return element in the selection that corresponds to the "anchor" value in the swing selection models.
   * If anchor element does not belong to the selection, or is missing - returns null.
   */
  @Nullable
  public abstract T getFirstSelectedItem();

  /**
   * @return element in the selection that corresponds to the "lead" value in the swing selection models.
   * If lead element does not belong to the selection, or is missing - returns null.
   */
  @Nullable
  public abstract T getLastSelectedItem();

  /**
   * @return sorted array of selected indexes
   */
  @NotNull
  public abstract int[] getSelectedIndexes();

  /**
   * @return true if item was found and selected.
   */
  public abstract boolean setSelected(T item);

  public void setSelectedIndexes(int[] indices) {
    Threads.assertAWTThread();
    if (indices == null || indices.length == 0) {
      clearSelection();
      return;
    }
    setSelectedIndexes(IntArray.sortedNoDuplicates(indices));
  }

  protected void setSelectedIndexes(IntArray sortedUniquIndexes) {
    if (sortedUniquIndexes.size() == 0) {
      clearSelection();
      return;
    }
    int[] current = getSelectedIndexes();
    Pair<IntArray, IntArray> diff = sortedUniquIndexes.difference(current);
    changeSelectionAt(diff.getFirst(), true);
    changeSelectionAt(diff.getSecond(), false);
  }

  public abstract void setSelectedIndex(int index);

  public abstract boolean isSelected(T item);

  public final void setSelected(@Nullable Collection<? extends T> items) {
    Threads.assertAWTThread();

    if(items == null || items.isEmpty()) clearSelection();
    else if (items.size() == 1) setSelected(items.iterator().next());
    else priSetSelected(items);
  }

  protected void priSetSelected(Collection<? extends T> items) {
    boolean selectionStarted = false;
    //noinspection ConstantConditions
    for (T item : items) {
      if (!selectionStarted) {
        selectionStarted = setSelected(item);
      } else {
        addSelection(item);
      }
    }
  }

  public final void setSelected(T[] items) {
    setSelected(Arrays.asList(items));
  }

  public abstract void selectAll();

  public abstract void clearSelection();

  public abstract void invertSelection();

  public abstract void addSelectedRange(int first, int last);

  public abstract void removeSelectedRange(int first, int last);

  public abstract int getSelectedIndex();

  public void addChangeListener(Lifespan life, ChangeListener listener) {
    life.add(addSelectedItemsListener(listener));
  }

  public void addChangeListener(Lifespan life, final ThreadGate gate, final ChangeListener listener) {
    addChangeListener(life, new ChangeListener() {
      public void onChange() {
        gate.execute(new Runnable() {
          public void run() {
            listener.onChange();
          }
        });
      }
    });
  }

  public Detach addAWTChangeListener(ChangeListener listener) {
    DetachComposite life = new DetachComposite();
    addChangeListener(life, listener);
    return life;
  }

  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    addChangeListener(life, ThreadGate.AWT, listener);
  }

  /**
   * @return true if any element is selected. false means no element can be selected right now
   */
  public abstract boolean ensureSelectionExists();

  public abstract void addSelection(T item);

  public abstract boolean isAllSelected();

  public Detach addSelectedItemsListener(final ChangeListener listener) {
    DetachComposite life = new DetachComposite();
    mySelectedItemsListeners.addChangeListener(life, ThreadGate.STRAIGHT, listener);
    life.add(addListener(new MyChangeListenerAdapter<T>(listener)));
    return life;
  }

  protected void fireSelectedItemsChanged() {
    if (myEventsInhibited)
      return;
    mySelectedItemsListeners.fireChanged();
  }

  public int getSelectedCount() {
    return getSelectedIndexes().length;
  }

  public abstract boolean isSelectedAt(int index);

  public abstract void addSelectionIndex(int index);

  public abstract void removeSelectionAt(int index);

  public abstract void removeSelection(T element);

  public final void removeSelection(List<T> elements) {
    for (T element : elements) {
      removeSelection(element);
    }
  }

  void setEventsInhibited(boolean inhibited) {
    myEventsInhibited = inhibited;
  }

  protected abstract int getElementCount();

  protected abstract T getElement(int index);

  public void selectAll(Condition<? super T> condition) {
    for (int i = 0; i < getElementCount(); i++)
      if (condition.isAccepted(getElement(i)))
        addSelectionIndex(i);
  }

  @NotNull
  public Condition<T> areSelected() {
    return new Condition<T>() {
      @Override
      public boolean isAccepted(T item) {
        return isSelected(item);
      }
    };
  }

  public void updateSelectionAt(IntArray indexes, boolean makeSelected) {
    if (!hasSelection() && !makeSelected) return;
    if (!indexes.isSorted()) indexes = IntArray.sortedNoDuplicates(indexes);
    IntArray changeSelection = new IntArray();
    for (int i = 0; i < indexes.size(); i++) {
      int index = indexes.get(i);
      if (makeSelected != isSelectedAt(index)) changeSelection.add(index);
    }
    changeSelectionAt(changeSelection, makeSelected);
  }

  protected void changeSelectionAt(IntArray sortedIndexes, boolean makeSelected) {
    if (sortedIndexes == null || sortedIndexes.size() == 0) return;
    ObjInt2Procedure<SelectionAccessor<?>> procedure = makeSelected ? ADD_INTERVAL : REMOVE_INTERVAL;
    sortedIndexes.visitSequentialValueIntervals(procedure, this, true);
  }

  public interface Listener <T> {
    void onSelectionChanged(T newSelection);
  }


  private static class MyChangeListenerAdapter<T> implements Listener<T> {
    private final ChangeListener myListener;

    public MyChangeListenerAdapter(ChangeListener listener) {
      myListener = listener;
    }

    public void onSelectionChanged(T newSelection) {
      myListener.onChange();
    }
  }
}
