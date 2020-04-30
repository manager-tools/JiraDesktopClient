package com.almworks.syncreg;

import com.almworks.api.syncreg.SyncFlagRegistry;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.HashMap;
import java.util.Map;

class SyncFlagRegistryImpl implements SyncFlagRegistry {
  private static final DBAttribute<byte[]> FLAG_SYNC = SyncRegistryComponent.NS.bytes("flagSync");

  /**
   * Map ConnectionID -> ( ID -> FLAG )
   */
  private final Map<String, Map<String, Boolean>> myMap = Collections15.hashMap();
  private final SyncRegistryImpl myRegistry;

  SyncFlagRegistryImpl(SyncRegistryImpl registry) {
    myRegistry = registry;
  }

  @Override
  public boolean isSyncFlag(String connectionID, String id) {
    if (!myRegistry.isLoaded()) {
      assert false;
      return false;
    }
    if (connectionID == null || id == null || !myRegistry.isLoaded())
      return false;
    synchronized (myMap) {
      Map<String, Boolean> syncMap = getMapByConnectionID(connectionID);
      Boolean v = syncMap.get(id);
      return v != null && v;
    }
  }

  @Override
  public void setSyncFlag(String connectionID, String id, boolean flag) {
    if (!myRegistry.isLoaded()) {
      assert false;
      return;
    }
    if (connectionID == null || id == null || !myRegistry.isLoaded()) return;
    boolean changed;
    synchronized (myMap) {
      Map<String, Boolean> syncMap = getMapByConnectionID(connectionID);
      if (flag) {
        Boolean prev = syncMap.put(id, Boolean.TRUE);
        changed = prev == null || !prev;
      } else {
        Boolean prev = syncMap.remove(id);
        changed = prev != null && prev;
      }
    }
    if (changed) myRegistry.onSyncRegistryChanged(flag, !flag);
  }

  public void clearFlags(String connectionID) {
    if (!myRegistry.isLoaded()) {
      assert false;
      return;
    }
    if (connectionID == null)
      return;
    Map<String, Boolean> removed;
    synchronized (myMap) {
      removed = myMap.remove(connectionID);
    }
    if (removed != null) {
      myRegistry.onSyncRegistryChanged(false, true);
    } else {
      Log.debug("no sync map for connection " + connectionID);
    }
  }

  private Map<String, Boolean> getMapByConnectionID(String connectionID) {
    assert Thread.holdsLock(myMap);
    Map<String, Boolean> syncMap = myMap.get(connectionID);
    if (syncMap == null) {
      syncMap = Collections15.hashMap();
      myMap.put(connectionID, syncMap);
    }
    return syncMap;
  }

  private static final byte INT_TRUE = 2;
  private static final byte INT_FALSE = 1;
  public void load(DBReader reader) {
    long holder = reader.findMaterialized(SyncRegistryComponent.SYNC_HOLDER);
    if (holder <= 0) return;
    byte[] bytes = reader.getValue(holder, FLAG_SYNC);
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    int mapSize = stream.nextInt();
    if (mapSize < 0 || stream.isErrorOccurred()) {
      LogHelper.error("Negative map size", mapSize, stream);
      return;
    }
    Map<String, Map<String, Boolean>> result = Collections15.hashMap();
    while (mapSize > 0) {
      String connection = stream.nextUTF8();
      if (stream.isErrorOccurred()) {
        LogHelper.error("Failed to load flag sync", stream);
        return;
      }
      int connectionSize = stream.nextInt();
      if (connectionSize < 0 || stream.isErrorOccurred())  {
        LogHelper.error("Failed to load connection", connection, connectionSize, stream);
        return;
      }
      HashMap<String, Boolean> connectionMap = Collections15.hashMap();
      while (connectionSize > 0) {
        String id = stream.nextUTF8();
        byte value = stream.nextByte();
        if (stream.isErrorOccurred() || !(value == 0 || value == INT_TRUE || value == INT_FALSE)) {
          LogHelper.error("Failed to load id", connection, id, value, connectionSize, stream);
          return;
        }
        Boolean boolValue;
        if (value == 0) boolValue = null;
        else boolValue = value == INT_TRUE;
        connectionMap.put(id, boolValue);
        connectionSize--;
      }
      result.put(connection, connectionMap);
      mapSize--;
    }
    if (!stream.isSuccessfullyAtEnd()) {
      LogHelper.error("Failed to load flag sync", stream);
      return;
    }
    synchronized (myMap) {
      myMap.putAll(result);
    }
  }
  
  public void save(DBWriter writer) {
    Map<String, Map<String, Boolean>> map = deepCopy();
    long holder = writer.materialize(SyncRegistryComponent.SYNC_HOLDER);
    ByteArray bytes = new ByteArray();
    bytes.addInt(map.size());
    for (Map.Entry<String, Map<String, Boolean>> entry : map.entrySet()) {
      bytes.addUTF8(entry.getKey());
      Map<String, Boolean> connectionMap = entry.getValue();
      bytes.addInt(connectionMap.size());
      for (Map.Entry<String, Boolean> connectionEntry : connectionMap.entrySet()) {
        bytes.addUTF8(connectionEntry.getKey());
        byte value;
        Boolean boolValue = connectionEntry.getValue();
        if (boolValue == null) value = 0;
        else value = boolValue ? INT_TRUE : INT_FALSE;
        bytes.addByte(value);
      }
    }
    writer.setValue(holder, FLAG_SYNC, bytes.toNativeArray());
  }
  
  private Map<String, Map<String, Boolean>> deepCopy() {
    synchronized (myMap) {
      Map<String, Map<String, Boolean>> result = Collections15.hashMap();
      for (Map.Entry<String, Map<String, Boolean>> entry : myMap.entrySet()) {
        result.put(entry.getKey(), Collections15.hashMap(entry.getValue()));
      }
      return result;
    }
  }
}
