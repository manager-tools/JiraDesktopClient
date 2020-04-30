package com.almworks.util.advmodel;

import com.almworks.util.collections.*;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.ObjInt2Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author : Dyoma
 */
public class OrderListModel<T> extends AbstractAListModel<T> {
  private final List<T> myElements = Collections15.arrayList();
  public static final ObjInt2Procedure<OrderListModel<?>> REMOVE_RANGE = new ObjInt2Procedure<OrderListModel<?>>() {
    @Override
    public void invoke(OrderListModel<?> model, int a, int b) {
      model.removeRange(a, b);
    }
  };

  public OrderListModel() {
    assert addAssertThreadListener(this);
  }

  public OrderListModel(List<? extends T> elements) {
    this();
    myElements.addAll(elements);
  }

  @ThreadAWT
  public void insert(int index, T element) {
    Threads.assertAWTThread();
    myElements.add(index, element);
    fireInsert(index, 1);
  }

  public void insertAll(int index, Collection<? extends T> elements) {
    Threads.assertAWTThread();
    myElements.addAll(index, elements);
    fireInsert(index, elements.size());
  }

  public void insertAll(int index, T... elements) {
    insertAll(index, Arrays.asList(elements));
  }

  public void swap(final int index1, final int index2) {
    Threads.assertAWTThread();
    if (index1 == index2)
      return;
    Collections.swap(myElements, index1, index2);
    fireRearrange(new SwapEvent(index1, index2));
  }

  public int getSize() {
    return myElements.size();
  }

  public T getAt(int index) {
    return myElements.get(index);
  }

  @ThreadAWT
  public int addElement(T element) {
    int size = getSize();
    insert(size, element);
    return size;
  }

  public int findPlace(T element, Comparator<? super T> comparator) {
    int index = Collections.binarySearch(myElements, element, comparator);
    return index >= 0 ? index : -index - 1;
  }

  public int addElement(T element, @NotNull Comparator<? super T> comparator) {
    int place = findPlace(element, comparator);
    insert(place, element);
    return place;
  }

  public void removeAll(Condition<? super T> condition) {
    int inilialSize = myElements.size();
    BitSet toRemove = new BitSet(inilialSize + 1);
    for (int i = 0; i < inilialSize; i++) {
      T t = myElements.get(i);
      if (condition.isAccepted(t))
        toRemove.set(i);
    }
    int index = 0;
    int alreadyRemoved = 0;
    while (index < inilialSize) {
      index = toRemove.nextSetBit(index);
      if (index == -1)
        break;
      int lastIndex = toRemove.nextClearBit(index);
      assert lastIndex != -1;
      lastIndex--;
      assert lastIndex >= index;
      removeRange(index - alreadyRemoved, lastIndex - alreadyRemoved);
      alreadyRemoved += lastIndex - index + 1;
      index = lastIndex + 1;
    }
  }

  public void removeRange(int firstIndex, int lastIndex) {
    assert lastIndex >= firstIndex;
    Threads.assertAWTThread();
    firstIndex = Math.max(0, firstIndex);
    lastIndex = Math.min(myElements.size() - 1, lastIndex);
    if (firstIndex > lastIndex)
      return;
    final List<T> list = myElements.subList(firstIndex, lastIndex + 1);
    RemovedEvent<T> event = fireBeforeElementsRemoved(firstIndex, list);
    list.clear();
    fireRemoved(event);
  }

  public void sort(Comparator<? super T> comparator) {
    sort(comparator, CollectionSortPolicy.DEFAULT);
  }

  public void sort(Comparator<? super T> comparator, CollectionSortPolicy policy) {
    final List<T> oldCopy = Collections15.unmodifiableListCopy(myElements);
    policy.sort(myElements, comparator);
    fireRearrange(SortListEvent.create(this, oldCopy));
  }

  public void sort(int index0, int index1, Comparator<? super T> comparator) {
    sort(index0, index1, comparator, CollectionSortPolicy.DEFAULT);
  }

  public void sort(int index0, int index1, Comparator<? super T> comparator, CollectionSortPolicy policy) {
    final List<T> sublist = sublist(index0, index1);
    final List<T> oldCopy = Collections15.unmodifiableListCopy(sublist);
    policy.sort(sublist, comparator);
    fireRearrange(new SortSublistEvent(index0, index1, sublist, oldCopy, 0));
  }

