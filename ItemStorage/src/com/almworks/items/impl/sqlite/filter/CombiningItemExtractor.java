package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

import java.util.List;

public class CombiningItemExtractor extends CompositeExtractor {
  public CombiningItemExtractor(List<ExtractionOperator> operators, boolean reuseList) {
    super(operators, reuseList);
  }

  public ExtractionFunction apply(TransactionContext context, ExtractionFunction input) {
    List<ExtractionOperator> operators = getOperators();
    if (operators.isEmpty())
      return input;
    return new CombiningExtractionFunction(input, operators);
  }

  private static class CombiningExtractionFunction extends ExtractionFunction {
    private final ExtractionFunction myInput;
    private final List<ExtractionOperator> myOperators;

    public CombiningExtractionFunction(ExtractionFunction input, List<ExtractionOperator> operators) {
      myInput = input;
      myOperators = operators;
    }

    @Override
    public void execute(TransactionContext context, ExtractionVisitor visitor)
      throws SQLiteException
    {
      CombiningVisitor v = new CombiningVisitor(visitor);
      for (ExtractionOperator operator : myOperators) {
        ExtractionFunction f = operator.apply(context, myInput);
        f.execute(context, v);
      }
    }
  }


  private static class CombiningVisitor implements ExtractionVisitor {
    private int myStartCount;
    private final ExtractionVisitor myDelegate;

    public CombiningVisitor(ExtractionVisitor delegate) {
      myDelegate = delegate;
    }

    public void visitStarted(TransactionContext context) {
      if (myStartCount++ == 0)
        myDelegate.visitStarted(context);
    }

    public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
      myDelegate.visitSQL(context, sql);
    }

    public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
      myDelegate.visitItems(context, items);
    }

    public void visitFinished(TransactionContext context) throws SQLiteException {
      if (--myStartCount == 0)
        myDelegate.visitFinished(context);
    }
  }
}
