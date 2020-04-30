package com.almworks.api.explorer;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.util.sources.AbstractItemSource;
import org.almworks.util.TypedKey;

public abstract class DelegatingItemSource extends AbstractItemSource {
  private final TypedKey<ItemSource> DELEGATE = key(DelegatingItemSource.class);

  protected DelegatingItemSource() {
    super(DelegatingItemSource.class.getName());
  }

  public void stop(ItemsCollector collector) {
    ItemSource delegate = collector.getValue(DELEGATE);
    if (delegate != null) {
      delegate.stop(collector);
      collector.putValue(DELEGATE, null);
    }
    getProgressDelegate(collector).setDone();
  }

  public void reload(ItemsCollector collector) {
    ItemSource delegate = createDelegate();
    if (delegate == null) {
      assert false;
      delegate = EMPTY;
    }
    collector.putValue(DELEGATE, delegate);
    delegate.reload(collector);
    getProgressDelegate(collector).delegate(delegate.getProgress(collector));
  }

  protected abstract ItemSource createDelegate();
}
