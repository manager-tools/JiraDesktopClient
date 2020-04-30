package com.almworks.api.application;

import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.NotNull;

/**
 * @author dyoma
 */
public interface ItemSource {
  @ThreadAWT
  void stop(@NotNull ItemsCollector collector);

  @ThreadAWT
  void reload(@NotNull ItemsCollector collector);

  ProgressSource getProgress(ItemsCollector collector);

  ItemSource EMPTY = new EmptyItemSource();


  public static class EmptyItemSource implements ItemSource {
    public void stop(ItemsCollector collector) {
    }

    public ProgressSource getProgress(ItemsCollector collector) {
      return Progress.STUB;
    }

    public void reload(ItemsCollector collector) {
    }
  }
}
