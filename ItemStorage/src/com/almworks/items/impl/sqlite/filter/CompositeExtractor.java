package com.almworks.items.impl.sqlite.filter;

import org.almworks.util.Collections15;
import org.almworks.util.StringUtil;

import java.util.Collections;
import java.util.List;

public abstract class CompositeExtractor extends ExtractionOperator {
  private final List<ExtractionOperator> myOperators;

  protected CompositeExtractor(List<ExtractionOperator> operators, boolean reuseList) {
    myOperators = Collections.unmodifiableList(reuseList ? operators : Collections15.arrayList(operators));
  }

  @Override
  public int getPerformanceHit() {
    int r = -100;
    for (ExtractionOperator operator : myOperators) {
      r = Math.max(r, operator.getPerformanceHit());
    }
    r += myOperators.size() - 1;
    return r;
  }

  public List<ExtractionOperator> getOperators() {
    return myOperators;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(StringUtil.substringAfterLast(getClass().getName(), "."));
    b.append("(");
    for (ExtractionOperator operator : myOperators) {
      b.append(String.valueOf(operator));
    }
    b.append(")");
    return b.toString();
  }
}
