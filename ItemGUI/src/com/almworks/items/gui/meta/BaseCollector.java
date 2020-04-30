package com.almworks.items.gui.meta;

import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.util.ItemSetModel;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.collections.Convertor;
import com.almworks.util.threads.Threads;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectIterator;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCollector<V, H extends BaseCollector.BaseHolder<V>> {
  private final Factory myFactory;
  protected final ItemSetModel<H> myModel;

  public BaseCollector(ImageSlice slice) {
    myFactory = new Factory();
    myModel = new ItemSetModel<H>(slice, myFactory);
  }

  public final void start(Lifespan life) {
    ImageSlice slice = myModel.getSlice();
    slice.ensureStarted(life);
    slice.addData(DataLoader.IDENTITY_LOADER);
    initSlice(slice);
    myModel.start(life);
  }

  protected abstract void initSlice(ImageSlice slice);

  protected abstract H createHolder(long item);

  @Nullable
  public V get(long item) {
    H h = myFactory.getOrCreateHolder(item);
    return h == null ? null : h.getOrCreate();
  }

  @Nullable
  public H getHolder(DBStaticObject obj) {
    return obj == null ? null : myModel.findImageByValue(DataLoader.IDENTITY_LOADER, obj.getIdentity());
  }

  @Nullable
  public V get(DBStaticObject obj) {
    H h = getHolder(obj);
    return h == null ? null : h.getOrCreate();
  }
  
  public List<V> copyCurrent() {
    ArrayList<V> result = Collections15.arrayList();
    for (TLongObjectIterator<H> it = myFactory.myHolders.iterator(); it.hasNext();) {
      it.advance();
      H holder = it.value();
      if (holder == null) continue;
      V value = holder.getOrCreate();
      if (value != null) result.add(value);
    }
    return result;
  }

  protected final Convertor<H, V> toValue = new Convertor<H, V>() {
    @Override
    public V convert(H holder) {
      return holder.getOrCreate();
    }
  };

  protected <T> T getValue(long item, DBAttribute<T> attribute) {
    return myModel.getSlice().getValue(item, attribute);
  }

  protected <T> T getValue(long item, DataLoader<T> loader) {
    return myModel.getSlice().getValue(item, loader);
  }

  private class Factory implements ItemSetModel.ItemWrapperFactory<H> {
    private final TLongObjectHashMap<H> myHolders = new TLongObjectHashMap<>();

    @Override
    public H getForItem(ImageSlice slice, long item) {
      return getOrCreateHolder(item);
    }

    H getOrCreateHolder(long item) {
      H holder = myHolders.get(item);
      if (holder == null) {
        holder = createHolder(item);
        myHolders.put(item, holder);
      }
      return holder;
    }

    @Override
    public void afterChange(LongList removed, LongList changed, LongList added) {
      for (LongListIterator i = removed.iterator(); i.hasNext(); ) myHolders.remove(i.nextValue());
      for (LongListIterator i = changed.iterator(); i.hasNext(); ) {
        long item = i.nextValue();
        H holder = myHolders.get(item);
        // If holder == null, we won't skip the update - next time we're asked to get it in getForItem() it will be created with the latest values
        if (holder != null) holder.update();
      }
    }

  }

  protected static abstract class BaseHolder<V> implements GuiFeature {
    protected final long myItem;
    private V myValue;

    public BaseHolder(long item) {
      myItem = item;
    }

    @Override
    public long getItem() {
      return myItem;
    }

    @Override
    public boolean isLoaded() {
      return getOrCreate() != null;
    }

    protected V getOrCreate() {
      Threads.assertAWTThread();
      if (myValue == null) myValue = createValue();
      return myValue;
    }

    public void update() {
      V value = createValue();
      if (value != null) myValue = value;
    }

    protected abstract V createValue();

    @Override
    public String toString() {
      return "Holder@" + myItem + "[" + myValue + "]";
    }
  }
}
