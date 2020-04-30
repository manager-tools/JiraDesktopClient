package com.almworks.util.advmodel;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Equality;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author : Dyoma
 */

@ThreadAWT
public interface AListModel<T> extends Modifiable, Iterable<T> {
  AListModel EMPTY = new EmptyListModel();

  static <T> AListModel<T> empty() {
    //noinspection unchecked
    return EMPTY;
  }

  Detach addListener(Listener<? super T> listener);

  Detach addListStructureListener(ChangeListener listener);

  Detach addRemovedElementListener(RemovedElementsListener<T> listener);

  int getSize();

  T getAt(int index);

  List<T> getAt(int[] indices);

  /**
   * @param exclusiveToIndex EXCLUSIVE
   */
  List<T> subList(int fromIndex, int exclusiveToIndex);

  int detectIndex(Condition<? super T> condition);

  @Nullable
  T detect(Condition<? super T> condition);

  List<T> toList();

  int indexOf(Object element);

  int indexOf(T element, Equality<? super T> equality);

  /**
   * @param to exclusive
   * @return -1 if not found
   */
  int indexOf(Object element, int from, int to);

  int indexOf(T element, int from, int to, Equality<? super T> equality);

  boolean containsAny(Collection<? extends T> elements);

  int[] indeciesOf(Collection<?> elements);

  boolean contains(T element);

  void removeFirstListener(Condition<Listener> condition);

  void forceUpdateAt(int index);

  interface Listener<T> {
    /**
     * @param index  insertion index
     * @param length greater than 0
     */
    void onInsert(int index, int length);

    void onRemove(int index, int length, RemovedEvent<T> event);

    void onListRearranged(AListEvent event);

    void onItemsUpdated(UpdateEvent event);
  }


  class RemoveNotice<T> {
    private final int myFirstIndex;
    private final List<? extends T> myElementsToRemove;
    private final Map<TypedKey, Object> myUserData;

    private RemoveNotice(List<? extends T> elementsToRemove, Map<TypedKey, Object> userData, int firstIndex) {
      myElementsToRemove = elementsToRemove;
      myUserData = userData;
      myFirstIndex = firstIndex;
    }

    public <T> void putUserData(@NotNull TypedKey<T> key, T value) {
      key.putTo(myUserData, value);
    }

    public <R> RemoveNotice<R> convertNotice(Convertor<T, R> convertor) {
      return new RemoveNotice<R>(collectElements(convertor), myUserData, myFirstIndex);
    }

    public <R> List<R> collectElements(Convertor<T, R> convertor) {
      return convertor.collectList(myElementsToRemove);
    }

    public List<T> getList() {
      return Collections.unmodifiableList(myElementsToRemove);
    }

    public int getLength() {
      return myElementsToRemove.size();
    }

    public RemovedEvent createPostRemoveEvent() {
      return new RemovedEvent(myFirstIndex, getLength(), myUserData, myElementsToRemove);
    }

    public static <T> RemoveNotice<T> create(int firstIndex, List<? extends T> toRemove) {
      assert toRemove.size() > 0;
      Map<TypedKey, Object> userData = Collections15.hashMap();
      return new RemoveNotice<T>(Collections15.arrayList(toRemove), userData, firstIndex);
    }

    public int getFirstIndex() {
      return myFirstIndex;
    }

    public RemoveNotice<T> translateIndex(int diff) {
      return new RemoveNotice<T>(myElementsToRemove, myUserData, myFirstIndex + diff); 
    }
  }


  class RemovedEvent<T> {
    private final int myFirstIndex;
    private final int myLength;
    private final Map<TypedKey, Object> myUserData;
    private final List<T> myRemoved;

    private RemovedEvent(int firstIndex, int length, Map<TypedKey, Object> userData, List<T> removed) {
      assert firstIndex >= 0 : firstIndex;
      assert length > 0;
      myFirstIndex = firstIndex;
      myLength = length;
      myUserData = userData;
      myRemoved = removed;
    }

    public final <T> T getUserData(@NotNull TypedKey<T> key) {
      //noinspection ConstantConditions
      return key.getFrom(myUserData);
    }

    public final int getFirstIndex() {
      return myFirstIndex;
    }

    public final int getLength() {
      return myLength;
    }

    public final int getLastIndex() {
      return myFirstIndex + myLength - 1;
    }

    public final boolean isInRange(int index) {
      return myFirstIndex <= index && index <= getLastIndex();
    }

