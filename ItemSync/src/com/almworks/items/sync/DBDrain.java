package com.almworks.items.sync;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DBDrain extends VersionSource {
  ItemVersionCreator createItem();

  @Override
  @NotNull
  ItemVersion forItem(DBIdentifiedObject object);

  /**
   * Provides facility to change item. When changing server version of an item all values of shadowable attributes are
   * copied from previous version<br>
   * Note when item is downloaded from server it should be revived since if the item is new it has "invisible" state in
   * server branch, and the invisible state is copied to new version.
   * @see {@link ItemVersionCreator#setAlive()}
   */
  ItemVersionCreator changeItem(long item);

  ItemVersionCreator changeItem(DBIdentifiedObject obj);

  ItemVersionCreator changeItem(ItemProxy proxy);

  long materialize(ItemProxy object);

  long materialize(DBIdentifiedObject object);

  List<ItemVersionCreator> changeItems(LongList items);

  /**
   * @see com.almworks.items.api.DBWriter#finallyDo(com.almworks.util.exec.ThreadGate, com.almworks.util.commons.Procedure)
   */
  void finallyDo(ThreadGate gate, Procedure<Boolean> procedure);
}
