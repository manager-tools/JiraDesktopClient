package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DP;
import com.almworks.items.impl.CombiningIterable;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBFilterInvalidException;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.sql.SQLItemSelect;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.ExtractionProcessor;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.bool.BoolExpr;

import java.util.List;

public class JoinSubqueryOperator extends ExtractionOperator {
  private final DBTable myReferringTable;
  private final DBColumn myReferringColumn;
  private final ExtractionOperator mySubquery;
  private final boolean myNegated;

  public JoinSubqueryOperator(DBTable referringTable, DBColumn referringColumn,
    BoolExpr<DP> referentsPredicate, boolean negated, TransactionContext context)
  {
    myNegated = negated;
    myReferringTable = referringTable;
    myReferringColumn = referringColumn;
    BoolExpr<DP> constraint = referentsPredicate;
    ExtractionOperator extractor;
    try {
      extractor = ExtractionProcessor.createExtractor(constraint, context);
    } catch (DBFilterInvalidException e) {
      extractor = NONE;
    }
    mySubquery = extractor;
  }

  @Override
  public int getPerformanceHit() {
    if (mySubquery.equals(ExtractionOperator.NONE))
      return -90;
    if (mySubquery.equals(ExtractionOperator.ALL))
      return -2;
    return 2;
  }

  @Override
  public ExtractionFunction apply(TransactionContext context, ExtractionFunction input) {
    return new JoinExtractionFunction(input);
  }


  private class JoinExtractionFunction extends ExtractionFunction {
    private final ExtractionFunction myInput;
    private String myTableName;
    // one of these is null:
    private List<SQLItemSelectBuilder> mySubqueryBuilders;
    private LongList mySubqueryItemsSorted;

    public JoinExtractionFunction(ExtractionFunction input) {
      myInput = input;
    }

    public final void execute(TransactionContext context, final ExtractionVisitor visitor) throws SQLiteException
    {
      if (!preExecute(context, visitor)) {
        return;
      }
      myInput.execute(context, new ExtractionVisitor() {
        public void visitStarted(TransactionContext context) {
          visitor.visitStarted(context);
        }

        public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
          if (mySubqueryItemsSorted != null && !myNegated) {
            // can join the referring table by referring column and add "where" iterator
            SQLItemSelectBuilder.Join join = sql.joinPrimaryTable(myTableName, myReferringColumn.getName(), false);
            sql.whereColumnValueInList(mySubqueryItemsSorted, join.getAlias(), DBColumn.ITEM.getName());
            visitor.visitSQL(context, sql);
          } else if (mySubqueryBuilders != null && !myNegated) {
            // cached input items, if needed
            for (SQLItemSelectBuilder subqueryBuilder : mySubqueryBuilders) {
              subqueryBuilder = subqueryBuilder.clone();
              // join two SQL
              SQLItemSelectBuilder builder = sql.clone();
              SQLItemSelectBuilder.Join join =
                builder.joinPrimaryTable(myTableName, myReferringColumn.getName(), false);
              join.joinSecondaryQueryInner(DBColumn.ITEM.getName(), subqueryBuilder);
              visitor.visitSQL(context, builder);
            }
          } else {
            // default to visitItems - todo may be it's more effective
            visitItems(context, sql.loadItems(context));
          }
        }

        public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
          if (mySubqueryItemsSorted != null) {
            LongList referredItems = getReferredItems(context, mySubqueryItemsSorted);
            visitor.visitItems(context, new CombiningIterable(items, referredItems, myNegated));
          } else if (mySubqueryBuilders != null) {
            // todo optimize
            LongList referredItems = getAllReferredItems(context);
            visitor.visitItems(context, new CombiningIterable(items, referredItems, myNegated));
          } else assert false;
        }

        public void visitFinished(TransactionContext context) throws SQLiteException {
          visitor.visitFinished(context);
        }
      });
    }

    private LongList getAllReferredItems(TransactionContext context) throws SQLiteException {
      LongSetBuilder referredBuilder = new LongSetBuilder();
      for (SQLItemSelectBuilder subqueryBuilder : mySubqueryBuilders) {
        subqueryBuilder = subqueryBuilder.clone();
        adjustSubqueryToLoadReferredItems(subqueryBuilder);
        subqueryBuilder.build().loadItems(context, referredBuilder);
      }
      return referredBuilder.commitToArray();
    }

    private void adjustSubqueryToLoadReferredItems(SQLItemSelectBuilder subqueryBuilder) {
      SQLItemSelectBuilder.Join join = subqueryBuilder.joinPrimaryTable(myTableName, DBColumn.ITEM.getName(), false);
      subqueryBuilder.setSelectedColumn(join.getAlias(), myReferringColumn.getName());
    }

    private LongList getReferredItems(TransactionContext context, LongList subqueryItems) throws SQLiteException {
      SQLItemSelectBuilder builder = new SQLItemSelectBuilder();
      SQLItemSelectBuilder.Join join = builder.joinPrimaryTable(myTableName, myReferringColumn.getName(), false);
      builder.whereColumnValueInList(subqueryItems, join.getAlias(), DBColumn.ITEM.getName());
      LongSetBuilder refbuilder = new LongSetBuilder();
      builder.build().loadItems(context, refbuilder);
      return refbuilder.commitToArray();
    }

    protected boolean preExecute(TransactionContext context, ExtractionVisitor visitor)
      throws SQLiteException
    {
      myTableName = context.getTableName(myReferringTable, false);
      if (myTableName == null) {
        // don't execute
        conditionFalse(context, visitor);
        return false;
      }

      if (mySubquery.equals(ExtractionOperator.NONE)) {
        conditionFalse(context, visitor);
        return false;
      }

      CollectingExtractionVisitor subResult;
      try {
        ExtractionFunction function = mySubquery.apply(context, SingularExtractFunction.EXTRACT_ALL);
        subResult = new CollectingExtractionVisitor();
        function.execute(context, subResult);
      } catch (SQLiteException e) {
        throw new SQLiteException(-111, "", e);
      }
      LongList subqueryItems = subResult.getItemsSorted();
      List<SQLItemSelectBuilder> subqueryBuilders = subResult.getBuilders();
      if (subqueryBuilders == null && subqueryItems == null) {
        conditionFalse(context, visitor);
        return false;
      }

      if (subqueryItems != null && subqueryBuilders != null) {
        LongSetBuilder builder = new LongSetBuilder();
        for (SQLItemSelectBuilder subqueryBuilder : subqueryBuilders) {
          SQLItemSelect select = subqueryBuilder.build();
          select.loadItems(context, builder);
        }
        builder.addAll(subqueryItems);
        subqueryItems = builder.commitToArray();
        subqueryBuilders = null;
      }

      if (mySubqueryItemsSorted != null && mySubqueryItemsSorted.isEmpty()) {
        conditionFalse(context, visitor);
        return false;
      }

      // one of these should be null
      mySubqueryBuilders = subqueryBuilders;
      mySubqueryItemsSorted = subqueryItems;

      return true;
    }

    private void conditionFalse(TransactionContext context, ExtractionVisitor visitor)
      throws SQLiteException
    {
      if (myNegated) {
        // pass execution to input: no filtering here
        myInput.execute(context, visitor);
      } else {
        // nothing is returned
        visitor.visitStarted(context);
        visitor.visitFinished(context);
      }
    }
  }
}
