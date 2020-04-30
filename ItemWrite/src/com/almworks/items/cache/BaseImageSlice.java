package com.almworks.items.cache;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.util.LogHelper;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

abstract class BaseImageSlice extends AbstractImageSlice {
  private final DBImage myImage;
  private final LoadersHolder myLoaders;
  private final FireEventSupport<Listener> myListeners = FireEventSupport.create(Listener.class);
  private static final int STATE_INITIAL = 0;
  private static final int STATE_RUNNING = 1;
  private static final int STATE_COMA = 2;
  private static final int STATE_BURIED = 3;
  private final AtomicInteger myAlive = new AtomicInteger();
  private volatile Lifespan myLife;
  private final LoadersSet.Controller myAttributes;

  BaseImageSlice(DBImage image) {
    myAlive.set(STATE_INITIAL);
    myImage = image;
    myLoaders = new LoadersHolder(this);
    myAttributes = new LoadersSet.Controller(this, myLoaders);
  }

  @Override
  public <T> T getValue(long item, DataLoader<T> data) {
    return myImage.getValue(item, data);
  }

  @Override
  public <T> T getNNValue(long item, DataLoader<T> loader, T defaultValue) {
    return Util.NN(getValue(item, loader), defaultValue);
  }

  @Override
  public <T> T getValue(long item, DBAttribute<T> data) {
    return myImage.getValue(item, data);
  }

  @Override
  public boolean hasValue(long item, DataLoader<?> data) {
    return myImage.hasValue(item, data);
  }

  @Override
  public void addData(Collection<? extends DataLoader<?>> loaders) {
    myAttributes.getDefault().add(loaders);
  }

  @Override
  public void removeData(Collection<? extends DataLoader<?>> loaders) {
    myAttributes.getDefault().remove(loaders);
  }

  @Override
  public void addListener(Lifespan life, Listener listener) {
    myListeners.addAWTListener(life, listener);
  }

  @Override
  public LoadersSet createAttributeSet(Lifespan life) {
    return myAttributes.createSet(life);
  }

  void requestUpdate() {
    Lifespan life = myLife;
    if (life == null || life.isEnded()) return;
    myImage.requestUpdate();
  }

  protected boolean isBuried() {
    return myAlive.get() == STATE_BURIED;
  }

  protected boolean isInitial() {
    return myAlive.get() == STATE_INITIAL;
  }

  protected boolean isRunning() {
    return myAlive.get() == STATE_RUNNING;
  }

  @Override
  public void ensureStarted(Lifespan life) {
    myImage.ensureRegistered(life, this);
  }

  boolean doStartSlice(Lifespan life) {
    if (myLife != null) {
      LogHelper.error("Already started");
      return false;
    }
    if (!myAlive.compareAndSet(STATE_INITIAL, STATE_RUNNING)) return false;
    myLife = life;
    myLife.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        bury();
      }
    });
    return true;
  }

  private void bury() {
    while (true) {
      int state = myAlive.get();
      switch (state) {
      case STATE_INITIAL: if (myAlive.compareAndSet(STATE_INITIAL, STATE_BURIED)) return;
      case STATE_RUNNING:
        if (!myAlive.compareAndSet(STATE_RUNNING, STATE_COMA)) continue;
        ThreadGate.AWT.execute(new Burial(this));
        return;
      case STATE_COMA:
      case STATE_BURIED: return;
      default: LogHelper.error("Illegal state", state); return;
      }
    }
  }

  protected abstract LongList earth();

  abstract void updateItemSet(CacheUpdate update);

  abstract void applyItemSetUpdate(CacheUpdate update, DataChange change);

  final void applyUpdate(CacheUpdate update, DataChange change) {
    applyItemSetUpdate(update, change);
    myLoaders.applyLoaderUpdate(update);
  }

  protected void setItemSet(CacheUpdate update, LongList newSet) {
    myLoaders.setItemSet(update, newSet);
  }

  void notifyListeners(DataChange change) {
    if (!isRunning()) return;
    myListeners.getDispatcher().onChange(new GenericEvent(change, this));
  }

  public DataLoader<?>[] getActualData() {
    return myLoaders.getActual();
  }

  public final DBImage getImage() {
    return myImage;
  }

  private static class Burial implements Runnable, ImageSliceEvent {
    private final BaseImageSlice mySlice;
    private LongList myItems;

    public Burial(BaseImageSlice slice) {
      mySlice = slice;
    }

    @Override
    public void run() {
      myItems = mySlice.earth();
      if (!mySlice.myAlive.compareAndSet(STATE_COMA, STATE_BURIED))
        LogHelper.error("unexpected state", mySlice.myAlive.get());
      mySlice.myListeners.getDispatcher().onChange(this);
      mySlice.myListeners.noMoreEvents();
      mySlice.getImage().unregister(mySlice);
    }

    @Override
    public long getIcn() {
      return mySlice.getImage().getICN();
    }

    @Override
    public LongList getAdded() {
      return LongList.EMPTY;
    }

    @Override
    public LongList getRemoved() {
      return myItems;
    }

    @Override
    public LongList getChanged() {
      return LongList.EMPTY;
    }

    @Override
    public ImageSlice getSlice() {
      return mySlice;
    }

    @Override
    public boolean isChanged(long item, DataLoader<?> loader) {
      return false;
    }
  }
}
