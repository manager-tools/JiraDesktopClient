package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.entities.api.collector.typetable.EntityTable;
import com.almworks.items.impl.sqlite.SQLUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;

import java.util.*;

class BulkResolution {
  private static final int MAX_QUERY_VALUES = SQLUtil.MAX_SQL_PARAMS - 2;
  public static final ResolutionPolicy POLICY = new ResolutionPolicy() {
    @SuppressWarnings("unchecked")
    private final Collection<Class<?>> SUPPORTED_TYPES = Collections15.<Class<?>>unmodifiableSetCopy(String.class, Integer.class, Long.class);

    @Override
    public Result resolve(WriteState state, EntityTable table, List<EntityPlace> places, List<KeyInfo> resolutionColumns) {
      //noinspection ConstantConditions
      if (MAX_QUERY_VALUES < 2) return null;
      if (resolutionColumns.size() != 1) return null;
      KeyInfo column = resolutionColumns.get(0);
      AttributeCache cache = state.getAttributeCache();
      DBItemType dbItemType = cache.getType(table.getItemType());
      long type = dbItemType != null ? state.getReader().findMaterialized(dbItemType) : 0;
      KeyCounterpart counterpart = cache.getCounterpart(column);
      DBAttribute<?> attribute = counterpart.getAttribute();
      if (type <= 0 || attribute == null) return null;
      if (!checkAttribute(attribute)) return null;
      BulkResolution resolution = new BulkResolution(state, column, counterpart, attribute, type, places, new Result(table));
      resolution.perform();
      return resolution.getResult();
    }

    private boolean checkAttribute(DBAttribute<?> attribute) {
      return attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR && SUPPORTED_TYPES.contains(attribute.getScalarClass());
    }
  };
  
  private final WriteState myState;
  private final KeyInfo myColumn;
  private final KeyCounterpart myCounterpart;
  private final List<EntityPlace> myPlaces;
  private final DBAttribute<?> myAttribute;
  private final long myType;
  private final ResolutionPolicy.Result myResult;

  private BulkResolution(WriteState state, KeyInfo column, KeyCounterpart counterpart, DBAttribute<?> attribute, long type, List<EntityPlace> places,
    ResolutionPolicy.Result result) {
    myState = state;
    myColumn = column;
    myCounterpart = counterpart;
    myAttribute = attribute;
    myType = type;
    myPlaces = places;
    myResult = result;
  }

  public void perform() {
    LongArray items = queryItems();
    if (items == null) return;
    HashSet<Object> allDBValues = Collections15.hashSet();
    Map<Object, Long> valueResolution = collectResolutionMap(items, allDBValues);
    resolve(valueResolution, allDBValues);
  }

  public ResolutionPolicy.Result getResult() {
    return myResult;
  }

  private void resolve(Map<Object, Long> valueResolution, Collection<Object> allDBValues) {
    for (EntityPlace place : myPlaces) {
      Object entityValue = place.getValue(myColumn);
      if (entityValue == null || entityValue == ValueRow.NULL_VALUE) continue;
      Object dbValue = myCounterpart.convertToDB(myState, entityValue);
      if (dbValue == null) continue;
      if (!allDBValues.contains(dbValue)) myResult.addNotExisting(place);
      Long item = valueResolution.get(dbValue);
      if (item != null && item > 0) myResult.addResolution(place, item);
    }
  }

  private Map<Object, Long> collectResolutionMap(LongArray items, Set<Object> allExistingValues) {
    filterBy(items, SyncAttributes.CONNECTION, myState.getConnection());
    filterBy(items, DBAttribute.TYPE, myType);
    List<?> dbValues = myAttribute.collectValues(items, getReader());
    allExistingValues.addAll(dbValues);
    Map<Object, Long> valueResolution = Collections15.hashMap();
    for (int i = 0; i < dbValues.size(); i++) {
      Object dbValue = dbValues.get(i);
      Long existing = valueResolution.get(dbValue);
      if (existing != null) valueResolution.put(dbValue, 0l);
      else valueResolution.put(dbValue, items.get(i));
    }
    return valueResolution;
  }

  private void filterBy(LongArray items, DBAttribute<Long> attribute, long connection) {
    List<Long> connections = attribute.collectValues(items, getReader());
    for (int i = 0; i < items.size(); i++) {
      Long c = connections.get(i);
      if (c == null || c <= 0 || c != connection) {
        items.removeAt(i);
        connections.remove(i);
        i--;
      }
    }
  }

  private LongArray queryItems() {
    ArrayList<Object> values = Collections15.arrayList();
    for (EntityPlace place : myPlaces) {
      Object value = place.getValue(myColumn);
      if (value == null || value == ValueRow.NULL_VALUE) continue;
      values.add(value);
    }
    LongArray result = new LongArray();
    int start = 0;
    while (start < values.size()) {
      int end = Math.min(start + MAX_QUERY_VALUES, values.size());
      BoolExpr<DP> expr = myCounterpart.queryOneOf(myState, values.subList(start, end));
      if (expr == null) return null;
      LongArray found = DatabaseUnwrapper.query(getReader(), expr).copyItemsSorted();
      result.addAll(found);
      start = end;
    }
    result.sortUnique();
    return result;
  }

  private DBReader getReader() {
    return myState.getReader();
  }
}
