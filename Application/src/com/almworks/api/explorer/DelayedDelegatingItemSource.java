package com.almworks.api.explorer;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.util.sources.AbstractItemSource;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class DelayedDelegatingItemSource extends AbstractItemSource {
  private static final float INITIAL_PROGRESS = 0.05F;

  private final Progress myProgress;

  private ItemSource myDelegate;
  private WeakReference<ItemsCollector> myLastCollector;
  private boolean myRunning;

  public DelayedDelegatingItemSource(String name, Object initialActivity) {
    super(DelayedDelegatingItemSource.class.getName());
    myProgress = new Progress(name, initialActivity);
    myProgress.setProgress(INITIAL_PROGRESS);
  }

  public void addError(String error) {
    myProgress.addError(error);
  }

  @ThreadAWT
  public void stop(ItemsCollector collector) {
    myRunning = false;
    ItemSource delegate = myDelegate;
    if (delegate != null)
      delegate.stop(collector);
    myLastCollector = null;
    myProgress.setDone();
  }

  @ThreadAWT
  public void reload(@NotNull ItemsCollector collector) {
    myRunning = true;
    ItemSource delegate = myDelegate;
    if (delegate != null) {
      startDelegate(delegate, collector);
    }
    myLastCollector = new WeakReference<ItemsCollector>(collector);
  }

  public ProgressSource getProgress(ItemsCollector collector) {
    return myProgress;
  }

  @ThreadAWT
  public void setDelegate(ItemSource delegate) {
    assert myDelegate == null;
    myDelegate = delegate;
    if (myRunning && delegate != null) {
      ItemsCollector collector = myLastCollector.get();
      if (collector != null) {
        startDelegate(delegate, collector);
      } else {
        assert false : delegate + " " + this;
      }
    }
  }

  private void startDelegate(ItemSource delegate, ItemsCollector collector) {
    delegate.reload(collector);
    myProgress.setActivity(null);
    myProgress.delegate(delegate.getProgress(collector), 1 - INITIAL_PROGRESS);
  }
}
