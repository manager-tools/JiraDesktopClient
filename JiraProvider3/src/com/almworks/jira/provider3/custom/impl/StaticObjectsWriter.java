package com.almworks.jira.provider3.custom.impl;

import com.almworks.api.engine.DBCommons;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.ItemReference;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import org.jetbrains.annotations.Nullable;

class StaticObjectsWriter {
  private final DBDrain myDrain;
  private final LongArray myTypeItems;
  private final LongSet myNotWritten;

  private StaticObjectsWriter(DBDrain drain, LongList allItems, LongArray typeItems) {
    myDrain = drain;
    myTypeItems = typeItems;
    myNotWritten = LongSet.copy(allItems);
  }

  public static StaticObjectsWriter create(DBDrain drain, ItemReference owner, DBItemType ... types) {
    LongArray typeItems = new LongArray();
    for (DBItemType type : types) {
      long item = drain.materialize(type);
      if (item > 0) typeItems.add(item);
    }
    typeItems.sortUnique();
    LongArray allObjects = drain.getReader().query(DBCommons.OWNER.queryEqual(owner).and(DPEquals.equalOneOf(DBAttribute.TYPE, typeItems))).copyItemsSorted();
    return new StaticObjectsWriter(drain, allObjects, typeItems);
  }

  public void write(@Nullable DBStaticObject object) {
    if (object == null) return;
    long item = object.forceWrite(myDrain);
    myNotWritten.remove(item);
    assert checkType(item, object);
  }

  private boolean checkType(long item, DBStaticObject object) {
    if (item <= 0) return true;
    Long type = myDrain.forItem(item).getValue(DBAttribute.TYPE);
    if (type == null || type <= 0) LogHelper.error("Item has no type", object);
    else if (!myTypeItems.contains(type)) LogHelper.error("Writing not declared type", object, BadUtil.getItemType(myDrain.getReader(), type));
    return true;
  }

  public void deleteNotWritten() {
    for (ItemVersionCreator creator : myDrain.changeItems(myNotWritten)) creator.delete();
  }
}
