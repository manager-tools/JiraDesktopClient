package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.BoolOperation;
import com.almworks.util.bool.ReductionRule;
import org.jetbrains.annotations.NotNull;

/**
 * Flattens nested operations of similar type:
 * <pre>
 * A & (B & C) &lt;=&gt; A & B & C
 * </pre>
 */
public class FlattenRule<P> extends ReductionRule<P> {
  private static final FlattenRule INSTANCE = new FlattenRule();

  public static <P> ReductionRule<P> instance() {
    return INSTANCE;
  }

  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    BoolExpr.Operation<P> op = expression.asOperation();
    if (op == null)
      return false;

    if (!findNestedOperation(op))
      return false;

    builder.likeOperation(op);
    BoolOperation operation = op.getOperation();
    for (BoolExpr<P> arg : op.getArguments()) {
      BoolExpr.Operation<P> child = arg.asOperation(operation);
      // if flattening, add all nested's arguments as our own
      if (canBeFlattened(child)) {
        assert child != null;
        builder.addAll(child.getArguments());
      } else {
        builder.add(arg);
      }
    }
    return true;
  }

  /**
   * Looks if there's a candidate for flattening
   */
  private boolean findNestedOperation(BoolExpr.Operation<P> op) {
    BoolOperation operation = op.getOperation();
    for (BoolExpr<P> arg : op.getArguments()) {
      BoolExpr.Operation<P> child = arg.asOperation(operation);
      if (canBeFlattened(child)) {
        return true;
      }
    }
    return false;
  }

  private boolean canBeFlattened(BoolExpr.Operation<P> child) {
    return child != null && !child.isNegated();
  }
}