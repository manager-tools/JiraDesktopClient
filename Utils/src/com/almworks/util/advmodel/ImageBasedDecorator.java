package com.almworks.util.advmodel;

import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;

import java.util.Comparator;
import java.util.List;

/**
 * @author : Dyoma
 */
public abstract class ImageBasedDecorator<T, D> extends AROList<D> implements AListModel.Listener<T> {
  private final OrderListModel<ItemWrapper<T, D>> myImage = new OrderListModel<ItemWrapper<T, D>>();
  private final AListModel<? extends T> mySource;
  private final Detach myDetach;

  protected ImageBasedDecorator(AListModel<? extends T> source) {
    mySource = source;
    // IS: this must not be called from constructor, as it leads to calling virtual methods
//    resynch();
    myDetach = mySource.addListener((AListModel.Listener)this);
  }

  protected void initModel() {
    if (mySource.getSize() > 0)
      onInsert(0, mySource.getSize());
  }

  protected abstract boolean isAccepted(int sourceIndex);

  protected abstract D createImage(T sourceItem);

  public Detach getDetach() {
    return myDetach;
  }

  public Detach addListener(final Listener<? super D> listener) {
    return myImage.addListener(new WrappingListener<T, D>(listener));
  }

  public void removeFirstListener(final Condition<Listener> condition) {
    myImage.removeFirstListener(new Condition<Listener>() {
      public boolean isAccepted(Listener listener) {
        return (listener instanceof WrappingListener) && condition.isAccepted(((WrappingListener) listener).myListener);
      }
    });
  }

  public Detach addRemovedElementListener(final RemovedElementsListener<D> listener) {
    return myImage.addRemovedElementListener(new RemovedElementsListener<ItemWrapper<T, D>>() {
      public void onBeforeElementsRemoved(RemoveNotice<ItemWrapper<T, D>> notice) {
        listener.onBeforeElementsRemoved(notice.convertNotice(Unwrapper.INSTANCE));
      }
    });
  }

  public int getSize() {
    return myImage.getSize();
  }

  public D getAt(int index) {
    return myImage.getAt(index).myConvertedItem;
  }

  public void onInsert(final int index, int length) {
    int imageIndex = detectImageIndexGreaterOrEqualThanSourceIndex(index);
    for (int i = imageIndex; i < myImage.getSize(); i++) {
      myImage.getAt(i).mySourceIndex += length;
    }
    addNewItems(index, length, imageIndex);
  }

  protected int addNewItems(final int sourceIndex, int length, int firstImageIndex) {
    return addAcceptedItems(sourceIndex, length, firstImageIndex);
  }

  public void forceUpdateAt(int index) {
    myImage.forceUpdateAt(index);
  }

  private int addAcceptedItems(final int sourceIndex, int length, int firstImageIndex) {
    if (length <= 0)
      return firstImageIndex;

    final int GROUP_THRESHOLD = 3;
    if (length < GROUP_THRESHOLD) {
      for (int i = sourceIndex; i < sourceIndex + length; i++) {
        if (isAccepted(i)) {
          myImage.insert(firstImageIndex, createWrapper(i));
          firstImageIndex++;
        }
      }
    } else {
      List<ItemWrapper<T, D>> toInsert = Collections15.arrayList();
      for (int i = sourceIndex; i < sourceIndex + length; i++) {
        if (isAccepted(i))
          toInsert.add(createWrapper(i));
      }
      int insertSize = toInsert.size();
      if (insertSize > 0) {
        myImage.insertAll(firstImageIndex, toInsert);
        firstImageIndex += insertSize;
      }
    }
    return firstImageIndex;
  }

  private ItemWrapper<T, D> createWrapper(int sourceIndex) {
    T sourceItem = mySource.getAt(sourceIndex);
    D imageItem = createImage(sourceItem);
    return new ItemWrapper<T, D>(sourceItem, imageItem, sourceIndex);
  }

  public void onRemove(int index, int length, RemovedEvent event) {
    int imageIndex = detectImageIndexGreaterOrEqualThanSourceIndex(event.getFirstIndex());
    int lastToRemove;
    for (lastToRemove = imageIndex; lastToRemove < myImage.getSize(); lastToRemove++) {
      if (myImage.getAt(lastToRemove).mySourceIndex > event.getLastIndex())
        break;
    }
    // todo set source index in wrappers to -1 ?
    if (lastToRemove != imageIndex) {
      doRemove(imageIndex, lastToRemove - 1);
    }
    for (int i = imageIndex; i < myImage.getSize(); i++) {
      myImage.getAt(i).mySourceIndex -= event.getLength();
    }
  }

