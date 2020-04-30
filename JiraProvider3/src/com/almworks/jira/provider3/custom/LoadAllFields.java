package com.almworks.jira.provider3.custom;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.ConnectionState;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Function;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadAllFields {
  public static final TypedKey<String> NAME = TypedKey.create("name");
  public static final TypedKey<String> KEY = TypedKey.create("key");
  public static final TypedKey<Long> FIELD_ITEM = TypedKey.create("fieldItem");

  private final List<Map<TypedKey<?>, ?>> myFields = Collections15.arrayList();
  private final Map<TypedKey<?>, Function<ItemVersion, ?>> myAdditional = Collections15.hashMap();

  public LoadAllFields() {
  }

  public <T> void addAdditionalLoader(TypedKey<T> key, Function<ItemVersion, T> loader) {
    Function<ItemVersion, ?> prev = myAdditional.put(key, loader);
    LogHelper.assertError(prev == null, "Loader redefined", key, loader, prev);
  }

  public void load(DBReader reader) {
    LongArray fields = reader.query(DPEqualsIdentified.create(DBAttribute.TYPE, CustomField.DB_TYPE)).copyItemsSorted();
    for (LongIterator cursor : fields) {
      ItemVersion field = SyncUtils.readTrunk(reader, cursor.value());
      HashMap<TypedKey<?>,?> map = Collections15.hashMap();
      String key = field.getValue(CustomField.KEY);
      String name = field.getValue(CustomField.NAME);
      if (name == null) {
        LogHelper.warning("Skipping field", key, field, field.getValue(CustomField.ID));
        continue;
      }
      KEY.putTo(map, key);
      NAME.putTo(map, name);
      FIELD_ITEM.putTo(map, field.getItem());
      for (Map.Entry<TypedKey<?>, Function<ItemVersion, ?>> entry : myAdditional.entrySet()) {
        @SuppressWarnings("unchecked")
        TypedKey<Object> dataKey = (TypedKey<Object>) entry.getKey();
        Object data = entry.getValue().invoke(field);
        if (data != null) dataKey.putTo(map, data);
      }
      myFields.add(map);
    }
  }

  public List<Map<TypedKey<?>, ?>> getAllFields() {
    return myFields;
  }

  public List<Map<TypedKey<?>, ?>> getFieldsByKey(String key) {
    ArrayList<Map<TypedKey<?>, ?>> result = Collections15.arrayList();
    for (Map<TypedKey<?>, ?> field : myFields) if (Util.equals(key, KEY.getFrom(field))) result.add(field);
    return result;
  }

  public LongList getFieldItemsByKey(String key) {
    LongArray result = new LongArray();
    for (Map<TypedKey<?>, ?> field : myFields)
      if (Util.equals(key, KEY.getFrom(field))) {
        Long item = FIELD_ITEM.getFrom(field);
        if (item != null) result.add(item);
        else LogHelper.error("Missing field item", field);
      }
    return result;
  }

  public int countFields(String key) {
    if (key == null) return 0;
    int count = 0;
    for (Map<TypedKey<?>, ?> field : myFields) {
      String k = KEY.getFrom(field);
      if (key.equals(k)) count++;
    }
    return count;
  }

  public static class ConnectionName implements Function<ItemVersion, String> {
    public static final TypedKey<String> CONNECTION_NAME = TypedKey.create("connectionName");

    private final TLongObjectHashMap<String> myConnectionNames = new TLongObjectHashMap<>();

    private ConnectionName() {
    }

    public static ConnectionName create(ConnectionManager manager) {
      ConnectionName result = new ConnectionName();
      List<Connection> connections = manager.getConnections().copyCurrent();
      for (Connection connection : connections) {
        ConnectionState state = connection.getState().getValue();
        String connectionID = connection.getConnectionID();
        if (state != ConnectionState.READY) {
          LogHelper.warning("Skipping connection", state, connectionID);
          continue;
        }
        String name = manager.getConnectionName(connectionID);
        long item = connection.getConnectionItem();
        if (item < 0 || name == null) {
          LogHelper.error("Missing connection value", item, name, connectionID);
          continue;
        }
        result.myConnectionNames.put(item, name);
      }
      return result;
    }

    public static void addTo(LoadAllFields loadFields, ConnectionManager connectionManager) {
      loadFields.addAdditionalLoader(CONNECTION_NAME, create(connectionManager));
    }

    @Override
    public String invoke(ItemVersion field) {
      Long connection = field.getValue(SyncAttributes.CONNECTION);
      if (connection == null || connection <= 0 ) {
        LogHelper.error("Missing connection", field);
        return null;
      }
      return myConnectionNames.get(connection);
    }
  }
}
