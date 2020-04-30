package com.almworks.items.impl.dbadapter;

import com.almworks.util.collections.arrays.ObjectArrayAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.Const;

import java.util.List;

public abstract class BaseObjectColumn<T> extends DBColumn<T> implements PhysicalColumnAdapter.Any {
  protected BaseObjectColumn(String name) {
    super(name);
  }

  public ObjectArrayAccessor getArrayAccessor() {
    return ObjectArrayAccessor.INSTANCE;
  }

  public T loadValue(Object storage, int row) {
    return (T) getArrayAccessor().getObjectValue(storage, row);
  }

  public Object storeNative(Object storage, int row, Object value) {
    return getArrayAccessor().setObjectValue(storage, row, toUserValue(value));
  }

  public Object toDatabaseValue(T userValue) {
    return userValue;
  }

  public T getValue(DBRowSet rowSet, int row) {
    return loadValue(rowSet.getColumnData(this), row);
  }

  public T getValue(DBRow row) {
    return row.getValue(this);
  }

  public List<T> getValuesList(DBRowSet result) {
    if (result.getRowCount() == 0)
      return Collections15.emptyList();
    List<T> list = Collections15.arrayList();
    Object columnData = result.getColumnData(this);
    Object[] data;
    if (!(columnData instanceof Object[])) {
      assert columnData == null : columnData;
      data = Const.EMPTY_OBJECTS;
    } else
      data = (Object[]) columnData;
    for (int i = 0; i < Math.min(data.length, result.getRowCount()); i++)
      list.add((T) data[i]);
    for (int i = data.length; i < result.getRowCount(); i++)
      list.add(null);
    return list;
  }
}
