package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.api.DP;
import com.almworks.items.impl.sqlite.TransactionContext;

public abstract class TypedExtractionFactory<P extends DP> implements ExtractionOperatorFactory {
  private final Class<P> myPredicateClass;

  protected TypedExtractionFactory(Class<P> predicateClass) {
    myPredicateClass = predicateClass;
  }

  @Override
  public ExtractionOperator convert(DP predicate, boolean negated, TransactionContext transactionContext) {
    if (predicate == null || !predicate.getClass().equals(myPredicateClass))
      return null;
    return convertTyped((P) predicate, negated, transactionContext);
  }

  protected abstract ExtractionOperator convertTyped(P predicate, boolean negated, TransactionContext trContext);
}
