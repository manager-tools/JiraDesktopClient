package com.almworks.items.impl.sqlite;

import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.impl.DBReaderImpl;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.filter.CollectingExtractionVisitor;
import com.almworks.items.impl.sqlite.filter.ExtractionFunction;
import com.almworks.items.impl.sqlite.filter.SingularExtractFunction;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.bool.BoolExpr;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PredicateConversionTests extends MemoryDatabaseFixture {
  private TransactionContext myContext;

  public void test() {
    db.writeBackground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        // create tables
        TestData.writeMap(TestData.ITEM1, TestData.VALUESET1, writer);
        return null;
      }
    }).waitForCompletion();
    db.readBackground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        try {
          myContext = ((DBReaderImpl) reader).getContext();
          checkSinglePredicates();
          checkAnds();
          checkOrs();
        } catch (SQLiteException e) {
          throw new DBException(e);
        } finally {
          myContext = null;
        }
        return null;
      }
    }).waitForCompletion();
  }

  private void checkSinglePredicates() throws SQLiteException {
    checkSql(DPEquals.create(TestData.STRING, "x"), "SELECT _T0.ITEM R FROM STRING _T0 WHERE (_T0.VALUE = ?)");
    checkSql(DPEqualsIdentified.create(TestData.LINK, TestData.STRING),
      "SELECT _T0.ITEM R FROM LINK _T0 WHERE (_T0.VALUE = ?)");
  }

  private void checkAnds() throws SQLiteException {
    checkSql(DPEquals.create(TestData.STRING, "x").and(DPEqualsIdentified.create(TestData.LINK, TestData.STRING)),
      "SELECT _T0.ITEM R FROM LINK _T0 INNER JOIN STRING _T1 ON _T0.ITEM=_T1.ITEM " +
        "WHERE (_T0.VALUE = ?) AND (_T1.VALUE = ?)");
  }


  private void checkOrs() throws SQLiteException {
    checkSql(DPEquals.create(TestData.STRING, "x").or(DPEqualsIdentified.create(TestData.LINK, TestData.STRING)),
      "SELECT _T0.ITEM R FROM LINK _T0 WHERE (_T0.VALUE = ?)",
      "SELECT _T0.ITEM R FROM STRING _T0 WHERE (_T0.VALUE = ?)");
  }

  private void checkSql(BoolExpr<DP> expr, String... sql) throws SQLiteException {
    CollectingExtractionVisitor collector = collect(expr, myContext);
    assertNull(collector.getItemsSorted());
    Set<String> sqls = new HashSet<String>();
    for (SQLItemSelectBuilder builder : collector.getBuilders()) {
      sqls.add(normalizeSql(builder.build().toString()));
    }
    Set<String> expected = new HashSet<String>();
    for (String s : sql) {
      expected.add(normalizeSql(s));
    }
//    new CollectionsCompare().unordered(expected, sqls);
    assertEquals(expr.toString(), expected, sqls);
  }

  private void checkSql(String sql1, String sql2) {
    assertEquals(normalizeSql(sql1), normalizeSql(sql2));
  }

  private String normalizeSql(String sql) {
    return sql.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
  }

  private CollectingExtractionVisitor collect(BoolExpr<DP> q, TransactionContext context) throws SQLiteException {
    ExtractionProcessor ep = ExtractionProcessor.create(q, context);
    ExtractionFunction f = ep.apply(context, SingularExtractFunction.EXTRACT_ALL);
    CollectingExtractionVisitor collector = new CollectingExtractionVisitor();
    f.execute(context, collector);
    return collector;
  }
}
