package com.almworks.api.engine;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;

import java.util.*;

public class SyncParameters {
  private final Map<SyncParameter, ?> myMap = Collections15.hashMap();
  private boolean myReadOnly = false;

  public SyncParameters() {
  }

  public static SyncParameters downloadChangesAndMeta() {
    SyncParameters parameters = new SyncParameters();
    parameters.put(SyncParameter.UPDATE_CHANGES, Boolean.TRUE);
    parameters.put(SyncParameter.ALL_ITEMS, SyncType.RECEIVE_ONLY);
    parameters.put(SyncParameter.DOWNLOAD_META, Boolean.TRUE);
    return parameters;
  }

  public static SyncParameters downloadChangesAndMeta(Collection<Connection> conns) {
    if(conns == null || conns.isEmpty()) {
      return downloadChangesAndMeta();
    }

    final SyncParameters parameters = new SyncParameters();
    parameters.put(SyncParameter.UPDATE_CHANGES, Boolean.TRUE);
    parameters.put(SyncParameter.DOWNLOAD_META, Boolean.TRUE);
    parameters.put(SyncParameter.EXACT_CONNECTIONS, makeMap(conns, SyncType.RECEIVE_ONLY));
    return parameters;
  }

  private static Map<Connection, SyncType> makeMap(Collection<Connection> conns, SyncType type) {
    final Map<Connection, SyncType> map = Collections15.hashMap();
    for(final Connection conn : conns) {
      map.put(conn, type);
    }
    return map;
  }

  public static SyncParameters downloadChanges() {
    SyncParameters parameters = new SyncParameters();
    parameters.put(SyncParameter.UPDATE_CHANGES, Boolean.TRUE);
    parameters.put(SyncParameter.ALL_ITEMS, SyncType.RECEIVE_ONLY);
    return parameters;
  }

  public static SyncParameters downloadChanges(Collection<Connection> conns) {
    if(conns == null || conns.isEmpty()) {
      return downloadChanges();
    }
    
    final SyncParameters parameters = new SyncParameters();
    parameters.put(SyncParameter.UPDATE_CHANGES, Boolean.TRUE);
    parameters.put(SyncParameter.EXACT_CONNECTIONS, makeMap(conns, SyncType.RECEIVE_ONLY));
    return parameters;
  }

  public static SyncParameters receiveAndSendAll() {
    return singleton(SyncParameter.ALL_ITEMS, SyncType.RECEIVE_AND_SEND);
  }

  public static SyncParameters all(SyncType syncType) {
    return singleton(SyncParameter.ALL_ITEMS, syncType);
  }

  public static SyncParameters synchronizeItem(long item) {
    return singleton(SyncParameter.EXACT_ITEMS, Collections.singletonMap(item, SyncType.RECEIVE_AND_SEND));
  }

  public static SyncParameters synchronizeConnection(Connection connection, SyncType syncType) {
    return singleton(SyncParameter.EXACT_CONNECTIONS, Collections.singletonMap(connection, syncType));
  }

  public static SyncParameters syncItems(Collection<Long> items, SyncType syncType) {
    return createItemsParams(items, syncType);
  }

  public static SyncParameters syncItemsByID(Collection<Integer> ids, SyncType syncType) {
    return createItemsParamsByIds(ids, syncType);
  }

  private static SyncParameters createItemsParams(Collection<Long> items, SyncType type) {
    HashMap<Long, SyncType> params = Collections15.hashMap();
    for (Iterator<Long> iterator = items.iterator(); iterator.hasNext();) {
      Long item = iterator.next();
      params.put(item, type);
    }
    return singleton(SyncParameter.EXACT_ITEMS, params);
  }

  private static SyncParameters createItemsParamsByIds(Collection<Integer> items, SyncType type) {
    HashMap<Integer, SyncType> params = Collections15.hashMap();
    for (Iterator<Integer> iterator = items.iterator(); iterator.hasNext();) {
      Integer id = iterator.next();
      params.put(id, type);
    }
    return singleton(SyncParameter.EXACT_ITEMS_BY_ID, params);
  }

  @CanBlock
  public boolean isAffectedConnection(Database db, Connection connection) {
    Map<Connection, SyncType> exactConnections = get(SyncParameter.EXACT_CONNECTIONS);
    if (exactConnections != null && !exactConnections.containsKey(connection))
      return false;

    if (get(SyncParameter.ALL_ITEMS) != null)
      return true;

    boolean mayUpload = connection.isUploadAllowed();
    if (mayUpload && get(SyncParameter.CHANGED_ITEMS) != null)
      return true;

    Set<Connection> initializeConnections = get(SyncParameter.INITIALIZE_CONNECTION);
    if (initializeConnections != null && initializeConnections.contains(connection))
      return true;

    final Map<Long, SyncType> items = get(SyncParameter.EXACT_ITEMS);
    if (items != null) {
      final long scope = connection.getConnectionItem();
      Boolean result = db.readBackground(new ReadTransaction<Boolean>() {
        @Override
        public Boolean transaction(DBReader reader) throws DBOperationCancelledException {
          for (Long item : items.keySet()) {
            Long ca = SyncAttributes.CONNECTION.getValue(item, reader);
            if (ca != null && ca == scope)
              return true;
          }
          return false;
        }
      }).waitForCompletion();
      return result != null && result;
    }

    return exactConnections != null;
  }

