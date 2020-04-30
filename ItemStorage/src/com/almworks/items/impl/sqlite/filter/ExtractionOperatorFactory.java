package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.api.DP;
import com.almworks.items.impl.sqlite.TransactionContext;
import org.jetbrains.annotations.Nullable;

public interface ExtractionOperatorFactory {
  /**
   * Creates an operator equivalent to the given filter.
   * @return null if conversion is not possible with this convertor (try another)
   */
  @Nullable
  ExtractionOperator convert(DP predicate, boolean negated, TransactionContext transactionContext);
}
