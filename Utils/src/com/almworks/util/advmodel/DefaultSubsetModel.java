package com.almworks.util.advmodel;

import com.almworks.integers.*;
import com.almworks.integers.func.IntFunctions;
import com.almworks.integers.func.IntToInt;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.almworks.integers.func.IntFunctions.ADD;
import static com.almworks.integers.func.IntFunctions.apply;
import static org.almworks.util.Collections15.hashSet;

/**
 * If elements are removed from the full set, they do not disappear from the subset.
 * This is useful for editors living on this model: if applicable values (full set) depends on values selected in another editor,
 * model for selected values in the editor living on this model (subset) does not drop these elements in case the user inadvertently changes the dependancy editor value.
 * <br/>
 * Also, this model allows for reordering of subset model elements, which prevents it from updating element order in case elements in full set are reordered.
 * @author dyoma
 */
// TODO This issue should be addressed by splitting this class in two:
// TODO one that directly reflects changes from the underlying full set (no detached elements: they are removed immediately, user cannot reorder subset elements),
// TODO and another with the current behaviour
public class DefaultSubsetModel<T> extends SubsetModel<T> {
  private final OrderListModel<T> mySubset = new OrderListModel<T>();
  private final ImageBasedDecorator<T, T> myComplementSet;
  private final IntTwoWayMap myFullSetToSubsetIdxs = new IntTwoWayMap();
  /** Contains elements that are retained in the subset but removed from the full set. Invariant: {@link #myFullSetToSubsetIdxs} does not contain indexes for detached elements.*/
  private final Set<T> myDetachedElements = hashSet();

  public DefaultSubsetModel(Lifespan lifespan, AListModel<? extends T> fullSet, final boolean byDefaultInSubset) {
    myComplementSet = new ImageBasedDecorator.ComplemetaryListModel<T>(fullSet, mySubset) {
      public int addNewItems(int sourceIndex, int length, int firstImageIndex) {
        if (!byDefaultInSubset) {
          return super.addNewItems(sourceIndex, length, firstImageIndex);
        } else {
          add0(IntProgression.arithmetic(sourceIndex, length), getSource().subList(sourceIndex, sourceIndex + length));
          return firstImageIndex;
        }
      }
    };

    if (byDefaultInSubset) {
      setFull();
    }
    lifespan.add(myComplementSet.getDetach());
    lifespan.add(fullSet.addListener(new SubsetElementsUpdater()));
  }

  private void add0(int fullSetIdx, @Nullable T element) {
    if (element == null) element = getFullSet().getAt(fullSetIdx);
    if (!myFullSetToSubsetIdxs.containsKey(fullSetIdx)) {
      int subsetIdx = mySubset.addElement(element);
      myFullSetToSubsetIdxs.put(fullSetIdx, subsetIdx);
    } else {
      int oldSubsetIdx = myFullSetToSubsetIdxs.get(fullSetIdx);
      if (mySubset.getAt(oldSubsetIdx) != element)
        mySubset.replaceAt(oldSubsetIdx, element);
    }
  }

  private void addDetached(T element) {
    mySubset.addElement(element);
    myDetachedElements.add(element);
  }

  private void add0(IntList fullSetIdxs, @Nullable List<T> fullSetElements) {
    int sz = fullSetIdxs.size();
    if (fullSetElements != null && sz != fullSetElements.size()) {
      LogHelper.error(fullSetIdxs, fullSetElements);
      sz = Math.min(sz, fullSetElements.size());
    }
    for (int i = 0; i < sz; ++i) {
      add0(fullSetIdxs.get(i), fullSetElements != null ? fullSetElements.get(i) : null);
    }
  }

  private void add0(Collection<T> fullSetElements) {
    AListModel<T> fullSet = getFullSet();
    for (T element : fullSetElements) {
      int fullSetIdx = fullSet.indexOf(element);
      if (fullSetIdx >= 0) add0(fullSetIdx, element);
      else addDetached(element);
    }
  }

  private void ins0(final int subsetIdx, T element) {
    mySubset.insert(subsetIdx, element);
    // Shift all subset indexes after the inserted one (important - before adding the index to the map)
    myFullSetToSubsetIdxs.transformValues(subsetIdx, apply(ADD, 1));
    int fullSetIdx = getFullSet().indexOf(element);
    if (fullSetIdx >= 0) {
      int oldSubsetIdx = myFullSetToSubsetIdxs.put(fullSetIdx, subsetIdx);
      LogHelper.assertError(subsetIdx == oldSubsetIdx || element == null, "Added element is already in the subset", element, "full set index:", fullSetIdx, "new subset index:", subsetIdx, "old subset index:", oldSubsetIdx);
    } else {
      myDetachedElements.add(element);
    }
  }

