package com.almworks.items.impl.sql;

import com.almworks.integers.LongCollector;
import com.almworks.integers.LongIterable;
import com.almworks.items.impl.dbadapter.ItemVisitor;
import com.almworks.items.impl.sqlite.SQLUtil;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteLongArray;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SQLItemSelect {
  private final SQLParts myParts;
  @Nullable
  private final Map<String, LongIterable> myArrayParameters;
  private final List<?> myArguments;

  public SQLItemSelect(SQLParts parts, Map<String, LongIterable> arrayParameters, List<?> arguments) {
    myParts = parts;
    myArrayParameters = arrayParameters;
    myArguments = arguments;
    if (arguments != null && arguments.size() > 100) {
      Log.warn("too many parameters on query " + parts + " (" + myArguments.size() + ")");
    }
  }

  public void loadItems(TransactionContext ctx, final LongCollector collector) throws SQLiteException {
    runStatement(ctx, myParts, new StatementProcedure() {
      public void apply(TransactionContext context, SQLiteStatement statement) throws SQLiteException {
        SQLUtil.loadLongs(statement, context, collector);
      }
    });
  }

  long count(TransactionContext ctx) throws SQLiteException {
    final long[] r = {0};
    runStatement(ctx, myParts, new StatementProcedure() {
      public void apply(TransactionContext context, SQLiteStatement statement) throws SQLiteException {
        if (statement.step()) {
          long v = statement.columnLong(0);
          r[0] += v;
        } else {
          Log.warn("counting did not return value");
        }
      }
    });
    return r[0];
  }


  public void visitItems(TransactionContext context, final ItemVisitor visitor)
    throws SQLiteException
  {
    runStatement(context, myParts, new StatementProcedure() {
      @Override
      public void apply(TransactionContext context, SQLiteStatement statement) throws SQLiteException {
        SQLUtil.loadItems(statement, context, visitor);
      }
    });
  }

  public void insertInto(TransactionContext context, String tableName, String columnName) throws SQLiteException {
    SQLParts insert = new SQLParts();
    insert.append("INSERT OR IGNORE INTO ").append(tableName).append(" (").append(columnName).append(") \n");
    insert.append(myParts);
    runStatement(context, insert, new StatementProcedure() {
      @Override
      public void apply(TransactionContext context, SQLiteStatement statement) throws SQLiteException {
        statement.step();
      }
    });
  }


  private void runStatement(TransactionContext context, SQLParts sql, StatementProcedure procedure)
    throws SQLiteException
  {
    List<SQLiteLongArray> arrays = null;
    SQLiteStatement statement = null;
    if (myArrayParameters != null) {
      arrays = Collections15.arrayList();
      sql = useArrays(context, sql, myArrayParameters, arrays);
    }
    try {
      statement = context.prepare(sql);
      context.addCancellable(statement);
      bindArguments(statement, 0);
      procedure.apply(context, statement);
    } finally {
      context.removeCancellable(statement);
      if (statement != null) {
        statement.dispose();
      }
      if (arrays != null) {
        for (SQLiteLongArray array : arrays) {
          array.dispose();
        }
      }
    }
  }

  private SQLParts useArrays(TransactionContext context, SQLParts sql, Map<String, LongIterable> parameters,
    List<SQLiteLongArray> arrays) throws SQLiteException
  {
    for (Map.Entry<String, LongIterable> e : parameters.entrySet()) {
      SQLiteLongArray array = context.useIterable(e.getValue());
      arrays.add(array);
      sql = SQLUtil.replacePart(sql, e.getKey(), array.getName());
    }
    return sql;
  }

  private void bindArguments(SQLiteStatement st, int offset) throws SQLiteException {
    if (myArguments != null) {
      for (int i = 0; i < myArguments.size(); i++) {
        Object arg = myArguments.get(i);
        SQLUtil.bindParameter(st, i + offset + 1, arg);
      }
    }
  }

  @Override
  public String toString() {
    return myParts.toString();
  }

  private static abstract class StatementProcedure {
    public abstract void apply(TransactionContext context, SQLiteStatement statement)
      throws SQLiteException;
  }
}

