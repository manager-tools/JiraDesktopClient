package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.explorer.loader.ItemModelRegistryImpl;
import com.almworks.explorer.loader.ItemsCollection;
import com.almworks.items.api.DBReader;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.util.advmodel.AListModelUpdater;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.progress.ProgressData;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.tests.DebugFlag;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifecycle;
import util.concurrent.Synchronized;
import util.concurrent.SynchronizedBoolean;

import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
class ItemsCollectorImpl extends SimpleModifiable implements ItemsCollector, ItemsCollectionController {
  private final ItemsCollection myCollection;
  private final Synchronized<ItemCollectorWidget> myWidget;
  private final ItemSource mySource;
  private final SynchronizedBoolean myLoadingDone = new SynchronizedBoolean(true);
  private final Lifecycle myLoadingCycle = new Lifecycle();
  private final DebugFlag mySourceIsRunning = new DebugFlag(false);
  private final PropertyMap myClientValues = new PropertyMap();

  private double myLastProgressValue;

  private final Runnable myProgressRunnable = new Runnable() {
    public void run() {
      progress();
    }
  };
  private final Bottleneck myProgressBottleneck = new Bottleneck(250, ThreadGate.AWT, myProgressRunnable);

  public ItemsCollectorImpl(ItemModelRegistryImpl registry, TimeTracker timeTracker, ItemCollectorWidget widget, ItemSource source) {
    this(new ItemsCollection(registry, timeTracker), widget, source);
  }

  private ItemsCollectorImpl(ItemsCollection collection, ItemCollectorWidget widget, ItemSource source) {
    myCollection = collection;
    myWidget = new Synchronized<ItemCollectorWidget>(widget);
    mySource = source;
  }

  public void reload() {
    myLoadingDone.set(false);
    myCollection.setLiveMode(true);
    myLoadingCycle.cycle();
    assert mySourceIsRunning.toggleToTrue();
    mySource.reload(this);
    myLastProgressValue = 0F;
    mySource.getProgress(this).getModifiable().addChangeListener(myLoadingCycle.lifespan(), new ChangeListener() {
      @CanBlock
      public void onChange() {
        boolean done = mySource.getProgress(ItemsCollectorImpl.this).isDone();
        if (done) {
          myProgressBottleneck.abort();
          ThreadGate.AWT.execute(myProgressRunnable);
        } else {
          myProgressBottleneck.request();
        }
      }
    });
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        progress();
        fireChanged();
      }
    });
  }

  @ThreadAWT
  private void progress() {
    Threads.assertAWTThread();
    if (!isLoading())
      return;
    ProgressData progress = mySource.getProgress(this).getProgressData();
    ItemCollectorWidget widget = getWidget();
    if (progress.isDone()) {
      myCollection.forceUpdate();
      myLoadingDone.set(true);
      widget.loadingDone();
      fireChanged();
    } else {
      double progressValue = progress.getProgress();
      if (progressValue > myLastProgressValue + 1e-4) {
        myLastProgressValue = progressValue;
        widget.showProgress((float)progressValue);
      }
      Object activity = progress.getActivity();
      widget.showLoadingMessage(activity == null ? null : activity.toString());
    }
    List<String> errors = progress.getErrors();
    widget.showErrors(errors);
  }

  @Override
  public void addItem(long item, DBReader reader) {
    myCollection.addItem(item, reader);
    fireChanged();
  }

  @Override
  public void removeItem(long item) {
    assert !Context.isAWT();
    myCollection.removeItems(item);
  }

  public void dispose() {
    myWidget.set(ItemCollectorWidget.DEAF);
    assert mySourceIsRunning.toggleToFalse();
    myCollection.dispose();
    doCancelLoading();
    fireChanged();
  }

  public boolean isLoading() {
    return !myLoadingDone.get();
  }

  public void cancelLoading(final String reason) {
    doCancelLoading();
    ThreadGate.AWT.execute(new Runnable() {
      // must be invoked later because otherwise the call from progress() may come later that these
      public void run() {
        ItemCollectorWidget widget = getWidget();
        widget.showErrors(Collections.singletonList(reason));
        widget.loadingDone();
        fireChanged();
      }
    });
  }

  private void doCancelLoading() {
    mySource.stop(this);
    myLoadingCycle.cycle();
    myLoadingDone.set(true);
  }

  public AListModelUpdater<? extends LoadedItem> getListModelUpdater() {
    return myCollection.getListModelUpdater();
  }

  private ItemCollectorWidget getWidget() {
    return myWidget.get();
  }

  public ScalarModel<LifeMode> getLifeModeModel() {
    return myCollection.getLifeModeModel();
  }

  public ItemsCollectionController createCopy(ItemCollectorWidget widget) {
    assert myLoadingDone.get() : "Not implemented yet";
    ItemsCollectorImpl copy = new ItemsCollectorImpl(myCollection.createCopy(), widget, mySource);
    copy.mySourceIsRunning.toggleToTrue();
    return copy;
  }

  public void setLiveMode(boolean live) {
    if (myLoadingDone.get())
      myCollection.setLiveMode(live);
  }

  public void updateAllItems() {
    myCollection.updateAllItems();
    fireChanged();
  }

  public boolean isElementSetChanged() {
    return myCollection.isElementSetChanged();
  }

  public <T> T getValue(TypedKey<T> key) {
    synchronized (myClientValues) {
      return myClientValues.get(key);
    }
  }

  public <T> T putValue(TypedKey<T> key, T value) {
    synchronized (myClientValues) {
      T prev = myClientValues.get(key);
      myClientValues.put(key, value);
      return prev;
    }
  }

  public void reportError(String error) {
    cancelLoading(error);
  }
}
