package com.almworks.items.cache;

import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.util.*;

public class LoadersSet {
  private final Set<DataLoader<?>> myLoaders = Collections15.hashSet();
  private final Controller myController;

  public LoadersSet(Controller controller) {
    myController = controller;
  }

  public void changeLoaders(Collection<? extends DataLoader<?>> add, Collection<? extends DataLoader<?>> remove) {
    if (add == null) add = Collections15.emptyCollection();
    else add = Collections15.hashSet(add);
    if (remove == null) remove = Collections15.emptyCollection();
    add.removeAll(remove);
    boolean changed = false;
    synchronized (myLoaders) {
      for (DataLoader<?> loader : remove) if (myLoaders.remove(loader)) changed = true;
      for (DataLoader<?> loader : add) if (myLoaders.add(loader)) changed = true;
    }
    if (changed) myController.updateLoaders();
  }

  public void add(Collection<? extends DataLoader<?>> loaders) {
    changeLoaders(loaders, null);
  }

  public void remove(Collection<? extends DataLoader<?>> loaders) {
    changeLoaders(null, loaders);
  }

  public void setLoaders(Collection<? extends DataLoader<?>> loaders) {
    if (loaders == null) loaders = Collections15.emptyCollection();
    boolean changed = false;
    synchronized (myLoaders) {
      for (DataLoader<?> loader : loaders) if (myLoaders.add(loader)) changed = true;
      for (Iterator<DataLoader<?>> it = myLoaders.iterator(); it.hasNext();) {
        DataLoader<?> loader = it.next();
        if (!loaders.contains(loader)) {
          it.remove();
          changed = true;
        }
      }
    }
    if (changed) myController.updateLoaders();
  }

  public void clear() {
    setLoaders(null);
  }

  public ImageSlice getSlice() {
    return myController.mySlice;
  }

  static class Controller {
    private final List<LoadersSet> mySets = Collections15.arrayList();
    private final ImageSlice mySlice;
    private final LoadersHolder myHolders;
    private final LoadersSet myDefault;

    Controller(ImageSlice slice, LoadersHolder loaders) {
      mySlice = slice;
      myHolders = loaders;
      myDefault = createSet(Lifespan.FOREVER);
    }

    public final LoadersSet createSet(Lifespan live) {
      final LoadersSet set = new LoadersSet(this);
      synchronized (mySets) {
        mySets.add(set);
      }
      live.add(new Detach() {
        @Override
        protected void doDetach() throws Exception {
          synchronized (mySets) {
            mySets.remove(set);
          }
          updateLoaders();
        }
      });
      return set;
    }

    private void updateLoaders() {
      while (true) {
        List<DataLoader<?>> add = Collections15.arrayList();
        List<DataLoader<?>> remove = Collections15.arrayList();
        HashSet<DataLoader<?>> newLoaders = Collections15.hashSet();
        Set<DataLoader<?>> actualLoaders = Collections15.hashSet();
        int version = myHolders.getLastRequested(actualLoaders);
        synchronized (mySets) {
          for (LoadersSet set : mySets) set.getLoaders(newLoaders);
        }
        for (DataLoader<?> loader : newLoaders) if (!actualLoaders.contains(loader)) add.add(loader);
        for (DataLoader<?> loader : actualLoaders) if (!newLoaders.contains(loader)) remove.add(loader);
        if (add.isEmpty() && remove.isEmpty()) return;
        if (myHolders.changeLoaders(add, remove, version)) return;
      }
    }

    public LoadersSet getDefault() {
      return myDefault;
    }
  }

  private void getLoaders(Collection<? super DataLoader<?>> target) {
    synchronized (myLoaders) {
      target.addAll(myLoaders);
    }
  }
}