  public void setFull() {
    AListModel<? extends T> fullSet = myComplementSet.getSource();
    mySubset.clear();
    myDetachedElements.clear();
    myFullSetToSubsetIdxs.clear();
    List<? extends T> elements = fullSet.toList();
    mySubset.addAll(elements);
    IntProgression idxs = IntProgression.arithmetic(0, elements.size());
    myFullSetToSubsetIdxs.insertAllRo(idxs, idxs);
  }

  protected AListModel<T> getImageModel() {
    return mySubset;
  }

  public AListModel<T> getComplementSet() {
    return myComplementSet;
  }

  public AListModel<T> getFullSet() {
    return myComplementSet.getSource();
  }

  public void addFromComplementSet(List<T> items) {
    add0(items);
    myComplementSet.resynch();
  }

  public void insertFromComplementSet(int index, T item) {
    ins0(index, item);
    myComplementSet.resynch();
  }

  public void swap(int index1, int index2) {
    mySubset.swap(index1, index2);
    myFullSetToSubsetIdxs.transformValues(IntFunctions.swap(index1, index2));
  }

  public void add(T item) {
    add0(Collections.singleton(item));
    myComplementSet.resynch();
  }

  public void setSubset(Collection<T> items) {
    assert checkItems(items);
    mySubset.clear();
    myFullSetToSubsetIdxs.clear();
    myDetachedElements.clear();
    add0(items);
    myComplementSet.resynch();
  }

  private boolean checkItems(Collection<T> items) {
    AListModel<T> fullSet = getFullSet();
    for (T item : items) {
      assert fullSet.indexOf(item) >= 0 : item;
    }
    return true;
  }

  public void addFromFullSet(int... indices) {
    add0(new IntArray(indices), null);
    myComplementSet.resynch();
  }

  public void removeAllAt(int... indices) {
    // First remove from the map because we need to locate detached elements while they are in the subset
    IntList detached = myFullSetToSubsetIdxs.removeAllValues(new IntArray(indices));
    for (IntIterator it = detached.iterator(); it.hasNext();) {
      int idx = it.nextValue();
      T element = mySubset.getAt(idx);
      boolean wasDetached = myDetachedElements.remove(element);
      if (!wasDetached) LogHelper.warning("Removed element: neither in full set, nor detached", element, idx, myDetachedElements, "\n", mySubset, "\n", getFullSet(), "\n", myFullSetToSubsetIdxs);
    }
    mySubset.removeAll(indices);
    ensureSubsetIdxsAreDense();
    myComplementSet.resynch();
  }

  @Override
  public void removeAll(List<T> items) {
    if (items == null) return;
    mySubset.removeAll(items);
    IntArray idxs = new IntArray(items.size());
    AListModel<T> fullSet = getFullSet();
    for (T item : items) {
      int idx = fullSet.indexOf(item);
      if (idx >= 0) {
        idxs.add(idx);
      } else {
        boolean wasDetached = myDetachedElements.remove(item);
        if (!wasDetached) LogHelper.warning(wasDetached, "Removed element: neither in full set nor detached", item, fullSet, "\n", myDetachedElements, "\n", myFullSetToSubsetIdxs);
      }
    }
    myFullSetToSubsetIdxs.removeAll(idxs);
    ensureSubsetIdxsAreDense();
    myComplementSet.resynch();
  }

  @Override
  public void removeAll(@NotNull final Condition<? super T> which) {
    final IntArray fullIdxRemoved = new IntArray();
    final AListModel<T> fullSet = getFullSet();
    mySubset.removeAll(new Condition<T>() {
      @Override
      public boolean isAccepted(T value) {
        if (!which.isAccepted(value)) return false;
        int fullIdx = fullSet.indexOf(value);
        if (fullIdx >= 0) {
          fullIdxRemoved.add(fullIdx);
        } else {
          boolean wasDetached = myDetachedElements.remove(value);
          if (!wasDetached) LogHelper.warning(wasDetached, "Removed element: neither in the full set nor detached", value, fullSet, myDetachedElements, "\n", myFullSetToSubsetIdxs);
        }
        return true;
      }
    });
    myFullSetToSubsetIdxs.removeAll(fullIdxRemoved);
    ensureSubsetIdxsAreDense();
    myComplementSet.resynch();
  }

