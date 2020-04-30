package com.almworks.api.application.util.sources;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

public abstract class AbstractItemSource implements ItemSource {
  private static int ourCount = 0;
  private final TypedKey<Progress> PROGRESS_DELEGATE = key(Progress.class);
  protected final TypedKey<DetachComposite> DETACH = key(Detach.class);
  protected final String myName;

  protected AbstractItemSource(String name) {
    myName = name;
  }

  public ProgressSource getProgress(ItemsCollector collector) {
    return getProgressDelegate(collector);
  }

  protected Progress getProgressDelegate(ItemsCollector collector) {
    synchronized (this) {
      Progress value = collector.getValue(PROGRESS_DELEGATE);
      if (value != null)
        return value;
      value = Progress.delegator(myName);
      Progress oldValue = collector.putValue(PROGRESS_DELEGATE, value);
      if (oldValue != null) {
        collector.putValue(PROGRESS_DELEGATE, oldValue);
        return oldValue;
      } else {
        return value;
      }
    }
  }

  protected void setProgress(ItemsCollector collector, ProgressSource progress) {
    Progress delegate = getProgressDelegate(collector);
    delegate.delegate(progress);
  }

  protected static synchronized <T> TypedKey<T> key(Class clazz) {
    return TypedKey.create(clazz.getName() + "." + (++ourCount));
  }
}
