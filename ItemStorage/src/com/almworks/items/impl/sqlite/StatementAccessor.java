package com.almworks.items.impl.sqlite;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public abstract class StatementAccessor<T> {
  public abstract void bind(SQLiteStatement statement, int index, T value) throws SQLiteException;

  public abstract T column(SQLiteStatement statement, int column) throws SQLiteException;

  public static <T> StatementAccessor<T> forClass(Class<T> valueClass) {
    if (Integer.class.equals(valueClass)) {
      return (StatementAccessor<T>) IntegerAccessor.INSTANCE;
    } else if (Long.class.equals(valueClass)) {
      return (StatementAccessor<T>) LongAccessor.INSTANCE;
    } else if (String.class.equals(valueClass)) {
      return (StatementAccessor<T>) StringAccessor.INSTANCE;
    } else {
      assert false : valueClass;
      return null;
    }
  }


  public static class IntegerAccessor extends StatementAccessor<Integer> {
    public static final IntegerAccessor INSTANCE = new IntegerAccessor();

    public void bind(SQLiteStatement statement, int index, Integer value) throws SQLiteException {
      if (value == null)
        statement.bindNull(index);
      else
        statement.bind(index, value);
    }

    public Integer column(SQLiteStatement statement, int column) throws SQLiteException {
      return statement.columnNull(column) ? null : statement.columnInt(column);
    }
  }


  public static class LongAccessor extends StatementAccessor<Long> {
    public static final LongAccessor INSTANCE = new LongAccessor();

    public void bind(SQLiteStatement statement, int index, Long value) throws SQLiteException {
      if (value == null)
        statement.bindNull(index);
      else
        statement.bind(index, value);
    }

    public Long column(SQLiteStatement statement, int column) throws SQLiteException {
      return statement.columnNull(column) ? null : statement.columnLong(column);
    }
  }


  public static class StringAccessor extends StatementAccessor<String> {
    public static final StringAccessor INSTANCE = new StringAccessor();

    public void bind(SQLiteStatement statement, int index, String value) throws SQLiteException {
      if (value == null)
        statement.bindNull(index);
      else
        statement.bind(index, value);
    }

    public String column(SQLiteStatement statement, int column) throws SQLiteException {
      return statement.columnNull(column) ? null : statement.columnString(column);
    }
  }
}
