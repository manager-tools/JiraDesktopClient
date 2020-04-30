package com.almworks.items.impl;

import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DP;
import com.almworks.items.impl.dbadapter.ItemVisitor;
import com.almworks.items.impl.sql.SQLItemSelect;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.impl.sqlite.filter.ExtractionFunction;
import com.almworks.items.impl.sqlite.filter.ExtractionOperator;
import com.almworks.items.impl.sqlite.filter.ExtractionOperatorFactory;
import com.almworks.items.impl.sqlite.filter.ExtractionVisitor;
import com.almworks.sqlite4java.SQLiteException;

public class DefaultExtractionFactory implements ExtractionOperatorFactory {
  public ExtractionOperator convert(final DP predicate, final boolean negated, TransactionContext transactionContext) {
    return new ExtractionOperator() {
      @Override
      public int getPerformanceHit() {
        return 20;
      }

      public ExtractionFunction apply(TransactionContext context, final ExtractionFunction input) {
        return new ExtractionFunction() {
          @Override
          public void execute(TransactionContext context, final ExtractionVisitor visitor) throws SQLiteException {
            final DBReaderImpl wrapper = new DBReaderImpl(context);
            input.execute(context, new ExtractionVisitor() {
              public void visitStarted(TransactionContext context) {
                visitor.visitStarted(context);
              }

              public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
                SQLItemSelect select = sql.build();
                final LongSetBuilder builder = new LongSetBuilder();
                select.visitItems(context, new ItemVisitor.ForEachItem() {
                  @Override
                  protected boolean visitItem(long item) throws SQLiteException {
                    if (predicate.accept(item, wrapper) ^ negated) {
                      builder.add(item);
                    }
                    return true;
                  }
                });
                visitor.visitItems(context, builder.commitToArray());
              }

              public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
                final LongSetBuilder builder = new LongSetBuilder();
                for (LongIterator ii = items.iterator(); ii.hasNext();) {
                  long item = ii.nextValue();
                  if (predicate.accept(item, wrapper) ^ negated) {
                    builder.add(item);
                  }
                }
                visitor.visitItems(context, builder.commitToArray());
              }

              public void visitFinished(TransactionContext context) throws SQLiteException {
                visitor.visitFinished(context);
              }
            });
          }
        };
      }
    };
  }
}