    public T getElement(int index) {
      return myRemoved.get(index);
    }

    public RemovedEvent<T> translateIndex(int diff) {
      return new RemovedEvent<T>(myFirstIndex + diff, myLength, myUserData, myRemoved);
    }

    public List<T> getAllRemoved() {
      return myRemoved;
    }
  }


  public abstract class Adapter<T> implements Listener<T>, ChangeListener {
    public void onInsert(int index, int length) {
      onChange();
    }

    public void onRemove(int index, int length, RemovedEvent<T> event) {
      onChange();
    }

    public void onListRearranged(AListEvent event) {
      onChange();
    }

    public void onItemsUpdated(UpdateEvent event) {
      onChange();
    }

    public void onChange() {

    }
  }


  interface RemovedElementsListener<T> {
    void onBeforeElementsRemoved(RemoveNotice<T> elements);
  }


  abstract class AListEvent {
    private final int myLowIndex;
    private final int myHighIndex;

    protected AListEvent(int index0, int index1) {
      myLowIndex = Math.min(index0, index1);
      myHighIndex = Math.max(index0, index1);
      assert myLowIndex <= myHighIndex : myLowIndex + " " + myHighIndex;
      assert myLowIndex >= 0 : myLowIndex + " " + myHighIndex;
      assert myHighIndex >= 0 : myLowIndex + " " + myHighIndex;
    }

    public final int getLowAffectedIndex() {
      return myLowIndex;
    }

    public final int getHighAffectedIndex() {
      return myHighIndex;
    }

    public final int getAffectedLength() {
      return myHighIndex - myLowIndex + 1;
    }

    public final boolean isAffected(int index) {
      if (index < getLowAffectedIndex())
        return false;
      else if (index > getHighAffectedIndex())
        return false;
      return true;
    }

    public abstract int getNewIndex(int oldIndex);

    public abstract AListEvent translateIndex(int diff);
  }


  abstract class UpdateEvent extends AListEvent {
    protected UpdateEvent(int index0, int index1) {
      super(index0, index1);
    }

    public final boolean isUpdated(int index) {
      if (!isAffected(index))
        return false;
      return privateIsUpdated(index);
    }

    public final int getNewIndex(int oldIndex) {
      return oldIndex;
    }

    public <T> List<T> collectUpdated(AListModel<? extends T> model) {
      int max = Math.min(getHighAffectedIndex(), model.getSize() - 1);
      List<T> result = Collections15.arrayList();
      for (int i = getLowAffectedIndex(); i <= max; i++) {
        if (!isUpdated(i))
          continue;
        result.add(model.getAt(i));
      }
      return result;
    }

    protected abstract boolean privateIsUpdated(int index);

    public abstract UpdateEvent translateIndex(int diff);
  }

  abstract class RearrangeEvent extends AListEvent {
    private final int myTranslateIndex;

    protected RearrangeEvent(int index0, int index1) {
      this(index0, index1, 0);
    }

    protected RearrangeEvent(int index0, int index1, int translateIndex) {
      super(index0 + translateIndex, index1 + translateIndex);
      myTranslateIndex = translateIndex;
    }

    /**
     * @deprecated This method is to fix bugs with index translation: if you encounter problems in privateGetNewIndex(), substract the value returned by this method from {@link #getLowAffectedIndex()} and {@link #getHighAffectedIndex()}
     */
    @Deprecated
    protected int getTranslated() {
      return myTranslateIndex; // HACK
    }

    public final int getNewIndex(int oldIndex) {
      if (!isAffected(oldIndex))
        return oldIndex;
      int newIndex = privateGetNewIndex(oldIndex - myTranslateIndex);
      if (newIndex == -1)
        return -1;
      else
        return newIndex + myTranslateIndex;
    }

    protected abstract int privateGetNewIndex(int oldIndex);
  }




  public static class EmptyListModel extends AROList {
    public Detach addListener(Listener listener) {
      return Detach.NOTHING;
    }

    public Detach addRemovedElementListener(RemovedElementsListener listener) {
      return Detach.NOTHING;
    }

    public int getSize() {
      return 0;
    }

    public Object getAt(int index) {
      throw new NoSuchElementException("index: " + index);
    }

    public void removeFirstListener(Condition condition) {
    }

    public void forceUpdateAt(int index) {
      assert false : index;
    }
  }
}
