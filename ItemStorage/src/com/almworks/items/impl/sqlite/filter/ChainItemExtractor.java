package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.sqlite.TransactionContext;

import java.util.List;

public class ChainItemExtractor extends CompositeExtractor {
  public ChainItemExtractor(List<ExtractionOperator> operators, boolean reuseList) {
    super(operators, reuseList);
  }

  public ExtractionFunction apply(TransactionContext context, ExtractionFunction input) {
    ExtractionFunction f = input;
    for (ExtractionOperator operator : getOperators()) {
      f = operator.apply(context, f);
    }
    return f;
  }
}
