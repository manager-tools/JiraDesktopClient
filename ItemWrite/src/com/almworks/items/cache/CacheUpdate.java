package com.almworks.items.cache;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.util.DefaultMap;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class CacheUpdate implements Runnable {
  private final DBImage myImage;
  private final DBReader myReader;
  private final Lifespan myLife;
  private final Procedure<LongList> myReload;
  private final Map<TypedKey<?>, Object> myData = Collections15.hashMap();
  private final DefaultMap<DataLoader<?>, LongSet> myRequiredItems = DefaultMap.longSet();
  private final List<BaseImageSlice> mySlices;
  private final Map<DataLoader<?>, LoaderCounterpart> myLoaded = Collections15.hashMap();
  private final Map<CoherentUpdate<?>, Object> myCoherentUpdates = Collections15.hashMap();
  private final long myICN;

  public CacheUpdate(DBImage image, DBReader reader, Lifespan life, Procedure<LongList> reload) {
    myImage = image;
    myReader = reader;
    myLife = life;
    myReload = reload;
    mySlices = myImage.copySlices();
    myICN = reader.getTransactionIcn();
  }

  public void perform() {
    for (BaseImageSlice slice : mySlices) slice.updateItemSet(this);
    for (Map.Entry<DataLoader<?>, LongSet> entry : myRequiredItems.entrySet()) {
      DataLoader<?> loader = entry.getKey();
      LoaderCounterpart counterpart = LoaderCounterpart.load(this, loader, entry.getValue());
      myLoaded.put(loader, counterpart);
    }
    List<CoherentUpdate<?>> updates = Collections15.arrayList();
    myImage.copyCoherentUpdates(updates);
    for (CoherentUpdate<?> update : updates) {
      Object value = update.readDB(myReader);
      myCoherentUpdates.put(update, value);
    }
    ThreadGate.AWT.execute(this);
  }

  public Map<DataLoader<?>, LoaderCounterpart> getLoaded() {
    return myLoaded;
  }

  @SuppressWarnings( {"unchecked"})
  @Override
  public void run() {
    myImage.updateComplete(this);
    for (Map.Entry<CoherentUpdate<?>, Object> entry : myCoherentUpdates.entrySet())
      ((CoherentUpdate<Object>) entry.getKey()).awtUpdate(entry.getValue());
  }

  public DBReader getReader() {
    return myReader;
  }

  public Lifespan getLife() {
    return myLife;
  }

  public Procedure<LongList> getReload() {
    return myReload;
  }

  public Collection<BaseImageSlice> getSlices() {
    return mySlices;
  }

  public <T> void putData(TypedKey<T> key, T value) {
    key.putTo(myData, value);
  }

  public void setRequiredData(List<DataLoader<?>> loaders, LongList newSet) {
    for (DataLoader<?> loader : loaders) myRequiredItems.getOrCreate(loader).addAll(newSet);
  }

  public <T> T getData(TypedKey<T> key) {
    return key.getFrom(myData);
  }

  public long getICN() {
    return myICN;
  }
}
