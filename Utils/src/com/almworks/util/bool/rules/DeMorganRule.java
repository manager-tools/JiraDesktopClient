package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.ReductionRule;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Moves negation towards leaves of the expression tree by implementing DeMorgan rules:
 * <pre>
 * !(A & B) &lt;=&gt; (!A) | (!B)
 * !(A | B) &lt;=&gt; (!A) & (!B)
 * </pre>
 */
public class DeMorganRule<P> extends ReductionRule<P> {
  private static final DeMorganRule INSTANCE = new DeMorganRule();

  public static <P> ReductionRule<P> instance() {
    return INSTANCE;
  }

  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    BoolExpr.Operation<P> op = expression.asOperation();
    // applies only to negated operations
    if (op == null || !op.isNegated())
      return false;
    List<BoolExpr<P>> args = op.getArguments();
    if (args.isEmpty())
      return false;
    // create operation of complementary type and negate all children
    builder.setOperation(op.getOperation().getComplementary(), false);
    for (BoolExpr<P> arg : args) {
      builder.add().setExpression(arg).negate().up();
    }
    return true;
  }
}