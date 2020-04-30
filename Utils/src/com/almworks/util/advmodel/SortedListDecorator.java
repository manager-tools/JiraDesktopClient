package com.almworks.util.advmodel;

import com.almworks.util.collections.CollectionSortPolicy;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.IntArray;
import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
public class SortedListDecorator<T> extends AROList<T> implements AListModel.Listener, AListModelDecorator<T> {
  private final Lifecycle mySourceAttachLife = new Lifecycle();
  private final OrderListModel<ItemWrapper<T>> myImage = OrderListModel.create();
  private final CollectionSortPolicy mySortPolicy;

  private AListModel<? extends T> mySource;
  private Comparator<? super T> myComparator;
  private Detach mySourceDetach = Detach.NOTHING;

  private SortedListDecorator(Lifespan life, AListModel<? extends T> source, Comparator<? super T> comparator,
    CollectionSortPolicy sortPolicy)
  {
    mySortPolicy = sortPolicy;
    setSource(life, source);
    if (comparator != null)
      setComparator(comparator);
  }

  public static <T> SortedListDecorator<T> create(Lifespan life, AListModel<? extends T> source,
    Comparator<? super T> comparator)
  {
    return new SortedListDecorator<T>(life, source, comparator, CollectionSortPolicy.DEFAULT);
  }

  @Deprecated
  public static <T> SortedListDecorator<T> create(AListModel<? extends T> source, Comparator<? super T> comparator) {
    return new SortedListDecorator<T>(Lifespan.FOREVER, source, comparator, CollectionSortPolicy.DEFAULT);
  }

  public static <T> SortedListDecorator<T> createEmpty() {
    return new SortedListDecorator<T>(Lifespan.FOREVER, null, null, CollectionSortPolicy.DEFAULT);
  }

  public static <T extends Comparable<? super T>> AListModel<T> createForComparables(@NotNull Lifespan life,
    AListModel<? extends T> source)
  {
    return create(life, source, Containers.<T>comparablesComparator());
  }

  @Deprecated
  public static <T> SortedListDecorator<T> createWithoutComparator(AListModel<? extends T> source) {
    return new SortedListDecorator<T>(Lifespan.FOREVER, source, null, CollectionSortPolicy.DEFAULT);
  }

  public Detach addListener(Listener<? super T> listener) {
    return myImage.addListener(new MyListener(listener));
  }

  public void removeFirstListener(final Condition<Listener> condition) {
    myImage.removeFirstListener(new Condition<Listener>() {
      public boolean isAccepted(Listener value) {
        return value instanceof MyListener && condition.isAccepted(((MyListener) value).myListener);
      }
    });
  }

  public Detach addRemovedElementListener(final RemovedElementsListener<T> listener) {
    return myImage.addRemovedElementListener(new RemovedElementsListener<ItemWrapper<T>>() {
      public void onBeforeElementsRemoved(RemoveNotice<ItemWrapper<T>> itemWrappers) {
        listener.onBeforeElementsRemoved(itemWrappers.convertNotice(Unwrapper.UNWRAPPER));
      }
    });
  }

  public int getSize() {
    return myImage.getSize();
  }

  public T getAt(int index) {
    return myImage.getAt(index).getItem();
  }

  @Deprecated
  public Detach setSource(AListModel<? extends T> source) {
    DetachComposite life = new DetachComposite();
    setSource(life, source);
    return life;
  }

  public void setSource(Lifespan life, AListModel<? extends T> source) {
    if (mySource != null)
      processRemove(0, mySource.getSize());
    mySourceDetach.detach();
    mySource = source;
    if (mySource != null) {
      mySourceDetach = AListModelDetacher.attach(life, mySource, this);
      onInsert(0, mySource.getSize());
    } else {
      mySourceDetach = Detach.NOTHING;
    }
    assert verifyIntegrity();
  }
  

  public AListModel<? extends T> getSource() {
    return mySource;
  }

  public void onInsert(int index, int length) {
    for (int i = 0; i < myImage.getSize(); i++) {
      SortedListDecorator.ItemWrapper<T> wrapper = myImage.getAt(i);
      int sourceIndex = wrapper.getSourceIndex();
      if (sourceIndex >= index)
        wrapper.setSourceIndex(sourceIndex + length);
    }
    for (int i = index; i < index + length; i++)
      myImage.addElement(new ItemWrapper<T>(this, i), Containers.comparablesComparator());
    assert verifyIntegrity();
  }

  public void onRemove(int index, int length, RemovedEvent event) {
    processRemove(event.getFirstIndex(), event.getLength());
    assert verifyIntegrity();
  }