  @ThreadAWT
  public boolean isAffectedConnectionShortCheck(Connection connection) {
    Map<Connection, SyncType> exactConnections = get(SyncParameter.EXACT_CONNECTIONS);
    if (exactConnections != null && !exactConnections.containsKey(connection))
      return false;
    return true;
  }

  public <T> T get(SyncParameter<T> parameter) {
    return parameter.getFrom(myMap);
  }

  public <T> SyncParameters put(SyncParameter<T> parameter, T value) {
    checkWriteAccess();
    checkParameter(parameter, value);
    parameter.putTo(myMap, value);
    return this;
  }

  private <T> void checkParameter(SyncParameter<T> parameter, T value) {
    assert parameter != null;
    assert value != null;
  }

  public Map toMap() {
    return Collections.unmodifiableMap((Map) myMap);
  }

  public void setReadOnly() {
    myReadOnly = true;
  }

  private void checkWriteAccess() {
    if (myReadOnly)
      throw new IllegalStateException("read-only synchronization parameters");
  }

  private static <T> SyncParameters singleton(SyncParameter<T> key, T value) {
    SyncParameters parameters = new SyncParameters();
    parameters.put(key, value);
    return parameters;
  }

  public boolean isReadOnly() {
    return myReadOnly;
  }

  public SyncParameters merge(SyncParameters parameters) {
    SyncParameters result = new SyncParameters();
    Map<SyncParameter, ?> map = Collections15.hashMap(parameters.toMap());
    for (Iterator<Map.Entry> ii = ((Map) myMap).entrySet().iterator(); ii.hasNext();) {
      Map.Entry<SyncParameter, ?> entry = ii.next();
      SyncParameter key = entry.getKey();
      Object thisValue = entry.getValue();
      Object thatValue = map.remove(key);
      if (thatValue == null) {
        result.put(key, thisValue);
        continue;
      }
      if (key == SyncParameter.ALL_ITEMS || key == SyncParameter.CHANGED_ITEMS) {
        result.put(key, SyncType.heaviest((SyncType) thisValue, (SyncType) thatValue));
      } else if (key == SyncParameter.EXACT_ITEMS) {
        result.put(key, union((Map<Long, SyncType>) thisValue, (Map<Long, SyncType>) thatValue));
      } else if (key == SyncParameter.EXACT_CONNECTIONS) {
        result.put(key, union((Map<Connection, SyncType>) thisValue, (Map<Connection, SyncType>) thatValue));
      } else {
        result.put(key, thisValue);
      }
    }
    for (Iterator<Map.Entry> ii = ((Map) map).entrySet().iterator(); ii.hasNext();) {
      Map.Entry<SyncParameter, ?> entry = ii.next();
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  private Set<Connection> union(Set<Connection> con1, Set<Connection> con2) {
    Set<Connection> result = Collections15.hashSet();
    if (con1 != null)
      result.addAll(con1);
    if (con2 != null)
      result.addAll(con2);
    return result;
  }

  private <K> Map<K, SyncType> union(Map<K, SyncType> map1, Map<K, SyncType> map2) {
    if (map2 == null)
      return map1;
    if (map1 == null)
      return map2;
    Map<K, SyncType> copy = new HashMap<K, SyncType>(map2);
    Map<K, SyncType> result = Collections15.hashMap();
    for (Iterator<Map.Entry<K, SyncType>> ii = map1.entrySet().iterator(); ii.hasNext();) {
      Map.Entry<K, SyncType> entry = ii.next();
      K key = entry.getKey();
      SyncType thisType = entry.getValue();
      SyncType thatType = copy.remove(key);
      result.put(key, SyncType.heaviest(thisType, thatType));
    }
    result.putAll(copy);
    return result;
  }

  public boolean hasSyncType(SyncType syncType) {
    for (Iterator<?> ii = myMap.values().iterator(); ii.hasNext();) {
      Object o = ii.next();
      if (o == syncType)
        return true;
      if (o instanceof Map) {
        for (Iterator<?> jj = ((Map) o).values().iterator(); jj.hasNext();) {
          Object u = jj.next();
          if (u == syncType)
            return true;
        }
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  public static SyncParameters initializeConnection(Connection connection) {
    return singleton(SyncParameter.INITIALIZE_CONNECTION, Collections.singleton(connection));
  }

  public boolean shouldLoadMeta() {
    Boolean b = get(SyncParameter.DOWNLOAD_META);
    if (b != null && b)
      return true;
    Set<Connection> set = get(SyncParameter.INITIALIZE_CONNECTION);
    if (set != null && !set.isEmpty())
      return true;
    return false;
  }
}