  private void ensureSubsetIdxsAreDense() {
    // Using the fact that function is called with ascending order values
    myFullSetToSubsetIdxs.transformValues(new IntToInt() {
      int newSubsetIdx = 0;

      @Override
      public int invoke(int oldSubsetIdx) {
        for (; myDetachedElements.contains(mySubset.getAt(newSubsetIdx)); ++newSubsetIdx)
          ;
        return newSubsetIdx++;
      }
    });
  }

  public void setSubsetIndices(int... originalIndices) {
    AListModel<T> fullSet = getFullSet();
    if (mySubset.getSize() == originalIndices.length) {
      boolean changed = false;
      for (int i = 0; i < originalIndices.length; i++) {
        int index = originalIndices[i];
        if (mySubset.getAt(i) == fullSet.getAt(index)) {
          if (myFullSetToSubsetIdxs.get(index) == i)
            continue;
          else
            myFullSetToSubsetIdxs.put(index, i);
        }
        changed = true;
      }
      if (!changed) return;
    }
    mySubset.clear();
    myFullSetToSubsetIdxs.clear();
    myDetachedElements.clear();
    addFromFullSet(originalIndices);
  }

  private class SubsetElementsUpdater implements Listener<T> {
    @Override
    public void onInsert(final int index, final int length) {
      // Fix full set indexes
      myFullSetToSubsetIdxs.transformKeys(new IntToInt() {
        @Override
        public int invoke(int oldFullSetIdx) {
          return oldFullSetIdx >= index ? oldFullSetIdx + length : oldFullSetIdx;
        }
      });
      AListModel<T> fullSet = getFullSet();
      // Maybe a detached element has been inserted into the model, update indexes then.
      for (int fullSetIdx = index; fullSetIdx < index + length; ++fullSetIdx) {
        T element = fullSet.getAt(fullSetIdx);
        if (myDetachedElements.remove(element)) {
          int subsetIdx = mySubset.indexOf(element);
          if (subsetIdx >= 0) {
            LogHelper.assertError(!myFullSetToSubsetIdxs.containsValue(subsetIdx), "detached element still in the map", element, subsetIdx, fullSetIdx);
            int oldSub = myFullSetToSubsetIdxs.put(fullSetIdx, subsetIdx);
            LogHelper.assertError(oldSub == subsetIdx, "replaced mapping for previously detached element", element, oldSub, subsetIdx, fullSetIdx);
          } else LogHelper.error("Detached element not in subset:", element, mySubset.toList());
        }
      }
    }

    @Override
    public void onRemove(final int index, final int length, RemovedEvent<T> event) {
      for (int i = 0; i < length; ++i) {
        int fullSetIdx = index + i;
        if (myFullSetToSubsetIdxs.containsKey(fullSetIdx)) {
          myFullSetToSubsetIdxs.remove(fullSetIdx);
        }
        T element = event.getElement(i);
        if (mySubset.contains(element)) {
          boolean alreadyDetached = !myDetachedElements.add(element);
          LogHelper.assertError(!alreadyDetached, element, fullSetIdx, getFullSet().toList(), mySubset.toList(), myFullSetToSubsetIdxs);
        }
      }
      // Fix full set indexes
      myFullSetToSubsetIdxs.transformKeys(new IntToInt() {
        @Override
        public int invoke(int oldFullSetIdx) {
          return oldFullSetIdx >= index + length ? oldFullSetIdx - length : oldFullSetIdx;
        }
      });
    }

    @Override
    public void onListRearranged(final AListEvent event) {
      // We could rearrange the subset accordingly, but we allow to swap subset model elements, so we should avoid ruining user-defined order (see class javadoc)
      try {
        myFullSetToSubsetIdxs.transformKeys(new IntToInt() {
          @Override
          public int invoke(int oldFullSetIdx) {
            return event.getNewIndex(oldFullSetIdx);
          }
        });
      } catch (IntTwoWayMap.NonInjectiveFunctionException ex) {
        LogHelper.error("Rearrange indexes: same new index " + ex.getDuplicateValue(), ex);
      }
    }

    @Override
    public void onItemsUpdated(UpdateEvent event) {
      AListModel<T> fullSet = getFullSet();
      for (int fullSetIdx = event.getLowAffectedIndex(); fullSetIdx <= event.getHighAffectedIndex(); ++fullSetIdx) {
        if (myFullSetToSubsetIdxs.containsKey(fullSetIdx)) {
          mySubset.replaceAt(myFullSetToSubsetIdxs.get(fullSetIdx), fullSet.getAt(fullSetIdx));
        }
      }
    }
  }
}
