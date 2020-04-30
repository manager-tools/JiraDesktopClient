package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.PrioritizedListenerSupport;
import com.almworks.sqlite4java.SQLiteException;

/**
 * ItemSet?
 * <p/>
 * ItemSource provides a set of items and updates to that set.
 * <p>
 * When a client is attached to this source, it receives reload() call first, then it may receive reload()
 * and update() calls.
 */
public interface ItemSource extends PrioritizedListenerSupport<ItemSource.Listener> {
  interface Listener {
    /**
     * This method allows the client to fully load ids from a table corresponding to the item set.
     * todo: item source may provide not only table name, but "where" conditions as well, if it is a narrowing of a
     * parent view
     * <p>
     * Invariant: before any update() method is called, reload() method is called.
     * <p>
     * This method may be called more than once, in which case the client has to recreate its view (id array, whatever)
     */
    void reload(TransactionContext context, String idTableName) throws SQLiteException;

    /**
     * This method is called to notify the client of the latest database update that affects the source.
     * <p>
     * The source keeps track of azll clients and provides correct events.
     */
    void update(TransactionContext context, ItemSourceUpdateEvent event, String idTableName, boolean forced) throws SQLiteException;
  }
}
