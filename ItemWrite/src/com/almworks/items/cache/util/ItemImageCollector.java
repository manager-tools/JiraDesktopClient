package com.almworks.items.cache.util;

import com.almworks.integers.LongList;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.ImageSliceEvent;
import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Computable;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.threads.Threads;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ItemImageCollector<T> implements ImageSlice.Listener {
  private final TLongObjectHashMap<T> myImages = new TLongObjectHashMap<>();
  private final ImageSlice mySlice;
  private final ImageFactory<T> myFactory;
  private final AtomicLong myVersion = new AtomicLong(0);
  private final boolean myAutoCreate;

  private ItemImageCollector(ImageSlice slice, ImageFactory<T> factory, boolean autoCreate) {
    mySlice = slice;
    myFactory = factory;
    myAutoCreate = autoCreate;
  }

  /**
   * Creates but doesn't start collector.
   * @param slice may be not started slice
   * @param factory image factory
   * @param autoCreate if true this collector creates image for an item immediately. This flag is useful when created image
   * is an active object and needs initialization
   * @return not started collector
   */
  public static <T> ItemImageCollector<T> create(ImageSlice slice, ImageFactory<T> factory, boolean autoCreate) {
    return new ItemImageCollector<T>(slice, factory, autoCreate);
  }

  public void start(Lifespan life) {
    mySlice.ensureStarted(life);
    mySlice.addListener(life, this);
  }

  public void addDataLoaders(DataLoader<?> ... loader) {
    mySlice.addData(loader);
  }

  public ImageSlice getSlice() {
    return mySlice;
  }

  @Override
  public void onChange(ImageSliceEvent event) {
    LongList added = event.getAdded();
    boolean hasChanges = !added.isEmpty();
    LongList changed = event.getChanged();
    for (int i = 0; i < changed.size(); i++) {
      long item = changed.get(i);
      T image = myImages.get(item);
      if (image != null && myFactory.update(image, item)) hasChanges = true;
    }
    LongList removed = event.getRemoved();
    for (int i = 0; i < removed.size(); i++) {
      T image = myImages.remove(removed.get(i));
      if (image != null) {
        myFactory.onRemoved(image);
        hasChanges = true;
      }
    }
    if (myAutoCreate)
      for (int i = 0; i < added.size(); i++) getImage(added.get(i));
    if (hasChanges) myVersion.incrementAndGet();
  }

  @ThreadSafe
  public long getVersion() {
    return myVersion.get();
  }

  public List<T> getAllImages() {
    Threads.assertAWTThread();
    LongList items = mySlice.getActualItems();
    List<T> result = Collections15.arrayList();
    for (int i = 0; i < items.size(); i++) {
      T image = getImage(items.get(i));
      if (image != null) result.add(image);
    }
    return result;
  }

  public T getImage(long item) {
    Threads.assertAWTThread();
    T image = myImages.get(item);
    if (image == null && mySlice.hasAllValues(item)) {
      image = myFactory.create(item);
      if (image != null) myImages.put(item, image);
    }
    return image;
  }

  public <V> T findImageByValue(DataLoader<V> loader, V value) {
    long item = getSlice().findItemByValue(loader, value);
    return item > 0 ? getImage(item) : null;
  }

  public interface ImageFactory<T> {
    /**
     * @param image item image to update
     * @return true if image value is really updated
     */
    @ThreadAWT
    boolean update(T image, long item);

    @ThreadAWT
    void onRemoved(T image);

    @ThreadAWT
    T create(long item);
  }


  public static class GetUpToDate<T> implements Computable<List<T>> {
    private final AtomicReference<Pair<Long, List<T>>> myLastKeys = new AtomicReference<Pair<Long, List<T>>>();
    private final ItemImageCollector<T> myKeys;

    private GetUpToDate(ItemImageCollector<T> keys) {
      myKeys = keys;
    }

    @Override
    public List<T> compute() {
      while (true) {
        Pair<Long, List<T>> pair = myLastKeys.get();
        long version = myKeys.getVersion();
        if (pair != null && pair.getFirst() == version) return pair.getSecond();
        List<T> images = Collections.unmodifiableList(myKeys.getAllImages());
        if (myLastKeys.compareAndSet(pair, Pair.create(version, images))) return images;
      }
    }

    public List<T> get() {
      Pair<Long, List<T>> pair = myLastKeys.get();
      if (pair != null && pair.getFirst() == myKeys.getVersion()) return pair.getSecond();
      return ThreadGate.AWT_IMMEDIATE.compute(this);
    }

    public static <T> GetUpToDate<T> create(ItemImageCollector<T> collector) {
      return new GetUpToDate<T>(collector);
    }
  }
}

