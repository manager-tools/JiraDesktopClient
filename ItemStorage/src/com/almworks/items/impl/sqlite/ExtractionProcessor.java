package com.almworks.items.impl.sqlite;

import com.almworks.integers.*;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.impl.DBReaderImpl;
import com.almworks.items.impl.dbadapter.DBFilterInvalidException;
import com.almworks.items.impl.dbadapter.ItemVisitor;
import com.almworks.items.impl.sql.SQLItemSelect;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.filter.*;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteLongArray;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolOperation;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.almworks.items.impl.sqlite.filter.SingularExtractFunction.EXTRACT_ALL;
import static com.almworks.util.bool.BoolOperation.OR;
import static com.almworks.util.bool.Reductions.toDnf;

/**
 * ExtractionProcessor is a facade for the implementation of searching. You should create ExtractionProcessor
 * from your BoolExpr (resulting in a single {@link ExtractionOperator}), and then apply the operator either to
 * any input {@link ExtractionFunction}, or (preferrably) directly call one of the facade methods in this class
 * to run search.
 */
public class ExtractionProcessor {
  private final ExtractionOperator myExtractor;

  private ExtractionProcessor(ExtractionOperator extractor) {
    myExtractor = extractor;
  }

  @Override
  public String toString() {
    return myExtractor.toString();
  }

  public static ExtractionProcessor create(BoolExpr<DP> filter, TransactionContext context)
    throws DBFilterInvalidException
  {
    ExtractionOperator executor = createExtractor(filter, context);
    return new ExtractionProcessor(executor);
  }

  public static ExtractionOperator createExtractor(BoolExpr<DP> filter, TransactionContext context)
    throws DBFilterInvalidException
  {
    BoolExpr<DP> resolution = DP.resolve(filter, new DBReaderImpl(context), null);
    BoolExpr<DP> dnf = toDnf(resolution);
    return buildDisjunction(dnf, context);
  }

  private static ExtractionOperator buildDisjunction(BoolExpr<DP> expression, TransactionContext context)
    throws DBFilterInvalidException
  {
    BoolExpr.Operation<DP> or = expression.asOperation(OR);
    if (or != null) {
      if (or.isNegated())
        throw new DBFilterInvalidException();
      List<BoolExpr<DP>> disjuncts = or.getArguments();
      List<ExtractionOperator> operators = Collections15.arrayList(disjuncts.size());
      for (BoolExpr<DP> disjunct : disjuncts) {
        operators.add(buildConjunction(disjunct, context));
      }
      return new CombiningItemExtractor(operators, true);
    } else {
      return buildConjunction(expression, context);
    }
  }

  private static ExtractionOperator buildConjunction(BoolExpr<DP> expression, TransactionContext context)
    throws DBFilterInvalidException
  {
    BoolExpr.Operation<DP> and = expression.asOperation(BoolOperation.AND);
    if (and != null) {
      if (and.isNegated())
        throw new DBFilterInvalidException();
      List<BoolExpr<DP>> conjuncts = and.getArguments();
      List<ExtractionOperator> operators = Collections15.arrayList(conjuncts.size());
      for (BoolExpr<DP> conjunct : conjuncts) {
        operators.add(buildTerm(conjunct, context));
      }
      Collections.sort(operators, ExtractionOperator.PERFORMANCE_COMPARATOR);
      return new ChainItemExtractor(operators, true);
    } else {
      return buildTerm(expression, context);
    }
  }

  private static ExtractionOperator buildTerm(BoolExpr<DP> expression, TransactionContext context)
    throws DBFilterInvalidException
  {
    if (expression instanceof BoolExpr.Literal) {
      if (expression == BoolExpr.<DP>TRUE()) {
        return ExtractionOperator.ALL;
      } else {
        return ExtractionOperator.NONE;
      }
    }
    DP filter = expression.getTerm();
    if (filter == null)
      throw new DBFilterInvalidException();
    boolean negated = expression.isNegated();
    // todo priorities for factories - in case there will be conflicting factories
    for (ExtractionOperatorFactory factory : context.getDatabaseContext().getConfiguration().getFilterConvertors()) {
      ExtractionOperator r = factory.convert(filter, negated, context);
      if (r != null)
        return r;
    }
    throw new DBFilterInvalidException("Unexecutable expression: " + filter);
  }