  public void onListRearranged(AListEvent event) {
    List<int[]> relocations = Collections15.arrayList();
    int minImageIndex = 0;
    int maxImageIndex = myImage.getSize() - 1;
    for (int i = event.getLowAffectedIndex(); i <= event.getHighAffectedIndex(); i++) {
      final int oldIndex = i;
      int newIndex = event.getNewIndex(oldIndex);
      if (newIndex == oldIndex)
        continue;
      int imageIndex = myImage.detectIndex(new Condition<ItemWrapper>() {
        public boolean isAccepted(ItemWrapper value) {
          return value.getSourceIndex() == oldIndex;
        }
      });
      assert imageIndex != -1;
      minImageIndex = Math.min(minImageIndex, imageIndex);
      maxImageIndex = Math.max(maxImageIndex, imageIndex);
      relocations.add(new int[] {imageIndex, newIndex});
    }
    if (!relocations.isEmpty()) {
      for (Iterator<int[]> iterator = relocations.iterator(); iterator.hasNext();) {
        int[] ints = iterator.next();
        int imageIndex = ints[0];
        myImage.getAt(imageIndex).setSourceIndex(ints[1]);
      }
    }
    myImage.sortOrUpdate(minImageIndex, maxImageIndex, Containers.comparablesComparator(), mySortPolicy);
  }

  public void onItemsUpdated(UpdateEvent event) {
    onListRearranged(event);
  }

  public void forceUpdateAt(int index) {
    myImage.forceUpdateAt(index);
  }

  public Comparator<? super T> getComparator() {
    return myComparator;
  }

  public void setComparator(Comparator<? super T> comparator) {
    myComparator = comparator;
    myImage.ensureSorted(Containers.comparablesComparator(), mySortPolicy);
  }

  boolean verifyIntegrity() {
    if (mySource == null) {
      assert myImage.getSize() == 0;
      return true;
    }
    assert myImage.getSize() == mySource.getSize();
    BitSet bitSet = new BitSet(myImage.getSize());
    for (int i = 0; i < myImage.getSize(); i++) {
      bitSet.set(myImage.getAt(i).getSourceIndex());
    }
    assert bitSet.cardinality() == myImage.getSize();
    return true;
  }

  private void processRemove(int index, int length) {
    IntArray indices = new IntArray(length);
    int lastIndex = index + length - 1;
    for (int i = 0; i < myImage.getSize(); i++) {
      SortedListDecorator.ItemWrapper<T> wrapper = myImage.getAt(i);
      int sourceIndex = wrapper.getSourceIndex();
      if (sourceIndex < index)
        continue;
      else if (sourceIndex > lastIndex)
        wrapper.setSourceIndex(sourceIndex - length);
      else
        indices.add(i);
    }
    myImage.removeAll(indices.toNativeArray());
  }

  private static class ItemWrapper<T> implements Comparable<ItemWrapper<T>> {
    private final T myItem;
    private int mySourceIndex;
    private final SortedListDecorator<T> myDecorator;

    public ItemWrapper(SortedListDecorator<T> decorator, int sourceIndex) {
      myDecorator = decorator;
      myItem = myDecorator.mySource.getAt(sourceIndex);
      mySourceIndex = sourceIndex;
    }

    public int compareTo(ItemWrapper<T> other) {
      if (myDecorator.myComparator != null) {
        int result = myDecorator.myComparator.compare(getItem(), other.getItem());
        if (result != 0)
          return result;
      }
      return Containers.compareInts(mySourceIndex, other.mySourceIndex);
    }

    public T getItem() {
      return myItem;
    }

    public int getSourceIndex() {
      return mySourceIndex;
    }

    public void setSourceIndex(int newIndex) {
      mySourceIndex = newIndex;
    }


    public String toString() {
      return mySourceIndex + ":" + myItem;
    }
  }


  private static class Unwrapper extends Convertor {
    public static final Unwrapper UNWRAPPER = new Unwrapper();
    public Object convert(Object itemWrapper) {
      return ((ItemWrapper) itemWrapper).getItem();
    }
  }


  private static class MyListener implements Listener {
    private final Listener myListener;

    public MyListener(Listener listener) {
      myListener = listener;
    }

    public void onInsert(int index, int length) {
      myListener.onInsert(index, length);
    }

    public void onRemove(int index, int length, RemovedEvent event) {
      RemoveNotice originalNotice = RemoveNotice.create(event.getFirstIndex(), event.getAllRemoved());
      RemoveNotice convertedNotice = originalNotice.convertNotice(Unwrapper.UNWRAPPER);
      myListener.onRemove(index, length, convertedNotice.createPostRemoveEvent());
    }

    public void onListRearranged(AListEvent event) {
      myListener.onListRearranged(event);
    }

    public void onItemsUpdated(UpdateEvent event) {
      myListener.onItemsUpdated(event);
    }
  }
}
