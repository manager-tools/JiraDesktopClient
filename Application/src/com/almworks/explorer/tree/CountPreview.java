package com.almworks.explorer.tree;

import com.almworks.api.application.tree.ItemsPreview;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBReader;
import com.almworks.util.threads.CanBlock;
import org.almworks.util.detach.Lifespan;

public class CountPreview implements ItemsPreview {
  private int myCount;

  public CountPreview(int count) {
    myCount = count;
  }

  @Override
  public boolean isValid() {
    return myCount >= 0;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public int getItemsCount() {
    return myCount;
  }

  @Override
  public void invalidate() {
    myCount = -1;
  }

  @CanBlock
  public static ItemsPreview scanView(DBFilter view, final Lifespan lifespan, DBReader reader) {
    if(!lifespan.isEnded()) {
      final long count = view.query(reader).count();
      if(!lifespan.isEnded()) {
        return new CountPreview((int) count);
      }
    }
    return null;
  }
}