  public void visitItems(TransactionContext context, final ItemVisitor visitor) throws SQLiteException {
    ExtractionFunction f = myExtractor.apply(context, EXTRACT_ALL);
    f.execute(context, new ExtractionVisitor() {
      public void visitStarted(TransactionContext context) {
      }

      public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
        SQLItemSelect select = sql.build();
        select.visitItems(context, visitor);
      }

      public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
        visitor.visitItems(items);
      }

      public void visitFinished(TransactionContext context) {
      }
    });
  }

  public LongList distributionCount(TransactionContext context, DBAttribute<?>... groupAttributes)
    throws SQLiteException
  {
    LongSetBuilder builder = new LongSetBuilder();
    loadItems(context, builder);
    LongList items = builder.commitToArray();
    return ItemsDistribution.distributionCount(context, items, groupAttributes);
  }

  public long count(TransactionContext context) throws SQLiteException {
    CollectingExtractionVisitor v = executeAndCollect(context);

    List<SQLItemSelectBuilder> builders = v.getBuilders();
    LongList itemsSorted = v.getItemsSorted();

    if (itemsSorted == null || itemsSorted.isEmpty()) {
      if (builders == null || builders.isEmpty()) {
        return 0;
      }
      if (builders.size() == 1) {
        return builders.get(0).countDistinctIds(context);
      }
    }

    if (builders == null || builders.isEmpty()) {
      return itemsSorted == null ? 0 : itemsSorted.size();
    }

    LongSetBuilder set = new LongSetBuilder();
    if (itemsSorted != null) {
      set.mergeFromSortedCollection(itemsSorted);
    }
    for (SQLItemSelectBuilder b : builders) {
      b.build().loadItems(context, set);
    }
    return set.size();
  }

  private CollectingExtractionVisitor executeAndCollect(TransactionContext context) throws SQLiteException {
    ExtractionFunction f = myExtractor.apply(context, EXTRACT_ALL);
    CollectingExtractionVisitor v = new CollectingExtractionVisitor();
    f.execute(context, v);
    return v;
  }

  public <T extends LongCollector> T filterItems(TransactionContext context, LongIterable items, final T filtered)
    throws SQLiteException
  {
    ExtractionFunction f = myExtractor.apply(context, new ItemIterableFunction(items));
    f.execute(context, new ExtractionVisitor() {
      public void visitStarted(TransactionContext context) {
      }

      public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
        SQLItemSelect select = sql.build();
        select.visitItems(context, new ItemVisitor.Collector(filtered));
      }

      public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
        filtered.addAll(items.iterator());
      }

      public void visitFinished(TransactionContext context) {
      }
    });
    return filtered;
  }


  public boolean checkItem(TransactionContext context, final long item) throws SQLiteException {
    if (item <= 0)
      return false;
    ExtractionFunction f = myExtractor.apply(context, new ItemIterableFunction(LongArray.create(item)));
    final boolean[] there = {false};
    f.execute(context, new ExtractionVisitor() {
      public void visitStarted(TransactionContext context) {
      }

      public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
        SQLItemSelect select = sql.build();
        ItemVisitor.Single v = new ItemVisitor.Single();
        select.visitItems(context, v);
        if (v.getItem() == item) {
          there[0] = true;
        }
      }

      public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
        for (LongIterator ii = items.iterator(); ii.hasNext();) {
          if (ii.nextValue() == item) {
            there[0] = true;
          }
        }
      }

      public void visitFinished(TransactionContext context) {
      }
    });
    return there[0];
  }

  public void insertInto(TransactionContext context, final String tableName, final String columnName,
    @Nullable ExtractionFunction input) throws SQLiteException
  {
    ExtractionFunction f = apply(context, input);
    f.execute(context, new ExtractionVisitor() {
      public void visitStarted(TransactionContext context) {
      }

      public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
        SQLItemSelect select = sql.build();
        select.insertInto(context, tableName, columnName);
      }

      public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
        SQLiteLongArray array = null;
        SQLiteStatement st = null;
        try {
          array = context.getConnection().createArray();
          st = context.prepare(context.sql()
            .append("INSERT OR IGNORE INTO ")
            .append(tableName)
            .append(" (")
            .append(columnName)
            .append(") SELECT value FROM").append(array.getName()));
          context.bindIterable(array, items, false, false);
          context.addCancellable(st);
          st.stepThrough();
        } finally {
          context.removeCancellable(st);
          if (st != null)
            st.dispose();
          if (array != null)
            array.dispose();
        }
      }

      public void visitFinished(TransactionContext context) {
      }
    });
  }

  /**
   * Visited items are not distinct nor sorted - collector must process
   */
  public void loadItems(TransactionContext context, final LongCollector collector) throws SQLiteException {
    loadItems(context, collector, SingularExtractFunction.EXTRACT_ALL);
  }

  /**
   * Visited items are not distinct nor sorted - collector must process
   */
  public void loadItems(TransactionContext context, final LongCollector collector, ExtractionFunction input)
    throws SQLiteException
  {
    ExtractionFunction f = apply(context, input);
    f.execute(context, new ExtractionVisitor() {
      public void visitStarted(TransactionContext context) {
      }

      public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
        sql.build().loadItems(context, collector);
      }

      public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
        collector.addAll(items.iterator());
      }

      public void visitFinished(TransactionContext context) {
      }
    });
  }

  protected ExtractionFunction apply(TransactionContext context, ExtractionFunction input) {
    return myExtractor.apply(context, input == null ? EXTRACT_ALL : input);
  }
}
