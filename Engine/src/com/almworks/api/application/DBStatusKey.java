package com.almworks.api.application;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionState;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.api.DP;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SlaveUtils;
import com.almworks.items.wrapper.ItemStorageAdaptor;
import com.almworks.util.Pair;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.Map;

public class DBStatusKey extends SystemKey<ItemWrapper.DBStatus> implements AutoAddedModelKey<ItemWrapper.DBStatus>{
  public static final Role<DBStatusKey> ROLE = Role.role(DBStatusKey.class);
  public static final DBStatusKey KEY = new DBStatusKey();

  private DBStatusKey() {
    super("dbStatus");
  }

  public void extractValue(ItemVersion itemVersion, LoadedItemServices itemServices, PropertyMap values) {
    ItemWrapper.DBStatus status = calcStatus(itemVersion.getItem(), itemVersion.getReader(), itemServices.getConnection());
    values.put(getModelKey(), status);
  }

  @Override
  public void copyValue(ModelMap to, PropertyMap from) {
    super.copyValue(to, from);
    to.registerKey(getName(), this);
  }

  public static ItemWrapper.DBStatus calcStatus(long item, DBReader reader, Connection connection) {
    if (connection != null && connection.getState().getValue() != ConnectionState.READY)
      return ItemWrapper.DBStatus.DB_CONNECTION_NOT_READY;
    if (SyncUtils.isNew(item, reader)) return ItemWrapper.DBStatus.DB_NEW;
    if (checkState(connection, reader, item, true)) return ItemWrapper.DBStatus.DB_CONFLICT;
    if (checkState(connection, reader, item, false)) return ItemWrapper.DBStatus.DB_MODIFIED;
    return ItemWrapper.DBStatus.DB_NOT_CHANGED;
  }

  private static final TypedKey<Map<Pair<Connection, Boolean>, LongList>> MOD_CONF = TypedKey.create("modifiedItems");
  private static boolean checkState(Connection connection, DBReader reader, long item, boolean conflict) {
    Map<Pair<Connection, Boolean>, LongList> cache = MOD_CONF.getFrom(reader.getTransactionCache());
    LongList items = null;
    if (cache != null) items = cache.get(Pair.create(connection, conflict));
    if (items == null) {
      BoolExpr<DP> filter = conflict ? conf(connection, reader) : mod(connection, reader);
      if (reader instanceof DBWriter) return reader.query(filter).contains(item);
      items = reader.query(filter).copyItemsSorted();
      if (cache == null) {
        cache = Collections15.hashMap();
        MOD_CONF.putTo(reader.getTransactionCache(), cache);
      }
      cache.put(Pair.create(connection, conflict), items);
    }
    return items.binarySearch(item) >= 0;
  }

  private static BoolExpr<DP> mod(Connection connection, DBReader reader) {
    return connection != null
      ? connection.getProvider().getPrimaryStructure().getLocallyChangedFilter()
      : ItemStorageAdaptor.modified(SlaveUtils.getMasterAttributes(reader));
  }

  private static BoolExpr<DP> conf(Connection connection, DBReader reader) {
    return connection != null
      ? connection.getProvider().getPrimaryStructure().getConflictingItemsFilter()
      : ItemStorageAdaptor.inConflict(SlaveUtils.getMasterAttributes(reader));
  }

  @Override
  public void setValue(PropertyMap values, ItemWrapper.DBStatus value) {
    super.setValue(values, value);
  }
}
