package com.almworks.items.impl.scalars;

import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Log;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;

// todo to make it sortable (and indexable), choose storage form or collation for correct comparison
// todo currently does not persist scale - should be fixed
public class ScalarAdapterDecimal extends ScalarValueAdapter<BigDecimal> {
  @Override
  public Class<BigDecimal> getAdaptedClass() {
    return BigDecimal.class;
  }

  @Override
  public DBColumn getScalarColumn() {
    return STRING_VALUE;
  }

  @Override
  public boolean isIndexable() {
    return false;
  }

  @Override
  public BigDecimal loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    return toUserValue(select.columnString(columnIndex));
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, BigDecimal userValue, TransactionContext context)
    throws SQLiteException
  {
    if (userValue == null) {
      Log.error("cannot bind null");
      userValue = BigDecimal.ZERO;
    }
    statement.bind(bindIndex, toDatabaseValue(userValue));
  }

  @Override
  protected BigDecimal readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    return toUserValue(CompactChar.readString(in));
  }

  @Override
  protected void writeValueToStream(DataOutput out, BigDecimal userValue, TransactionContext context) throws IOException {
    CompactChar.writeString(out, toDatabaseValue(userValue));
  }

  @Override
  public Object toSearchValue(BigDecimal userValue) {
    return toDatabaseValue(userValue);
  }

  private String toDatabaseValue(BigDecimal userValue) {
    return DatabaseUtil.decimalToString(userValue);
  }

  private BigDecimal toUserValue(String dbValue) {
    if (dbValue == null) return null;
    try {
      return new BigDecimal(dbValue);
    } catch (NumberFormatException e) {
      Log.warn("cannot convert [" + dbValue + "] to number", e);
      return null;
    }
  }
}

