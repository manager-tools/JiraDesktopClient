package com.almworks.jira.provider3.schema;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPCompare;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerResolution;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Condition;
import org.almworks.util.Util;

import java.util.Collection;
import java.util.Iterator;

public class Resolution {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerResolution.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerResolution.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerResolution.NAME);
  public static final DBAttribute<Integer> ORDER = ServerJira.toScalarAttribute(ServerResolution.ORDER);
  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .renderFirstNotNull(NAME, ID)
    .addAttributeSubloaders(ID)
    .orderByNumber(ORDER, true)
    .create();
  public static final Condition<ItemKey> IS_UNRESOLVED = new Condition<ItemKey>() {
    @Override
    public boolean isAccepted(ItemKey value) {
      LoadedItemKey item = Util.castNullable(LoadedItemKey.class, value);
      if (item == null) return false;
      Integer id = item.getValue(ID);
      return id != null && id < 0;
    }
  };

  public static boolean removeNoValue(DBReader reader, Collection<? extends Long> values) {
    boolean found = false;
    for (Iterator<? extends Long> it = values.iterator(); it.hasNext();) {
      Long item = it.next();
      if (item == null || item <= 0) {
        it.remove();
        continue;
      }
      if (isUnresolved(reader, item)) {
        it.remove();
        found = true;
      }
    }
    return found;
  }

  public static boolean isUnresolved(DBReader reader, long item) {
    if (item <= 0) return false;
    Integer id = ID.getValue(item, reader);
    return (id == null || id < 0) && reader.findMaterialized(DB_TYPE) == DBAttribute.TYPE.getValue(item, reader);
  }

  public static void initDB(DBDrain drain, long connection) {
    if (connection <= 0) {
      LogHelper.error("No connection", connection);
      return;
    }
    BoolExpr<DP> filter = DPEqualsIdentified.create(DBAttribute.TYPE, DB_TYPE)
      .and(DPEquals.create(SyncAttributes.CONNECTION, connection))
      .and(DPCompare.less(ID, 0, false));
    LongArray resolutions = DatabaseUnwrapper.query(drain.getReader(), filter).copyItemsSorted();
    if (resolutions.size() == 1) drain.changeItem(resolutions.get(0)).setAlive();
    else if (resolutions.size() > 0) LogHelper.warning("Several UNRESOLVED", resolutions, "ignored");
    else {
      drain.createItem()
        .setValue(DBAttribute.TYPE, DB_TYPE)
      .setValue(SyncAttributes.CONNECTION, connection)
      .setValue(ID, -1)
      .setValue(NAME, ServerResolution.UNRESOLVED_NAME);
    }
  }
}
