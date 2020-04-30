package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.sqlite.TransactionContext;

public class IdenticalItemExtractor extends ExtractionOperator {
  public IdenticalItemExtractor() {
  }

  public ExtractionFunction apply(TransactionContext context, ExtractionFunction input) {
    return input;
  }
}
