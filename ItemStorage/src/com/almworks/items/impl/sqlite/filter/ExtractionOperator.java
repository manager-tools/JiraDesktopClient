package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.util.collections.Containers;

import java.util.Comparator;

import static com.almworks.items.impl.sqlite.filter.SingularExtractFunction.EXTRACT_NONE;

public abstract class ExtractionOperator {
  public static final ExtractionOperator NONE = new ConstItemExtractor(EXTRACT_NONE);
  public static final ExtractionOperator ALL = new IdenticalItemExtractor();
  
  public static final Comparator<ExtractionOperator> PERFORMANCE_COMPARATOR = new Comparator<ExtractionOperator>() {
    @Override
    public int compare(ExtractionOperator o1, ExtractionOperator o2) {
      return Containers.compareInts(o1.getPerformanceHit(), o2.getPerformanceHit());
    }
  };

  public abstract ExtractionFunction apply(TransactionContext context, ExtractionFunction input);

  public int getPerformanceHit() {
    return 0;
  }
}
