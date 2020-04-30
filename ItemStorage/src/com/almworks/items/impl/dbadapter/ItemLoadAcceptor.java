package com.almworks.items.impl.dbadapter;

import com.almworks.integers.LongList;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.Break;

public interface ItemLoadAcceptor {
  /**
   * Called when next iteration of loading from the database completes.
   * <p>
   * The implementation must not store instances of item id collections and buffer, passed to this method, as they are
   * reused. However, the implementation is free to do with the buffer anything - clear it, add other data, etc.
   *
   * @param requestedItems
   * @param loadedItems
   * @param buffer buffer that contains loaded data
   * @param bufferStartRow the starting row in the buffer where data is stored for loadedItemsSorted @throws Break to interrupt the process
   */
  void acceptLoaded(LongList requestedItems, LongList loadedItems, DBRowSet buffer,
    int bufferStartRow) throws Break, SQLiteException;
}
