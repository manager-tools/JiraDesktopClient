package com.almworks.util.model;

import java.util.Collection;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface CollectionModel <E> extends ContentModel<CollectionModelEvent<E>, CollectionModel.Consumer<E>> {
  List<E> copyCurrent();

  Collection<E> getFullCollectionBlocking() throws InterruptedException;

  int getCurrentCount();

  // optional
  ScalarModel<Integer> getCountModel();

  // optional
  boolean containsCurrently(E element);

  public static interface Consumer <E> extends ContentModelConsumer<CollectionModelEvent<E>> {
    void onScalarsAdded(CollectionModelEvent<E> event);

    void onScalarsRemoved(CollectionModelEvent<E> event);
  }

  public static abstract class Adapter <E> implements Consumer<E> {
    public void onScalarsAdded(CollectionModelEvent<E> event) {
      onChange();
    }

    public void onScalarsRemoved(CollectionModelEvent<E> event) {
      onChange();
    }

    public void onContentKnown(CollectionModelEvent<E> event) {
    }

    protected void onChange() {
    }
  }
}

