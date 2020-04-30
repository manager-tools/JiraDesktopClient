package com.almworks.util.bool;

/**
 * Basic boolean operations - AND and OR. Boolean negation is implemented via "negated" flag in BoolExpr.
 * Other operations can be expressed via basic operations.
 */
public enum BoolOperation {
  AND, OR;

  public BoolOperation getComplementary() {
    return this == AND ? OR : AND;
  }
}
