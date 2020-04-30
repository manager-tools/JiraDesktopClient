package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongCollections;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import org.almworks.util.TypedKey;

import java.util.Map;

public class ItemSourceUpdateEventBuilder {
  private static final TypedKey<ItemSourceUpdateEventBuilder> BUILDER = TypedKey.create("ISUEB");

  private final LongSetBuilder myRemovedBuilder = new LongSetBuilder();
  private final LongSetBuilder myInsertedBuilder = new LongSetBuilder();
  private final LongSetBuilder myUpdatedBuilder = new LongSetBuilder();

  public static ItemSourceUpdateEventBuilder getFrom(Map context) {
    ItemSourceUpdateEventBuilder builder = BUILDER.getFrom(context);
    if (builder == null) {
      builder = new ItemSourceUpdateEventBuilder();
      BUILDER.putTo(context, builder);
    }
    builder.cleanUp();
    return builder;
  }

  private ItemSourceUpdateEventBuilder() {
  }

  public ItemSourceUpdateEvent createEvent() {
    LongList added = null;
    LongList removed = null;
    LongList changed = null;
    if (!myInsertedBuilder.isEmpty()) {
      added = myInsertedBuilder.toArray();
      myInsertedBuilder.clear(false);
    }
    if (!myRemovedBuilder.isEmpty()) {
      removed = myRemovedBuilder.toArray();
      myRemovedBuilder.clear(false);
    }
    if (!myUpdatedBuilder.isEmpty()) {
      changed = myUpdatedBuilder.toArray();
      myUpdatedBuilder.clear(false);
    }
    assert added == null || LongCollections.isSorted(added.toNativeArray()) : added;
    assert removed == null || LongCollections.isSorted(removed.toNativeArray()) : removed;
    assert changed == null || LongCollections.isSorted(changed.toNativeArray()) : changed;
    return new ItemSourceUpdateEventImpl(added, removed, changed);
  }

  public void cleanUp() {
    myInsertedBuilder.clear(true);
    myRemovedBuilder.clear(true);
    myUpdatedBuilder.clear(true);
  }

  public void addRemoved(long itemId) {
    myRemovedBuilder.add(itemId);
  }

  public void addInserted(long itemId) {
    myInsertedBuilder.add(itemId);
  }

  public void addChanged(long itemId) {
    myUpdatedBuilder.add(itemId);
  }
}
