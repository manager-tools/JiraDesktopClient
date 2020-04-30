package com.almworks.util.advmodel;

import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.commons.Function;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes from model equal elements.
 */
public class UniqueListDecorator<T> extends AbstractAListModel<T> implements AListModel.Listener<T> {
  private final AListModel<T> mySource;
  private final HashMap<T, Wrapper<T>> myMap = Collections15.hashMap();
  private final List<Wrapper<T>> myOrderedWrappers = Collections15.arrayList();

  public static <T> AListModel<T> create(Lifespan life, AListModel<T> source) {
    UniqueListDecorator<T> result = new UniqueListDecorator<T>(source);
    result.start(life);
    return result;
  }

  private UniqueListDecorator(AListModel<T> source) {
    mySource = source;
  }

  public T getAt(int index) {
    return myOrderedWrappers.get(index).myElement;
  }

  public int getSize() {
    return myOrderedWrappers.size();
  }

  public void onInsert(int index, int length) {
    Threads.assertAWTThread();
    int ins = seek(index);
    for (int i = ins; i < myOrderedWrappers.size(); i++)
      myOrderedWrappers.get(i).mySourceIndex += length;

    for (int k = 0; k < length; k++) {
      int sourceIndex = index + k;
      T element = mySource.getAt(sourceIndex);
      ins += insertElement(element, sourceIndex, ins);
    }
    // todo group events
  }

  public void onRemove(int index, int length, RemovedEvent<T> event) {
    Threads.assertAWTThread();
    // 1. remove corresponding
    int removeStart = seek(index);
    int removeLength = 0;
    List<T> removed = null;
    for (int i = removeStart; i < myOrderedWrappers.size(); i++) {
      Wrapper<T> wrapper = myOrderedWrappers.get(i);
      assert wrapper.mySourceIndex >= index : wrapper + " " + index;
      int offset = wrapper.mySourceIndex - index;
      if (offset >= 0 && offset < length) {
        removeLength++;
        if (removed == null)
          removed = Collections15.arrayList();
        removed.add(wrapper.myElement);
      } else {
        wrapper.mySourceIndex -= length;
      }
    }
    if (removeLength <= 0) {
      return;
    }

    // 1a. fire remove event
    assert removed != null;
    RemovedEvent<T> thisEvent = fireBeforeElementsRemoved(removeStart, removed);
    myOrderedWrappers.subList(removeStart, removeStart + removeLength).clear();
    myMap.keySet().removeAll(removed);
    fireRemoved(thisEvent);

    // 2. reinsert removed if they are found in the rest of the model
    Set<T> removedSet = Collections15.hashSet(removed);
    for (int i = 0; removedSet.size() > 0 && i < mySource.getSize(); i++) {
      T element = mySource.getAt(i);
      if (removedSet.remove(element)) {
        insertElement(element, i, seek(i));
      }
    }
  }

  public void onListRearranged(AListEvent event) {
    Threads.assertAWTThread();
    int low = event.getLowAffectedIndex();
    int high = event.getHighAffectedIndex();
    // our affected elements start index
    int thisLow = seek(low);
    if (thisLow == myOrderedWrappers.size())
      return;
    // our affected elements end index, inclusive
    int thisHigh = seek(high);
    if (thisHigh == myOrderedWrappers.size() || myOrderedWrappers.get(thisHigh).mySourceIndex > high)
      thisHigh--;

    if (thisHigh < thisLow) {
      // all affected elements are missing in our image
      // that means that each affected element has a "duplicate" with a lower index, that is reflected in our image
      assert checkImageContainsAll(mySource.subList(low, high + 1));
      return;
    }

    // 1. make a rearranged array. all changes are within affected range.
    List<Wrapper<T>> rearranged = Collections15.arrayList();
    // this map holds a) wrapper => oldIndex; then b) oldIndex => newIndex
    HashMap indexRearranged = new HashMap();
    for (int imageIndex = thisLow; imageIndex <= thisHigh; imageIndex++) {
      Wrapper<T> wrapper = myOrderedWrappers.get(imageIndex);
      int newSourceIndex = mySource.indexOf(wrapper.myElement, low, high + 1);
      assert newSourceIndex != -1;
      int place = seek(rearranged, newSourceIndex);
      rearranged.add(place, wrapper);
      wrapper.mySourceIndex = newSourceIndex;
      indexRearranged.put(wrapper, imageIndex);
    }

    // 2. replace range with the rearranged array, collection a permutation map
    assert rearranged.size() == thisHigh - thisLow + 1;
    for (int i = 0; i < rearranged.size(); i++) {
      Wrapper<T> wrapper = rearranged.get(i);
      int newIndex = thisLow + i;
      myOrderedWrappers.set(newIndex, wrapper);
      Integer oldIndex = (Integer) indexRearranged.remove(wrapper);
      indexRearranged.put(oldIndex, newIndex);
    }

    fireRearrange(new MyRearrangeEvent(thisLow, thisHigh, indexRearranged));
  }

