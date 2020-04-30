package com.almworks.items.sync;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface ItemVersionCreator extends ItemVersion, DBDrain {
  <T> ItemVersionCreator setValue(DBAttribute<T> attribute, @Nullable T value);

  ItemVersionCreator setValue(DBAttribute<Long> attribute, @Nullable DBIdentifiedObject value);

  void setValue(DBAttribute<Long> attribute, @Nullable ItemProxy value);

  void setValue(DBAttribute<Long> attribute, @Nullable ItemVersion value);

  void setList(DBAttribute<List<Long>> attribute, long[] value);

  void setList(DBAttribute<List<Long>> attribute, LongList value);

  void delete();

  void setSet(DBAttribute<? extends Collection<? extends Long>> attribute, LongList value);

  void setSet(DBAttribute<? extends Collection<? extends Long>> attribute, Collection<? extends ItemProxy> value);

  ItemVersionCreator setSequence(DBAttribute<byte[]> attribute, ScalarSequence sequence);

  /**
   * Marks the item as "alive" (not "invisible") in current branch.<br>
   * Marks the item as not removed.<br>
   * <br>
   * Note: When the item is created locally it has "invisible" state in server branch, so when item is downloaded from
   * server it has to be revived.<br>
   * It seems a good practice to revive all items downloaded from server, since integration is sure that the item actually
   * exists on server at the moment. This avoids downloading "removed" items after first-time submit and in case item was
   * temporary "invisible" due to user privileges restrictions on server.<br><br>
   * Note 2: Keeping "invisible" by mistake state after download may lead to item deletion during autoMerge.
   */
  void setAlive();

  void markMerged();

  <T extends Collection<? extends Long>> void addValue(DBAttribute<T> attribute, DBIdentifiedObject value);

  void addHistory(ItemProxy kind, byte[] step);
}
