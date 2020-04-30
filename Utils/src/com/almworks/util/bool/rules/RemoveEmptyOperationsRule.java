package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.ReductionRule;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This rule hierarchically removes all empty operations (operations without any arguments). If such operation
 * is itself an argument to another operation, then it is ignored. This may lead to another operation having no
 * children, so this rule always applies to the whole subtree.
 */
public class RemoveEmptyOperationsRule<P> extends ReductionRule<P> {
  private static final RemoveEmptyOperationsRule INSTANCE = new RemoveEmptyOperationsRule();

  public static <P> ReductionRule<P> INSTANCE() {
    return INSTANCE;
  }

  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    // first, look if there's an empty operation (an unlikely thing); if not, nothing to do
    if (!findEmptyOperation(expression))
      return false;

    // start with the top expression, look and remove empty operations recursively, children first
    boolean empty = processEmpty(expression, builder);
    if (empty) {
      // if all the expression is now empty, remove the operation and replace with TRUE (even if negated)
      builder.clearCurrent();
      builder.setExpression(BoolExpr.<P>TRUE());
    }
    return true;
  }

  /**
   * Looks for empty children expressions and removes them
   * @param expression source expression
   * @param builder target builder
   * @return true if expression was empty (was an operation with no children)
   */
  private boolean processEmpty(BoolExpr<P> expression, BoolExprBuilder<P> builder) {
    // if it is not an operation, not applicable
    BoolExpr.Operation<P> op = expression.asOperation();
    if (op == null) {
      builder.setExpression(expression);
      return false;
    }

    // build result, count added children
    builder.likeOperation(op);
    int childrenCount = 0;
    for (BoolExpr<P> arg : op.getArguments()) {
      builder.add();
      // instead adding child expression, recursive call to make an image without empty operations
      boolean empty = processEmpty(arg, builder);
      if (empty) {
        // if the result was empty operation, don't add it - remove empty builder node
        builder.remove();
      } else {
        builder.up();
        childrenCount++;
      }
    }
    return childrenCount == 0;
  }

  private boolean findEmptyOperation(BoolExpr<P> expression) {
    BoolExpr.Operation<P> op = expression.asOperation();
    if (op == null)
      return false;
    List<BoolExpr<P>> args = op.getArguments();
    if (args.isEmpty())
      return true;
    for (BoolExpr<P> arg : args) {
      if (findEmptyOperation(arg))
        return true;
    }
    return false;
  }
}
