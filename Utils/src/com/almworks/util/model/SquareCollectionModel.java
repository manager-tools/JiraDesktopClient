package com.almworks.util.model;


/**
 * :todoc:
 *
 * @author sereda
 */
public interface SquareCollectionModel <R, C, V>
  extends ContentModel<SquareCollectionModelEvent<R, C, V>, SquareCollectionModel.Consumer<R, C, V>> {

  interface Consumer <R, C, V> extends ContentModelConsumer<SquareCollectionModelEvent<R, C, V>> {
    void onRowsAdded(SquareCollectionModelEvent<R, C, V> event);

    void onColumnsAdded(SquareCollectionModelEvent<R, C, V> event);

    void onCellsSet(SquareCollectionModelEvent<R, C, V> event);
  }

  abstract class Adapter <R, C, V> implements Consumer<R, C, V> {
    public void onRowsAdded(SquareCollectionModelEvent<R, C, V> event) {
    }

    public void onColumnsAdded(SquareCollectionModelEvent<R, C, V> event) {
    }

    public void onCellsSet(SquareCollectionModelEvent<R, C, V> event) {
    }

    public void onContentKnown(SquareCollectionModelEvent<R, C, V> event) {
    }
  }
}