  public void ensureSorted(Comparator<? super T> comparator) {
    ensureSorted(comparator, CollectionSortPolicy.DEFAULT);
  }

  public void ensureSorted(Comparator<? super T> comparator, CollectionSortPolicy policy) {
    if (Containers.isOrderValid(myElements, comparator))
      return;
    sort(comparator, policy);
  }

  public void sortOrUpdate(int index0, int index1, Comparator<? super T> comparator, CollectionSortPolicy policy) {
    List<T> sublist = sublist(index0, index1);
    if (Containers.isOrderValid(sublist, comparator))
      updateRange(index0, index1);
    else
      sort(index0, index1, comparator, policy);
  }

  private List<T> sublist(int index0, int index1) {
    int minIndex = Math.min(index0, index1);
    int maxIndex = Math.max(index0, index1);
    return myElements.subList(minIndex, maxIndex + 1);
  }

  public T removeAt(int index) {
    assert index >= 0 && index < myElements.size() : index + " " + myElements.size();
    T element = myElements.get(index);
    removeRange(index, index);
    return element;
  }

  public void addAll(Collection<? extends T> list) {
    Threads.assertAWTThread();
    if (list == null || list.isEmpty()) return;
    int size = myElements.size();
    myElements.addAll(list);
    fireInsert(size, list.size());
  }

  public void addAll(T... items) {
    if (items == null || items.length == 0) return;
    addAll(Arrays.asList(items));
  }

  public int indexOf(Object value) {
    //noinspection SuspiciousMethodCalls
    return myElements.indexOf(value);
  }

  public int indexOf(T element, Equality<? super T> equality) {
    return indexOf(element, 0, getSize(), equality);
  }

  public int indexOf(T element, int from, int to, Equality<? super T> equality) {
    for (int i = from; i < to; i++)
      if (equality.areEqual(element, myElements.get(i)))
        return i;
    return -1;
  }

  public void removeAll(int[] indices) {
    if (indices == null || indices.length == 0) return;
    if (indices.length == 1) {
      removeAt(indices[0]);
      return;
    }
    IntArray idx = IntArray.create(indices);
    idx.sort();
    idx.removeSubsequentDuplicates();
    idx.visitSequentialValueIntervals(REMOVE_RANGE, this, false);
  }

  public void clear() {
    Threads.assertAWTThread();
    if (getSize() == 0)
      return;
    removeRange(0, getSize() - 1);
  }

  public static <T> OrderListModel<T> create() {
    return new OrderListModel<T>();
  }

  public static <T> OrderListModel<T> create(List<? extends T> elements) {
    return new OrderListModel<T>(elements);
  }

  public static <T> OrderListModel<T> create(T... elements) {
    return new OrderListModel<T>(Arrays.asList(elements));
  }

  public List<T> toList() {
    return new AbstractList<T>() {
      public T get(int index) {
        return getAt(index);
      }

      public int size() {
        return getSize();
      }

      public void add(int index, T element) {
        insert(index, element);
      }

      public T remove(int index) {
        return removeAt(index);
      }
    };
  }

  public void setElements(Collection<? extends T> elements) {
    clear();
    addAll(elements);
  }

  public boolean remove(T item) {
    int index = indexOf(item);
    if (index < 0) return false;
    removeAt(index);
    return true;
  }

  public Detach listenElement(final Modifiable element) {
    return element.addAWTChangeListener(new ChangeListener() {
      public void onChange() {
        int index = indexOf(element);
        assert index >= 0 : String.valueOf(element);
        if (index >= 0)
          updateAt(index);
      }
    });
  }

  public Detach listenElements(Collection<? extends Modifiable> elements) {
    DetachComposite detach = new DetachComposite();
    for (Modifiable modifiable : elements) {
      detach.add(listenElement(modifiable));
    }
    return detach;
  }

  public void updateElement(T element) {
    int index = indexOf(element);
    assert index != -1 : index;
    if (index < 0)
      return;
    updateAt(index);
  }

