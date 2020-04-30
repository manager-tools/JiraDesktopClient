package com.almworks.items.gui.edit.editors.enums;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.edit.CancelCommitException;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;

public interface EnumItemCreator {
  long createItem(CommitContext context, String id) throws CancelCommitException;

  class SimpleCreator implements EnumItemCreator {
    private final DBItemType myType;
    private final DBAttribute<String> myId;

    public SimpleCreator(DBItemType type, DBAttribute<String> id) {
      myType = type;
      myId = id;
    }

    @Override
    public long createItem(CommitContext context, String id) throws CancelCommitException {
      DBItemType dbType = myType;
      BoolExpr<DP> query = DPEqualsIdentified.create(DBAttribute.TYPE, dbType).and(DPEquals.create(myId, id));
      long connection = EngineConsts.getConnectionItem(context.getModel());
      if (connection > 0) query = query.and(DPEquals.create(SyncAttributes.CONNECTION, connection));
      else {
        LogHelper.error("Missing connection while creating", id);
        throw new CancelCommitException();
      }
      LongArray items = context.getReader().query(query).copyItemsSorted();
      if (items.size() >= 1) {
        LogHelper.assertError(items.size() == 1, "Multiple resolution", dbType, id, items);
        return items.get(0);
      }
      CommitContext enumContext = context.createNew();
      ItemVersionCreator item = enumContext.getCreator();
      item.setValue(DBAttribute.TYPE, dbType);
      item.setValue(SyncAttributes.CONNECTION, connection);
      item.setValue(myId, id);
      return item.getItem();
    }
  }
}
