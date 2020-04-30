package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.impl.AttributeAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBColumnType;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteLongArray;
import com.almworks.sqlite4java.SQLiteStatement;

/**
 * @author Igor Sereda
 */
public class ItemsDistribution {

  public static LongList distributionCount(TransactionContext context, LongList itemsSorted,
    DBAttribute<?>... groupAttributes) throws SQLiteException
  {
    if (groupAttributes == null || groupAttributes.length == 0)
      return LongList.EMPTY;
    if (itemsSorted == null || itemsSorted.isEmpty())
      return LongList.EMPTY;
    SQLiteLongArray array = null;
    SQLiteStatement st = SQLiteStatement.DISPOSED;
    try {
      int dim = groupAttributes.length;
      String[] tableNames = new String[dim];
      SQLParts parts = new SQLParts("SELECT count(*)");
      int actualDims = 0;
      for (int i = 0; i < dim; i++) {
        DBAttribute<?> attribute = groupAttributes[i];
        AttributeAdapter adapter = context.getDatabaseContext().getAttributeAdapter(attribute);
        if (adapter.getScalarColumn().getDatabaseClass() != DBColumnType.INTEGER) {
          throw new IllegalArgumentException("cannot calculate distribution by attribute " + attribute);
        }
        tableNames[i] = context.getTableName(adapter.getTable(), false);
        if (tableNames[i] != null) {
          actualDims++;
          parts.append(", t")
            .append(String.valueOf(actualDims))
            .append(".")
            .append(adapter.getScalarColumn().getName());
        }
      }
      if (actualDims == 0) {
        // no tables
        LongArray r = new LongArray();
        for (int i = 0; i < dim; i++)
          r.add(0);
        r.add(itemsSorted.size());
        return r;
      }

      array = context.useArray(itemsSorted, true, true);
      parts.append("\n  FROM ").append(array.getName()).append(" t0");
      int j = 0;
      String iid = DBColumn.ITEM.getName();
      for (int i = 0; i < dim; i++) {
        if (tableNames[i] != null) {
          j++;
          String alias = "t" + j;
          parts.append("\n  LEFT OUTER JOIN ")
            .append(tableNames[i])
            .append(" ")
            .append(alias)
            .append(" ON t0.value=")
            .append(alias)
            .append(".")
            .append(iid);
        }
      }
      parts.append("\n  GROUP BY ");
      String prefix = "";
      for (int i = 0; i < actualDims; i++) {
        parts.append(prefix).append(String.valueOf(2 + i));
        prefix = ", ";
      }

      st = context.prepare(parts);
      context.addCancellable(st);
      LongArray r = new LongArray();
      while (st.step()) {
        int count = st.columnInt(0);
        int k = 0;
        for (int i = 0; i < dim; i++) {
          if (tableNames[i] == null) {
            r.add(0);
          } else {
            k++;
            if (st.columnNull(k)) {
              r.add(0);
            } else {
              r.add(st.columnLong(k));
            }
          }
        }
        r.add(count);
      }

      return r;
    } finally {
      if (array != null)
        array.dispose();
      context.removeCancellable(st);
      st.dispose();
    }
  }
}
