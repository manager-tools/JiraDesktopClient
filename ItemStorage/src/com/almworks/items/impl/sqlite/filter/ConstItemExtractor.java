package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.sqlite.TransactionContext;

public class ConstItemExtractor extends ExtractionOperator {
  private final ExtractionFunction myResult;

  public ConstItemExtractor(ExtractionFunction result) {
    myResult = result;
  }

  public ExtractionFunction apply(TransactionContext context, ExtractionFunction input) {
    return myResult;
  }

  @Override
  public int getPerformanceHit() {
    // ignores input
    return -100;
  }
}
