package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.util.LogHelper;
import gnu.trove.TLongLongHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

interface ResolutionPolicy {
  /**
   * @return array of resolved or unresolved items, one for each place. May return null - means can not resolve any place.
   */
  @Nullable
  Result resolve(WriteState state, EntityTable table, List<EntityPlace> places, List<KeyInfo> resolutionColumns);

  ResolutionPolicy DEFAULT = new ResolutionPolicy() {
    @Override
    public Result resolve(WriteState state, EntityTable table, List<EntityPlace> places, List<KeyInfo> resolutionColumns) {
      DBItemType type = state.getAttributeCache().getType(table.getItemType());
      if (type == null) return null;
      DBEntityQuery query = DBEntityQuery.create(state, type, resolutionColumns);
      if (query == null) return null;
      Result result = new Result(table);
      for (EntityPlace place : places) {
        long item = query.search(place);
        if (item > 0) result.addResolution(place, item);
        else result.addNotExisting(place);
      }
      return result;
    }
  };
  
  class Result {
    private final EntityTable myExpectedTable;
    private final TLongLongHashMap myResolved = new TLongLongHashMap();
    private final TLongLongHashMap myNotExistIndexes = new TLongLongHashMap();

    public Result(EntityTable expectedTable) {
      myExpectedTable = expectedTable;
    }

    public void addResolution(EntityPlace place, long item) {
      if (item <= 0) return;
      if (!checkTable(place)) return;
      int index = place.getIndex();
      if (myNotExistIndexes.containsKey(index)) {
        LogHelper.error("Already not exists", place, item);
        return;
      }
      myResolved.put(index, item);
    }

    public void addNotExisting(EntityPlace place) {
      if (!checkTable(place)) return;
      int index = place.getIndex();
      if (myResolved.containsKey(index)) {
        LogHelper.error("Already resolved", place);
        return;
      }
      myNotExistIndexes.put(index, 1);
    }

    private boolean checkTable(EntityPlace place) {
      if (place == null) return false;
      if (myExpectedTable != place.getTable()) {
        LogHelper.error("Wrong table", myExpectedTable, place);
        return false;
      }
      return true;
    }

    public boolean isEmpty() {
      return myNotExistIndexes.isEmpty() && myResolved.isEmpty();
    }

    public long getResolution(EntityPlace place) {
      if (!checkTable(place)) return 0;
      int index = place.getIndex();
      return myResolved.containsKey(index) ? myResolved.get(index) : 0;
    }

    public boolean isNotExisting(EntityPlace place) {
      if (!checkTable(place)) return false;
      return myNotExistIndexes.containsKey(place.getIndex());
    }
  }
}
