package com.almworks.util.advmodel;

import com.almworks.util.Pair;
import com.almworks.util.collections.IntArray;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public abstract class ImageBasedDecorator2<T, D> {
  private final OrderListModel<D> myImage = OrderListModel.create();
  private final AListModel<? extends T> mySource;
  private final IntArray myImageSourceIndices = new IntArray();

  protected ImageBasedDecorator2(AListModel<? extends T> source) {
    mySource = source;
  }

  /**
   * This method is called whenever an item appears or gets updates in the source model. The method should provide
   * elements that correspond to that item.
   * <p/>
   * NB: see return value description
   *
   * @param sourceItem  appearing or changing item in the source model
   * @param sourceIndex sourceItem's index in the source model
   * @param update      if false, the item has just appeared; if true, the item has been updated
   * @return If method returns null, it means "don't change anything": in case of the new item, no image will be created; in
   *         case of the item update, previous image will be left.
   *         <p/>
   *         If method returns a collection, it will be used as a new/replaced image corresponding to the sourceItem.
   *         <p/>
   *         To make sure that sourceItem has no image, return Collections15.emptyList().
   */
  @Nullable
  protected abstract List<? extends D> createImage(T sourceItem, int sourceIndex, boolean update);

  public AListModel<D> getDecoratedImage() {
    return myImage;
  }

  public void attach(Lifespan lifespan) {
    if (lifespan.isEnded())
      return;
    myImage.clear();
    myImageSourceIndices.clear();

    Pair<List<D>, IntArray> insertion = createInsertion(mySource.toList(), 0);
    if (insertion != null) {
      myImageSourceIndices.insertAll(0, insertion.getSecond());
      myImage.insertAll(0, insertion.getFirst());
    }

    lifespan.add(mySource.addListener(new MyListener()));
  }


  private void insert(int index, int length) {
    // increase source indexes for items moved by the insertion
    int i;
    for (i = myImageSourceIndices.size() - 1; i >= 0; i--) {
      int sourceIndex = myImageSourceIndices.get(i);
      if (sourceIndex >= index) {
        myImageSourceIndices.set(i, sourceIndex + length);
      } else {
        break;
      }
    }
    int insertionPoint = i + 1;
    assert insertionPoint >= 0 && insertionPoint <= myImageSourceIndices.size();
    Pair<List<D>, IntArray> insertion = createInsertion(mySource.subList(index, index + length), index);
    if (insertion != null) {
      IntArray indices = insertion.getSecond();
      List<D> values = insertion.getFirst();
      assert indices.size() == values.size();
      myImageSourceIndices.insertAll(insertionPoint, indices);
      myImage.insertAll(insertionPoint, values);
    }
  }

  private void remove(int index, int length) {
    int toIndex = index + length;
    int i;
    int size = myImageSourceIndices.size();
    int deleteTo = size;
    for (i = size - 1; i >= 0; i--) {
      int sourceIndex = myImageSourceIndices.get(i);
      if (sourceIndex >= toIndex) {
        myImageSourceIndices.set(i, sourceIndex - length);
        deleteTo = i;
      } else if (sourceIndex < index) {
        break;
      }
    }
    int deleteFrom = i + 1;
    assert deleteFrom >= 0 && deleteFrom <= size : deleteFrom + " " + size;
    assert deleteTo >= 0 && deleteTo <= size : deleteTo + " " + size;
    assert deleteTo >= deleteFrom : deleteFrom + " " + deleteTo + " " + size;
    if (deleteFrom < deleteTo) {
      myImageSourceIndices.removeRange(deleteFrom, deleteTo);
      myImage.removeRange(deleteFrom, deleteTo - 1);
    }
  }

  @Nullable
  private Pair<List<D>, IntArray> createInsertion(List<? extends T> sourceItems, int startingSourceIndex) {
    List<D> list = null;
    IntArray indices = null;
    for (int i = 0; i < sourceItems.size(); i++) {
      T sourceItem = sourceItems.get(i);
      Collection<? extends D> image = createImage(sourceItem, startingSourceIndex + i, false);
      if (image != null) {
        for (D imageItem : image) {
          if (list == null) {
            list = Collections15.arrayList();
            indices = new IntArray();
          }
          list.add(imageItem);
          indices.add(startingSourceIndex + i);
        }
      }
    }
    return list == null ? null : Pair.create(list, indices);
  }


  private abstract class Cycler {
    protected final AListModel.AListEvent myEvent;
    private final int myMinAffected;

    public Cycler(AListModel.AListEvent event, int minAffected) {
      myEvent = event;
      myMinAffected = minAffected;
    }

    public void process() {
      int lowIndex = myEvent.getLowAffectedIndex();
      int highIndex = myEvent.getHighAffectedIndex();
      if (lowIndex <= highIndex) {
        int imageFrom = myImageSourceIndices.binarySearch(lowIndex);
        int imageTo = myImageSourceIndices.binarySearch(highIndex + 1); // exclusive
        int length = imageTo - imageFrom;
        if (imageFrom < myImageSourceIndices.size() && length >= myMinAffected) {
          // we have something to rearrange too - more than one item
          processStart(imageFrom, imageTo);
          int lastSourceIndex = myImageSourceIndices.get(imageFrom);
          int lastSourceIndexStart = imageFrom;
          // cycle until imageTo: the "<=" is not incidental, do not replace with "<"
          for (int i = imageFrom + 1; i <= imageTo; i++) {
            int sourceIndex = i < imageTo ? myImageSourceIndices.get(i) : Integer.MAX_VALUE;
            if (sourceIndex != lastSourceIndex) {
              int diff = processImageGroup(lastSourceIndex, lastSourceIndexStart, i);
              i += diff;
              imageTo += diff;
              lastSourceIndex = sourceIndex;
              lastSourceIndexStart = i;
            }
          }
          assert lastSourceIndex == Integer.MAX_VALUE;
          assert lastSourceIndexStart == imageTo;
          processFinish(imageFrom, imageTo);
        }
      }
    }

    protected abstract void processStart(int imageFrom, int imageTo);

    protected abstract void processFinish(int imageFrom, int imageTo);

    /**
     * This method is allowed to insert or remove items from image up to, not including, imageToExclusive. All
     * items starting from imageToExclusive must remain unchanged.
     * <p/>
     * The method returns the change of size (number of added minus number of removed).
     */
    protected abstract int processImageGroup(int sourceIndex, int imageFrom, int imageToExclusive);
  }


  private class RearrangeCycler extends Cycler {
    public RearrangeCycler(AListModel.AListEvent event) {
      super(event, 2);
    }

    private SortedMap<Integer, List<D>> myImageReorder;

    protected void processStart(int imageFrom, int imageTo) {
      myImageReorder = Collections15.treeMap();
    }

    protected int processImageGroup(int sourceIndex, int imageFrom, int imageToExclusive) {
      int newIndex = myEvent.getNewIndex(sourceIndex);
      assert newIndex >= myEvent.getLowAffectedIndex() && newIndex <= myEvent.getHighAffectedIndex();
      List<D> expunged = myImageReorder.put(newIndex, myImage.subList(imageFrom, imageToExclusive));
      assert expunged == null : "reorder event gave same reorder index " + newIndex + " " + myEvent;
      return 0;
    }

    protected void processFinish(int imageFrom, int imageTo) {
      int length = imageTo - imageFrom;
      List<D> rearranged = Collections15.arrayList(length);
      IntArray rearrangedIndexes = new IntArray(length);
      for (Map.Entry<Integer, List<D>> entry : myImageReorder.entrySet()) {
        int sourceIndex = entry.getKey();
        List<D> values = entry.getValue();
        rearranged.addAll(values);
        int s = rearrangedIndexes.size();
        rearrangedIndexes.insertRange(s, s + values.size(), sourceIndex);
      }
      assert rearranged.size() == length;
      assert rearrangedIndexes.size() == length;
      myImageSourceIndices.replaceAll(imageFrom, rearrangedIndexes);
      myImage.rearrange(imageFrom, rearranged);
    }
  }


  private class UpdateCycler extends Cycler {
    private int myUpdateBunchStart = -1;
    private int myUpdateBunchFinish = -1;

    public UpdateCycler(AListModel.AListEvent event) {
      super(event, 1);
    }

    protected void processStart(int imageFrom, int imageTo) {
    }

    protected int processImageGroup(int sourceIndex, int imageFrom, int imageToExclusive) {
      List<? extends D> recreate = createImage(mySource.getAt(sourceIndex), sourceIndex, true);
      if (recreate == null) {
        if (myUpdateBunchStart == -1) {
          myUpdateBunchStart = imageFrom;
        } else {
          assert myUpdateBunchFinish == imageFrom;
        }
        myUpdateBunchFinish = imageToExclusive;
        return 0;
      } else {
        flushPendingUpdate();
        // remove old elements, add new
        assert imageFrom < imageToExclusive;
        myImageSourceIndices.removeRange(imageFrom, imageToExclusive);
        myImage.removeRange(imageFrom, imageToExclusive - 1);
        int removed = imageToExclusive - imageFrom;
        int added = recreate.size();
        if (added > 0) {
          myImageSourceIndices.insertRange(imageFrom, imageFrom + added, sourceIndex);
          myImage.insertAll(imageFrom, recreate);
        }
        return added - removed;
      }
    }

    private void flushPendingUpdate() {
      if (myUpdateBunchStart >= 0) {
        myImage.updateRange(myUpdateBunchStart, myUpdateBunchFinish - 1);
        myUpdateBunchStart = -1;
        myUpdateBunchFinish = -1;
      }
    }

    protected void processFinish(int imageFrom, int imageTo) {
      flushPendingUpdate();
    }
  }


  private class MyListener implements AListModel.Listener {
    public void onInsert(int index, int length) {
      insert(index, length);
    }

    public void onRemove(int index, int length, AListModel.RemovedEvent event) {
      remove(index, length);
    }

    public void onListRearranged(AListModel.AListEvent event) {
      new RearrangeCycler(event).process();
    }

    public void onItemsUpdated(AListModel.UpdateEvent event) {
      new UpdateCycler(event).process();
    }
  }
}