  public void onItemsUpdated(UpdateEvent event) {
    resynch();
    // fire update on all image items, even if they are just added - who cares
    // also, update converted item
    int lowImage = detectImageIndexGreaterOrEqualThanSourceIndex(event.getLowAffectedIndex());
    int size = myImage.getSize();
    if (lowImage < size) {
      int highSource = event.getHighAffectedIndex();
      int highImage = detectImageIndexGreaterOrEqualThanSourceIndex(highSource);
      assert highImage >= lowImage;
      if (highImage >= size || myImage.getAt(highImage).mySourceIndex > highSource) {
        highImage--;
      }
      if (highImage >= lowImage) {
        for (int i = lowImage; i <= highImage; ++i) reconvertSourceItem(i);
        myImage.fireUpdate(updateRangeEvent(lowImage, highImage));
      }
    }
  }

  private void reconvertSourceItem(int imageIdx) {
    ItemWrapper<T, D> wrapper = myImage.getAt(imageIdx);
    assert wrapper != null;
    // Will fire later!
    myImage.replaceAt_NoFire(imageIdx, createWrapper(wrapper.mySourceIndex));
  }

  public void onListRearranged(AListEvent event) {
    int lowImageIndex = detectImageIndexGreaterOrEqualThanSourceIndex(event.getLowAffectedIndex());
    if (lowImageIndex == myImage.getSize())
      return;
    if (myImage.getAt(lowImageIndex).mySourceIndex > event.getHighAffectedIndex())
      return;
    int highAffectedIndex = event.getHighAffectedIndex();
    int highImageIndex = detectImageIndexGreaterOrEqualThanSourceIndex(highAffectedIndex);
    while (highImageIndex == myImage.getSize() || myImage.getAt(highImageIndex).mySourceIndex > highAffectedIndex) {
      highImageIndex--;
      assert highImageIndex >= lowImageIndex;
    }
    for (int i = lowImageIndex; i <= highImageIndex; i++) {
      ItemWrapper<T, D> wrapper = myImage.getAt(i);
      wrapper.mySourceIndex = event.getNewIndex(wrapper.mySourceIndex);
    }
    myImage.sort(ItemWrapper.COMPARATOR);
  }

  public AListModel<T> getSource() {
    return (AListModel<T>) mySource;
  }

  public int getOriginalIndex(int filteredIndex) {
    if (filteredIndex < 0 || filteredIndex >= getSize())
      return -1;
    return myImage.getAt(filteredIndex).mySourceIndex;
  }

  boolean checkConsistent() {
    int lastSourceIndex = -1;
    for (int i = 0; i < myImage.getSize(); i++) {
      ItemWrapper<T, D> wrapper = myImage.getAt(i);
      if (wrapper.mySourceIndex <= lastSourceIndex)
        return false;
      if (wrapper.mySourceItem != (T) mySource.getAt(wrapper.mySourceIndex))
        return false;
      lastSourceIndex = wrapper.mySourceIndex;
    }
    return true;
  }

  private int detectImageIndexGreaterOrEqualThanSourceIndex(final int index) {
    int imageIndex = myImage.detectIndex(new Condition<ItemWrapper<T, D>>() {
      public boolean isAccepted(ItemWrapper<T, D> value) {
        return value.mySourceIndex >= index;
      }
    });
    return imageIndex == -1 ? myImage.getSize() : imageIndex;
  }

  public void updateAll() {
    myImage.updateAll();
  }

  public void resynch() {
    resyncRemoveDisappeared();
    resyncAddAppeared();
  }

  private void resyncAddAppeared() {
    int expectedSourceIndex = 0;
    for (int imageIndex = 0; imageIndex < myImage.getSize(); imageIndex++) {
      ItemWrapper<T, D> wrapper = myImage.getAt(imageIndex);
      int sourceIndex = wrapper.mySourceIndex;
      if (sourceIndex != expectedSourceIndex)
        imageIndex = insertFromSource(expectedSourceIndex, sourceIndex, imageIndex);
      expectedSourceIndex = sourceIndex + 1;
    }
    if (expectedSourceIndex < mySource.getSize())
      insertFromSource(expectedSourceIndex, mySource.getSize(), myImage.getSize());
  }

