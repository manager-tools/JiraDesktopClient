package com.almworks.util.bool;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reduction rules are used to create a reduced modification of a {@link BoolExpr}. In general, the
 * result of the reduction must be logically the same as the original expression. This is not enforced
 * in ReductionRule - theoretically, you can use reductions to get a completely different expression.
 * However, for the sake of clarity, use ReductionRule only to build an equal replacement for the original
 * expression, and for all other reasons use {@link ConversionRule}.
 * <p>
 * The main method is {@link #reduce}, which applies the rule until nothing is changed.
 * <p>
 * Reductions are <b>slow</b>, and may take exponential time.
 * <p>
 * It is very easy to write or combine rules that will make reduce() method never finish. Be careful. 
 *
 * @param <P> predicate type
 * @see BoolExpr
 * @see ConversionRule
 */
public abstract class ReductionRule<P> {
  /**
   * Applies rule to expression, if possible.
   * <p/>
   * If rule is not applicable, returns false. Otherwise, uses builder to construct a replacement expression.
   *
   * @param expression source expression
   * @param builder    empty builder for receiving result expression
   * @return true if rule has been applied
   */
  public abstract boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder);

  /**
   * Applies reduction rule to the expression recursively and continuously until no changes are made.
   *
   * @param element source expression
   * @return result of reduction
   */
  @NotNull
  public final BoolExpr<P> reduce(@NotNull BoolExpr<P> element) {
    BoolExprBuilder<P> builder = BoolExprBuilder.create();
    BoolExpr<P> result = element;
    boolean applied;
    do {
      builder.clearAll();
      applied = applyRecursive(result, builder);
      if (applied) {
        BoolExpr<P> newResult = builder.build();
        assert !newResult.equals(result) : this + " cycles " + result;
        result = newResult;
      }
    } while (applied);
    return result;
  }

  /**
   * Apply rule to the expression recursively. If rule does not apply to the top node, go down through the
   * composite and try to apply to children.
   *
   * @param source source expression
   * @param builder target builder
   * @return true if rule applied at least once
   */
  public boolean applyRecursive(@NotNull BoolExpr<P> source, @NotNull BoolExprBuilder<P> builder) {
    boolean r = apply(source, builder);
    if (r)
      return true;
    BoolExpr.Operation<P> op = source.asOperation();
    if (op == null)
      return false;
    // construct a copy of the operation node, apply to children 
    builder.likeOperation(op);
    boolean changed = false;
    for (BoolExpr<P> arg : op.getArguments()) {
      builder.add();
      r = applyRecursive(arg, builder);
      if (r) {
        changed = true;
      } else {
        builder.clearCurrent();
        builder.setExpression(arg);
      }
      builder.up();
    }
    if (!changed) {
      builder.clearCurrent();
    }
    return changed;
  }

  /**
   * Convenience method, applies the rule once to the list of expressions and returns the list of results.
   */
  public final List<BoolExpr<P>> applyAll(List<BoolExpr<P>> expressions) {
    BoolExprBuilder<P> builder = BoolExprBuilder.create();
    List<BoolExpr<P>> result = Collections15.arrayList(expressions.size());
    for (BoolExpr<P> expression : expressions) {
      builder.clearAll();
      boolean r = apply(expression, builder);
      result.add(r ? builder.build() : expression);
    }
    return result;
  }
}