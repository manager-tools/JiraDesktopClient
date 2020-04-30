package com.almworks.api.application;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

public interface SearchResult {
  SearchResult EMPTY = new SearchResult() {
    public void addListener(Lifespan life, ThreadGate gate, final SearchListener listener) {
      if (!life.isEnded()) {
        gate.execute(new Runnable() {
          public void run() {
            listener.onSearchClosed(SearchResult.EMPTY);
          }
        });
      }
    }

    public boolean isDone() {
      return true;
    }

    public void showItem(ItemWrapper item) {

    }

    public Collection<? extends ItemWrapper> getItems() {
      return Collections15.emptyCollection();
    }

    @Nullable
    public JComponent getViewer() {
      return null;
    }
  };

  /**
   * Notifies the change of search lifecycle: SEARCHING -> DONE -> CLOSED <br>
   * It is guaranteed that:
   * 1. If search ever complete (before or after adding listener) than {@link com.almworks.api.application.SearchListener#onSearchCompleted(SearchResult)} be called
   * 2. {@link com.almworks.api.application.SearchListener#onSearchClosed(SearchResult)} is be called for every listener until listener life ends earlier.
   */
  void addListener(Lifespan life, ThreadGate gate, SearchListener listener);

  /**
   * @return true if search has reached stable state COMPLETED or CLOSED
   */
  boolean isDone();

  /**
   * @return currently loaded items wrappers
   */
  Collection<? extends ItemWrapper> getItems();

  /**
   * Shows item to user if it is contained in collection returned by {@link #getItems()}
   * @param item
   */
  void showItem(ItemWrapper item);

  @Nullable
  JComponent getViewer();
}
