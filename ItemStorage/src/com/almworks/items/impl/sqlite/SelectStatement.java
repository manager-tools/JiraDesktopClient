package com.almworks.items.impl.sqlite;

import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Collections15;

import java.util.List;

public class SelectStatement {
  private final SQLParts myParts;
  private final Object[] myParameterValues;
  private final int mySkipBindCount;

  private SelectStatement(SQLParts parts, Object[] parameterValues, int skipBindCount) {
    myParts = parts;
    myParameterValues = parameterValues;
    mySkipBindCount = skipBindCount;
  }

  public SQLiteStatement prepare(TransactionContext context) throws SQLiteException {
    SQLiteStatement stmt = context.prepare(myParts);
    try {
      bindParams(stmt);
    } catch (SQLiteException e) {
      stmt.dispose();
      throw e;
    }
    return stmt;
  }

  private void bindParams(SQLiteStatement stmt) throws SQLiteException {
    for (int i = 0; i < myParameterValues.length; i++) {
      Object value = myParameterValues[i];
      SQLUtil.bindParameter(stmt, i + mySkipBindCount + 1, value);
    }
  }

  public SQLiteStatement prependAndPrepare(TransactionContext context, String sqlPrefix) throws SQLiteException {
    assert sqlPrefix.indexOf('?') < 0;
    SQLParts parts = new SQLParts();
    parts.append(sqlPrefix);
    // todo optimize!!! SQLParts.append(SQLParts)
    parts.append(myParts.toString());
    SQLiteStatement stmt = context.prepare(parts);
    boolean success = false;
    try {
      bindParams(stmt);
      success = true;
    } finally {
      if (!success)
        stmt.dispose();
    }
    return stmt;
  }

  public String toString() {
    return myParts.toString();
  }

  public static class Builder {
    private final SQLParts mySQL = new SQLParts();
    private final List<Object> myParameters = Collections15.arrayList();
    private int mySkipBindCount;

    public Builder append(String sqlFragment) {
      // todo better representation of SQL part with bound parameter
//      assert sqlFragment.indexOf('?') < 0 : sqlFragment;
      mySQL.append(sqlFragment);
      return this;
    }

    public Builder appendJoin(boolean inner, String joinColumn, String table, String column) {
      String join = inner ? " INNER JOIN " : " LEFT OUTER JOIN ";
      mySQL.append(join)
        .append(table)
        .append(" ON ")
        .append(joinColumn)
        .append("=")
        .append(table)
        .append(".")
        .append(column);
      return this;
    }

    public void appendValue(long value) {
      mySQL.appendParams(1);
      myParameters.add(value);
    }

    public void appendValue(String value) {
      mySQL.appendParams(1);
      myParameters.add(value);
    }

    public Builder appendValues(int[] values) {
      if (values == null || values.length == 0)
        return this;
      assert values.length <= SQLUtil.MAX_SQL_PARAMS : values.length;
      mySQL.appendParams(values.length);
      for (int value : values)
        myParameters.add(value);
      return this;
    }

    public SelectStatement create() {
      return new SelectStatement(mySQL.fix(), myParameters.toArray(), mySkipBindCount);
    }

    public void setSkipBindCount(int index) {
      mySkipBindCount = index;
      assert myParameters.isEmpty();
    }

    public String toString() {
      return mySQL.toString();
    }

    public SQLParts getParts() {
      return mySQL;
    }

    // todo unsafe
    // todo remove method and class
    public List<Object> getParameterValues() {
      return myParameters;
    }
  }
}
