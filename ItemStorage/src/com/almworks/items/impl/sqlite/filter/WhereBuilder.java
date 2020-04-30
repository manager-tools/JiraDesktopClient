package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongSetBuilder;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBColumnType;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.SQLUtil;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Log;

import java.util.List;

public abstract class WhereBuilder {
  public static final Exists EXISTS = new Exists();

  public abstract void addWhere(SQLItemSelectBuilder sql, SQLItemSelectBuilder.Join join, boolean negated);

  protected static SQLParts appendColumn(SQLParts parts, SQLItemSelectBuilder.Join join, DBColumn column) {
    parts.append(join.getAlias()).append(".").append(column.getName());
    return parts;
  }

  public static class Equals extends WhereBuilder {
    private final DBColumn myColumn;
    private final Object myValue;

    public Equals(DBColumn column, Object value) {
      myColumn = column;
      myValue = value;
    }

    public DBColumn getColumn() {
      return myColumn;
    }

    @Override
    public void addWhere(SQLItemSelectBuilder sql, SQLItemSelectBuilder.Join join, boolean negated) {
      SQLPartsParameterized where = sql.addWhere();
      SQLParts parts = where.getParts();
      if (!negated) {
        appendColumn(parts, join, myColumn).append(" = ?");
      } else {
        parts.append("(");
        appendColumn(parts, join, myColumn).append(" != ? OR ");
        appendColumn(parts, join, myColumn).append(" IS NULL)");
      }
      where.addParameters(myValue);
    }
  }

  public static class Exists extends WhereBuilder {
    @Override
    public void addWhere(SQLItemSelectBuilder sql, SQLItemSelectBuilder.Join join, boolean negated) {
      if (!negated) {
        // do nothing - inner join does
      } else {
        appendColumn(sql.addWhere().getParts(), join, DBColumn.ITEM).append(" IS NULL");
      }
    }
  }

  // negated can be used only for scalar columns!
  public static class EqualsOneOf extends WhereBuilder {
    private static final int USE_ARRAY_THRESHOLD = 40;
    private final DBColumn myColumn;
    private final List<?> myValues;

    public EqualsOneOf(DBColumn column, List<?> values) {
      if (values.isEmpty()) throw new IllegalArgumentException();
      if (column.getDatabaseClass() != DBColumnType.INTEGER && values.size() > SQLUtil.MAX_SQL_PARAMS) throw new IllegalArgumentException("column " + column + " count " + values.size());
      myColumn = column;
      myValues = values;
    }

    @Override
    public void addWhere(SQLItemSelectBuilder sql, SQLItemSelectBuilder.Join join, boolean negated) {
      List<?> values = myValues;
      boolean useArray = myColumn.getDatabaseClass() == DBColumnType.INTEGER && values.size() > USE_ARRAY_THRESHOLD;
      Procedure<SQLParts> addTableDefinition;
      SQLPartsParameterized where = sql.addWhere();
      if (useArray) {
        LongSetBuilder lsb = new LongSetBuilder();
        for (Object value : values) {
          if (!(value instanceof Number)) continue;
          lsb.add(((Number) value).longValue());
        }
        // todo could be passed to sqlite4java as unique, sorted array
        final String array = sql.arrayParameter(lsb.commitToArray());
        addTableDefinition = new Procedure<SQLParts>() {
          public void invoke(SQLParts arg) {
            arg.append(array);
          }
        };
      } else {
        if (values.size() > SQLUtil.MAX_SQL_PARAMS) {
          // this case should be cut off at the extractor building phase
          // nothing to do here - show error and try using first N parameters
          Log.error("failed EqualsOneOf builder: " + values.size() + " " + myColumn);
          values = values.subList(0, SQLUtil.MAX_SQL_PARAMS);
        }
        final List<?> finalValues = values;
        addTableDefinition = new Procedure<SQLParts>() {
          public void invoke(SQLParts arg) {
            arg.append("(").appendParams(finalValues.size()).append(")");
          }
        };
        where.addParameters(values.toArray());
      }
      SQLParts parts = where.getParts();
      if (!negated) {
        appendColumn(parts, join, myColumn).append(" IN ");
        addTableDefinition.invoke(parts);
      } else {
        parts.append("(");
        appendColumn(parts, join, myColumn).append(" NOT IN ");
        addTableDefinition.invoke(parts);
        parts.append(" OR ");
        appendColumn(parts, join, myColumn).append(" IS NULL)");
      }
    }
  }

  public static class Compare extends WhereBuilder {
    private final DBColumn myColumn;
    private final Object myValue;
    private final boolean myAcceptLess;
    private final boolean myAcceptEquals;
    private final boolean myAcceptNull;

    public Compare(DBColumn column, Object value, boolean acceptLess, boolean acceptEquals, boolean acceptNull) {
      myColumn = column;
      myValue = value;
      myAcceptLess = acceptLess;
      myAcceptEquals = acceptEquals;
      myAcceptNull = acceptNull;
    }

    @Override
    public void addWhere(SQLItemSelectBuilder sql, SQLItemSelectBuilder.Join join, boolean negated) {
      SQLPartsParameterized where = sql.addWhere();
      SQLParts parts = where.getParts();
      String comparison = myAcceptLess ? "<" : ">";
      if (myAcceptNull) parts.append("(");
      if (myAcceptEquals) comparison = comparison + "=";
      appendColumn(parts, join, myColumn).append(comparison).append(" ? ");
      if (myAcceptNull) {
        parts.append(" OR ");
        appendColumn(parts, join, myColumn)
          .append(" IS NULL)");
      }
      where.addParameters(myValue);
    }
  }
}