  public void onItemsUpdated(UpdateEvent event) {
    // todo ?
  }

  private void start(Lifespan life) {
    life.add(mySource.addListener(this));
    if (mySource.getSize() > 0) onInsert(0, mySource.getSize());
  }

  /**
   * Returns offset in myOrderedWrappers where mySourceIndex >= sourceIndex.
   * The result is within [0; myOrderedWrappers.size()].
   */
  private int seek(final int sourceIndex) {
    return seek(myOrderedWrappers, sourceIndex);
  }

  private static <T> int seek(List<Wrapper<T>> list, final int sourceIndex) {
    int r = CollectionUtil.binarySearch(list, new Function<Wrapper<T>, Integer>() {
      public Integer invoke(Wrapper<T> argument) {
        if (argument.mySourceIndex < sourceIndex)
          return -1;
        else if (argument.mySourceIndex > sourceIndex)
          return 1;
        else
          return 0;
      }
    });
    return r >= 0 ? r : -r - 1;
  }

  private boolean checkImageContainsAll(List<T> list) {
    for (T element : list) {
      assert myMap.containsKey(element) : element;
    }
    return true;
  }

  private int insertElement(T element, int sourceIndex, int imageIndex) {
    Wrapper<T> wrapper = myMap.get(element);
    if (wrapper == null) {
      wrapper = new Wrapper<T>(element, sourceIndex);
      myMap.put(element, wrapper);
      myOrderedWrappers.add(imageIndex, wrapper);
      fireInsert(imageIndex, 1);
      return 1;
    } else if (sourceIndex < wrapper.mySourceIndex) {
      int del = seekExisting(wrapper);
      if (imageIndex == del)
        wrapper.mySourceIndex = sourceIndex;
      else {
        assert imageIndex < del : imageIndex + " " + del;
        move(del, imageIndex, sourceIndex, wrapper);
      }
      return 1;
    } else {
      return 0;
    }
  }

  private int seekExisting(Wrapper<?> wrapper) {
    int del = seek(wrapper.mySourceIndex);
    assert del < myOrderedWrappers.size() : wrapper + " " + del;
    return del;
  }

  private void move(int from, int to, int newSourceIndex, Wrapper<?> assumedWrapper) {
    Wrapper<T> removed = myOrderedWrappers.remove(from);
    assert removed == assumedWrapper : removed + " " + assumedWrapper;
    myOrderedWrappers.add(to, removed);
    removed.mySourceIndex = newSourceIndex;
    fireRearrange(new MoveEvent(from, to));
  }


  private static class Wrapper<T> {
    private T myElement;
    private int mySourceIndex;

    public Wrapper(T element, int sourceIndex) {
      myElement = element;
      mySourceIndex = sourceIndex;
    }

    public String toString() {
      return mySourceIndex + ":" + myElement;
    }
  }


  private static class MyRearrangeEvent extends RearrangeEvent {
    private final Map<Integer, Integer> myIndexRearranged;

    public MyRearrangeEvent(int thisLow, int thisHigh, Map<Integer, Integer> indexRearranged, int translation) {
      super(thisLow, thisHigh, translation);
      myIndexRearranged = indexRearranged;
    }

    public MyRearrangeEvent(int thisLow, int thisHigh, Map<Integer, Integer> indexRearranged) {
      this(thisLow, thisHigh, indexRearranged, 0);
    }

    protected int privateGetNewIndex(int oldIndex) {
      Integer newIndex = myIndexRearranged.get(oldIndex);
      if (newIndex != null)
        return newIndex;
      else {
        assert false;
        return oldIndex;
      }
    }

    public AListEvent translateIndex(int diff) {
      return new MyRearrangeEvent(getLowAffectedIndex(), getHighAffectedIndex(), myIndexRearranged, diff);
    }
  }
}
