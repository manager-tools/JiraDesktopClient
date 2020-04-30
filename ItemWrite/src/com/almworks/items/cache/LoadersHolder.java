package com.almworks.items.cache;

import com.almworks.integers.LongList;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

class LoadersHolder {
  private final TypedKey<List<DataLoader<?>>> LOADERS = TypedKey.create("loaders");
  private final BaseImageSlice mySlice;
  private final List<DataLoader<?>> myLoaders = Collections15.arrayList();
  private final Set<DataLoader<?>> myAdd = Collections15.hashSet();
  private final Set<DataLoader<?>> myRemove = Collections15.hashSet();
  private int myRequestVersion = 0;

  public LoadersHolder(BaseImageSlice slice) {
    mySlice = slice;
  }

  public void changeLoaders(Collection<? extends DataLoader<?>> add, Collection<? extends DataLoader<?>> remove) {
    changeLoaders(add, remove, null);
  }

  public boolean changeLoaders(Collection<? extends DataLoader<?>> add, Collection<? extends DataLoader<?>> remove,
    @Nullable Integer expectedVersion) {
    Set<DataLoader<?>> toAdd = Collections15.hashSet(add);
    Set<DataLoader<?>> toRemove = Collections15.hashSet(remove);
    toAdd.removeAll(toRemove);
    if (toAdd.isEmpty() && toRemove.isEmpty()) return true;
    boolean needsUpdate;
    synchronized (myLoaders) {
      if (expectedVersion != null && expectedVersion != myRequestVersion) return false;
      toAdd.removeAll(myAdd);
      toAdd.removeAll(myLoaders);
      myRemove.removeAll(toAdd);
      myAdd.addAll(toAdd);

      toRemove.removeAll(myRemove);
      myAdd.removeAll(toRemove);
      toRemove.removeAll(myLoaders);
      myRemove.addAll(toRemove);
      needsUpdate = !myAdd.isEmpty() || !myRemove.isEmpty();
      myRequestVersion++;
    }
    if (needsUpdate) mySlice.requestUpdate();
    return true;
  }

  public void setItemSet(CacheUpdate update, LongList newSet) {
    List<DataLoader<?>> newLoaders;
    synchronized (myLoaders) {
      newLoaders = Collections15.arrayList(myLoaders);
      newLoaders.addAll(myAdd);
      newLoaders.removeAll(myRemove);
    }
    update.putData(LOADERS, newLoaders);
    update.setRequiredData(newLoaders, newSet);
  }

  public int getLastRequested(Set<? super DataLoader<?>> target) {
    target.clear();
    synchronized (myLoaders) {
      target.addAll(myLoaders);
      target.addAll(myAdd);
      target.removeAll(myRemove);
      return myRequestVersion;
    }
  }

  public DataLoader<?>[] getActual() {
    synchronized (myLoaders) {
      return myLoaders.toArray(new DataLoader[myLoaders.size()]);
    }
  }

  public void applyLoaderUpdate(CacheUpdate update) {
    List<DataLoader<?>> loaders = update.getData(LOADERS);
    boolean needsUpdate;
    synchronized (myLoaders) {
      if (loaders != null) {
        myLoaders.clear();
        myLoaders.addAll(loaders);
        myAdd.removeAll(myLoaders);
        myRemove.retainAll(myLoaders);
      }
      needsUpdate = !myAdd.isEmpty() || !myRemove.isEmpty();
    }
    if (needsUpdate) mySlice.requestUpdate();
  }
}
