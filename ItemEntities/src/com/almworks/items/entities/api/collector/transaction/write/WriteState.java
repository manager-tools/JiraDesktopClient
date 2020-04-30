package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class WriteState {
  private static final List<ResolutionPolicy> RESOLUTIONS = Collections15.unmodifiableListCopy(BulkResolution.POLICY, ResolutionPolicy.DEFAULT);
  private final Map<EntityTable, ResolutionState[]> myResolutions = Collections15.hashMap();
  private final EntityWriter myWriter;

  private WriteState(EntityWriter writer) {
    myWriter = writer;
  }

  public static WriteState create(EntityWriter writer, Map<EntityPlace, Long> extResolutions) {
    WriteState state = new WriteState(writer);
    state.resolve(extResolutions);
    return state;
  }

  private void resolve(Map<EntityPlace, Long> extResolutions) {
    DBNamespace ns = getNamespace();
    Collection<EntityTable> allTables = getAllTables();
    for (EntityTable table : allTables) {
      ResolutionState[] states = new ResolutionState[table.getPlaceCount()];
      myResolutions.put(table, states);
      Pair<ItemProxy[], EntityPlace[]> pair = table.getResolvedByProxy(ns);
      ItemProxy[] proxies = pair.getFirst();
      EntityPlace[] places = pair.getSecond();
      for (int i = 0; i < proxies.length; i++) {
        ItemProxy proxy = proxies[i];
        EntityPlace place = places[i];
        long item = proxy.findItem(getReader());
        states[place.getIndex()] = item > 0 ? ResolutionState.byItem(place, item) : ResolutionState.byProxy(place, proxy);
      }
    }
    addExternalResolutions(extResolutions);
    List<Pair<EntityTable, Long>> performanceLog = Collections15.arrayList();
    for (EntityTable table : DependencyOrder.buildResolutionOrder(ns, allTables)) {
      long start = System.currentTimeMillis();
      resolve(table);
      performanceLog.add(Pair.create(table, System.currentTimeMillis() - start));
    }
    logPerformance(performanceLog);
  }

  private void logPerformance(List<Pair<EntityTable, Long>> performanceLog) {
    long totalTime = 0;
    long totalCount = 0;
    for (Pair<EntityTable, Long> pair : performanceLog) {
      totalTime += pair.getSecond();
      totalCount += pair.getFirst().getPlaceCount();
    }
    int tablesCount = performanceLog.size();
    long average = totalTime / tablesCount;
    if (average < 2) return;
    for (Iterator<Pair<EntityTable, Long>> it = performanceLog.iterator(); it.hasNext(); ) {
      Pair<EntityTable, Long> pair = it.next();
      if (pair.getSecond() <= average) it.remove();
    }
    Collections.sort(performanceLog, Containers.convertingComparator(Pair.<Long>convertorGetSecond()));
    Collections.reverse(performanceLog);
    StringBuilder report = new StringBuilder("Resolution time log. ");
    report.append("Tables: ").append(tablesCount).append(" Time: ").append(totalTime).append("ms Average: ").append(average).append("ms\n");
    long loggedTime = 0;
    long loggedCount = 0;
    for (Pair<EntityTable, Long> pair : performanceLog) {
      EntityTable table = pair.getFirst();
      Long time = pair.getSecond();
      report.append(table.getItemType()).append("\t").append(time).append("ms count:").append(table.getPlaceCount()).append("\n");
      loggedTime += time;
      loggedCount += table.getPlaceCount();
    }
    report.append("All others: ").append(totalTime - loggedTime).append("ms count:").append(totalCount - loggedCount);
    LogHelper.debug(report.toString());
  }

  private void addExternalResolutions(Map<EntityPlace, Long> extResolutions) {
    for (Map.Entry<EntityPlace, Long> entry : extResolutions.entrySet()) {
      EntityPlace place = entry.getKey();
      ResolutionState[] states = myResolutions.get(place.getTable());
      if (states == null) {
        LogHelper.error("Unknown table", place);
        continue;
      }
      states[place.getIndex()] = ResolutionState.byItem(place, entry.getValue());
    }
  }

  private void resolve(EntityTable table) {
    DBItemType type = getAttributeCache().getType(table.getItemType());
    if (type == null) return;
    for (int resolutionIndex = 0; resolutionIndex < table.getResolutionsCount(); resolutionIndex++) {
      Pair<List<KeyInfo>, Collection<EntityPlace>> resolutions = table.getResolution(resolutionIndex);
      List<KeyInfo> resolutionColumns = resolutions.getFirst();
      List<EntityPlace> unresolved = selectUnresolved(table, resolutions.getSecond());
      for (ResolutionPolicy resolution : RESOLUTIONS) {
        if (unresolved.isEmpty()) break;
        ResolutionPolicy.Result resolved = resolution.resolve(this, table, unresolved, resolutionColumns);
        if (resolved == null || resolved.isEmpty()) continue;
        updateResolutions(resolutionIndex, unresolved, resolved);
        unresolved = selectUnresolved(table, unresolved);
      }
    }
  }

  private void updateResolutions(int resolutionIndex, List<EntityPlace> unresolved, ResolutionPolicy.Result resolved) {
    for (Iterator<EntityPlace> it = unresolved.iterator(); it.hasNext(); ) {
      EntityPlace place = it.next();
      EntityTable table = place.getTable();
      ResolutionState[] states = myResolutions.get(table);
      int index = place.getIndex();
      ResolutionState state = states[index];
      ResolutionState newState = updateState(resolutionIndex, resolved, place, state);
      if (newState != null) {
        states[index] = newState;
        it.remove();
      }
    }
  }

  private ResolutionState updateState(int resolutionIndex, ResolutionPolicy.Result resolved, EntityPlace place, @Nullable ResolutionState currentState) {
    EntityTable table = place.getTable();
    long item = resolved.getResolution(place);
    ResolutionState newState;
    if (item > 0) {
      boolean mayConflict = isIdentityConflictPossible(table, resolutionIndex);
      LogHelper.assertError(currentState == null || !currentState.isResolved(), "Already resolved", item, currentState);
      if (mayConflict && currentState != null && currentState.isCanCreate() && identityConflict(item, place)) newState = null;
      else newState = ResolutionState.byItem(place, item);
    } else if (resolved.isNotExisting(place)) {
      boolean canCreateThis = table.isCreateResolution(resolutionIndex) || myWriter.getFakeCreate().isAllowed(place);
      newState = canCreateThis ? ResolutionState.canCreate(resolutionIndex, place) : ResolutionState.notFound(place);
      newState = currentState != null ? currentState.better(newState) : newState;
    } else newState = null;
    return newState;
  }

  private boolean isIdentityConflictPossible(EntityTable table, int resolutionIndex) {
    if (!table.isCreateResolution(resolutionIndex)) return true;
    for (int i = 0; i < table.getResolutionsCount(); i++) {
      if (i == resolutionIndex) continue;
      if (table.isMutableResolution(i)) continue;
      return true;
    }
    return false;
  }

  private boolean identityConflict(long item, EntityPlace place) {
    EntityTable table = place.getTable();
    int count = table.getResolutionsCount();
    HashSet<KeyInfo> checkedColumns = Collections15.hashSet();
    for (int i = 0; i < count; i++) {
      List<KeyInfo> columns = table.getResolution(i).getFirst();
      for (KeyInfo column : columns) {
        if (!checkedColumns.add(column)) continue;
        Object value = place.getValue(column);
        if (value == null) continue;
        KeyCounterpart counterpart = getAttributeCache().getCounterpart(column);
        DBAttribute<?> attribute = counterpart.getAttribute();
        if (attribute == null) continue;
        Object dbValue = getReader().getValue(item, attribute);
        if (counterpart.equalValue(this, dbValue, value)) return true;
      }
    }
    return false;
  }

  private List<EntityPlace> selectUnresolved(EntityTable table, Collection<EntityPlace> places) {
    ResolutionState[] states = myResolutions.get(table);
    ArrayList<EntityPlace> result = Collections15.arrayList();
    for (EntityPlace place : places) {
      int index = place.getIndex();
      ResolutionState state = states[index];
      if (state == null || !state.isResolved()) result.add(place);
    }
    return result;
  }

  public Map<EntityTable, ResolutionState[]> getResolutions() {
    return myResolutions;
  }

  long getItem(EntityPlace place) {
    ResolutionState state = getResolutionState(place);
    return state != null ? state.getItem() : 0;
  }

  private ResolutionState getResolutionState(EntityPlace place) {
    if (place == null) return null;
    ResolutionState[] states = myResolutions.get(place.getTable());
    if (states == null) {
      LogHelper.error("Unknown table", place);
      return null;
    }
    int index = place.getIndex();
    if (index < 0 || index >= states.length) {
      LogHelper.error("Wrong index", place, states.length);
      return null;
    }
    return states[index];
  }

  public void write(DBDrain drain) {
    reviveAll(drain);
    materializeAll(drain);
    createAll(drain);
    updateAll(drain);
  }

  private void reviveAll(DBDrain drain) {
    LongArray items = new LongArray();
    for (ResolutionState[] states : myResolutions.values()) {
      for (ResolutionState state : states) {
        long item = state.getItem();
        if (item > 0) items.add(item);
      }
    }
    items.sortUnique();
    for (LongIterator cursor : items) drain.changeItem(cursor.value()).setAlive();
  }

  private void materializeAll(DBDrain drain) {
    for (ResolutionState[] states : myResolutions.values()) {
      for (int i = 0; i < states.length; i++) {
        ResolutionState state = states[i];
        if (state.isResolved()) continue;
        ItemProxy proxy = state.getProxy();
        if (proxy == null) continue;
        long item = proxy.findOrCreate(drain);
        EntityPlace place = state.getPlace();
        if (item <= 0) {
          LogHelper.error("Failed to materialize proxy", proxy, place);
          throw new DBOperationCancelledException();
        }
        states[i] = ResolutionState.byItem(place, item);
      }
    }
  }

  private void createAll(DBDrain drain) {
    for (EntityTable table : DependencyOrder.buildResolutionOrder(getNamespace(), getAllTables())) {
      ResolutionState[] states = myResolutions.get(table);
      Entity entityType = table.getItemType();
      if (StoreBridge.NULL_TYPE == entityType) {
        checkAllCreated(states);
        continue;
      }
      DBItemType type = getAttributeCache().getType(entityType);
      if (type == null) throw new DBOperationCancelledException();
      for (int i = 0; i < states.length; i++) {
        ResolutionState state = states[i];
        if (state == null) {
          LogHelper.error("Not prepared", table);
          throw new DBOperationCancelledException();
        }
        if (state.isResolved()) continue;
        int index = state.getCreateResolution();
        if (index < 0) {
          LogHelper.warning("Cannot create", state.getPlace());
          throw new DBOperationCancelledException();
        }
        ItemVersionCreator item = drain.createItem();
        item.setValue(DBAttribute.TYPE, type);
        item.setValue(SyncAttributes.CONNECTION, myWriter.getConnection()); // todo dont write connection for global objects
        states[i] = ResolutionState.byItem(state.getPlace(), item.getItem());
      }
    }
  }

  private void checkAllCreated(ResolutionState[] states) {
    for (ResolutionState state : states) {
      if (!state.isResolved()) {
        LogHelper.error("Not created yet", state.getPlace());
        throw new DBOperationCancelledException();
      }
    }
  }

  private void updateAll(DBDrain drain) {
    for (EntityTable table : getAllTables()) {
      ResolutionState[] states = myResolutions.get(table);
      Map<Integer, Set<DBAttribute<?>>> clearNotSet = myWriter.getClearNotSet(table.getItemType());
      WriteTable.write(this, drain, table, states, clearNotSet);
    }
  }

  private DBNamespace getNamespace() {
    return myWriter.getNamespace();
  }

  private Collection<EntityTable> getAllTables() {
    return myWriter.getAllTables();
  }

  AttributeCache getAttributeCache() {
    return myWriter.getAttributeCache();
  }

  public long getConnection() {
    return myWriter.getConnectionItem();
  }

  public DBReader getReader() {
    return myWriter.getReader();
  }
}