  public void replaceElementsSet(Collection<? extends T> elements) {
    Set<T> elementsSet = Collections15.linkedHashSet(elements);
    int i = 0;
    while (i < getSize()) {
      T ownElement = getAt(i);
      if (!elementsSet.contains(ownElement)) {
        removeAt(i);
      } else {
        elementsSet.remove(ownElement);
        i++;
      }
    }
    addAll(elementsSet);
  }

  public T replaceAt(int index, T element) {
    T old = myElements.set(index, element);
    updateAt(index);
    return old;
  }

  public T replaceAt_NoFire(int index, T element) {
    return myElements.set(index, element);
  }

  public void removeAll(Collection<?> elements) {
    IntArray removeIndecies = new IntArray(elements.size());
    for (Object t : elements) {
      int index = indexOf(t);
      if (index != -1)
        removeIndecies.add(index);
    }
    if (removeIndecies.size() == 0)
      return;
    removeAll(removeIndecies.toNativeArray());
  }

  public void removeAll(T[] elements) {
    removeAll(Arrays.asList(elements));
  }

  private static boolean addAssertThreadListener(AListModel<?> model) {
    model.addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, new Adapter() {
      public void onChange() {
        Threads.assertAWTThread();
      }
    });
    model.addRemovedElementListener(new RemovedElementsListener() {
      public void onBeforeElementsRemoved(RemoveNotice elements) {
        Threads.assertAWTThread();
      }
    });
    return true;
  }

  public void rearrange(int fromIndex, List<T> rearranged) {
    int length = rearranged.size();
    List<T> sublist = myElements.subList(fromIndex, fromIndex + length);
    List<T> oldSublist = Collections15.unmodifiableListCopy(sublist);
    assert Util.equals(Collections15.hashSet(oldSublist), Collections15.hashSet(rearranged)) :
      "rearranged not equal " + oldSublist + " " + rearranged;
    for (int i = 0; i < rearranged.size(); i++) {
      sublist.set(i, rearranged.get(i));
    }
    fireRearrange(new SortSublistEvent(fromIndex, fromIndex + length - 1, rearranged, oldSublist, 0));
  }

  private class SortSublistEvent extends RearrangeEvent {
    private final List<T> mySublist;
    private final List<T> myOldCopy;

    public SortSublistEvent(int index0, int index1, List<T> sublist, List<T> oldCopy, int translateIndex) {
      super(index0, index1, translateIndex);
      mySublist = sublist;
      myOldCopy = oldCopy;
    }

    protected int privateGetNewIndex(int oldIndex) {
      int shift = getLowAffectedIndex() - getTranslated();
      return shift + mySublist.indexOf(myOldCopy.get(oldIndex - shift));
    }

    public AListEvent translateIndex(int diff) {
      return new SortSublistEvent(getLowAffectedIndex(), getHighAffectedIndex(), mySublist, myOldCopy, diff);
    }
  }


  private static class SortListEvent<T> extends RearrangeEvent {
    private final List<T> myOldCopy;
    private final List<T> myElements;

    protected SortListEvent(int index0, int index1, List<T> oldCopy, List<T> elements) {
      super(index0, index1);
      myOldCopy = oldCopy;
      myElements = elements;
    }

    @Nullable
    public static <T> SortListEvent<T> create(OrderListModel<T> model, List<T> oldCopy) {
      int size = model.getSize();
      if (size == 0)
        return null;
      else
        return new SortListEvent<T>(0, size - 1, oldCopy, model.myElements);
    }

    private SortListEvent(int index0, int index1, List<T> oldCopy, List<T> elements, int translateIndex) {
      super(index0, index1, translateIndex);
      myOldCopy = oldCopy;
      myElements = elements;
    }

    protected int privateGetNewIndex(int oldIndex) {
      if (oldIndex >= 0 && oldIndex < myOldCopy.size()) {
        T element = myOldCopy.get(oldIndex);
        return myElements.indexOf(element);
      } else {
        return -1;
      }
    }

    public AListEvent translateIndex(int diff) {
      return new SortListEvent(getLowAffectedIndex(), getHighAffectedIndex(), myOldCopy, myElements, diff);
    }
  }
}