  private void resyncRemoveDisappeared() {
    int removeStart = -1;
    for (int i = 0; i < myImage.getSize(); i++) {
      ItemWrapper<T, D> wrapper = myImage.getAt(i);
      if (!isAccepted(wrapper.mySourceIndex)) {
        if (removeStart < 0) {
          removeStart = i;
        }
      } else {
        if (removeStart >= 0) {
          int length = i - removeStart;
          assert length > 0 : i + " " + removeStart;
          doRemove(removeStart, i - 1);
          removeStart = -1;
          i -= length;
        }
      }
    }
    if (removeStart >= 0) {
      int length = myImage.getSize() - removeStart;
      assert length > 0 : myImage.getSize() + " " + removeStart;
      doRemove(removeStart, myImage.getSize() - 1);
    }
  }

  protected void doRemove(int firstIndex, int lastIndex) {
    myImage.removeRange(firstIndex, lastIndex);
  }

  private int insertFromSource(int from, int to, int imageIndex) {
    assert to > from;
    return addAcceptedItems(from, to - from, imageIndex);
  }

  public void imageUpdated(int index) {
    if (index >= 0 && index < myImage.getSize()) {
      myImage.updateAt(index);
    }
  }

  protected static class ItemWrapper<T, D> {
    private final T mySourceItem;
    private final D myConvertedItem;
    private int mySourceIndex;
    public static final Comparator<FilteringListDecorator.ItemWrapper<?, ?>> COMPARATOR =
      new Comparator<FilteringListDecorator.ItemWrapper<?, ?>>() {
        public int compare(FilteringListDecorator.ItemWrapper<?, ?> o1, FilteringListDecorator.ItemWrapper<?, ?> o2) {
          return Containers.compareInts(o1.mySourceIndex, o2.mySourceIndex);
        }
      };

    public ItemWrapper(T sourceItem, D convertedItem, int sourceIndex) {
      mySourceItem = sourceItem;
      myConvertedItem = convertedItem;
      mySourceIndex = sourceIndex;
    }

    public String toString() {
      return String.valueOf(myConvertedItem) + " @" + mySourceIndex;
    }
  }


  private static final class Unwrapper<T, D> extends Convertor<ItemWrapper<T, D>, D> {
    public static final Unwrapper INSTANCE = new Unwrapper();

    public D convert(ItemWrapper<T, D> itemWrapper) {
      return itemWrapper == null ? null : itemWrapper.myConvertedItem;
    }
  }

  private static class WrappingListener<T, D> implements Listener<ItemWrapper<T, D>> {
    private final Listener myListener;

    public WrappingListener(Listener listener) {
      myListener = listener;
    }

    public void onInsert(int index, int length) {
      myListener.onInsert(index, length);
    }

    public void onRemove(int index, int length, RemovedEvent<ItemWrapper<T, D>> event) {
      RemoveNotice<ItemWrapper<T, D>> notice = RemoveNotice.create(event.getFirstIndex(), event.getAllRemoved());
      RemoveNotice<T> converted = notice.convertNotice(Unwrapper.INSTANCE);
      myListener.onRemove(index, length, converted.createPostRemoveEvent());
    }

    public void onListRearranged(AListEvent event) {
      myListener.onListRearranged(event);
    }

    public void onItemsUpdated(UpdateEvent event) {
      myListener.onItemsUpdated(event);
    }
  }

  public static class ComplemetaryListModel<T> extends ImageBasedDecorator<T, T> {
    private final AListModel<T> mySubset;

    public ComplemetaryListModel(AListModel<? extends T> fullSet, AListModel<T> subsetModel) {
      super(fullSet);
      mySubset = subsetModel;
      initModel();
      mySubset.addListener(new Adapter<T>() {
        public void onInsert(int index, int length) {
          resynch();
        }

        public void onRemove(int index, int length, RemovedEvent<T> event) {
          resynch();
        }

        public void onListRearranged(AListEvent event) {
          resynch();
        }
      });
    }

    public boolean isAccepted(int sourceIndex) {
      return mySubset.indexOf(getSource().getAt(sourceIndex)) == -1;
    }

    protected T createImage(T sourceItem) {
      return sourceItem;
    }
  }
}
