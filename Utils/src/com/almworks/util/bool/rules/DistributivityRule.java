package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.BoolOperation;
import com.almworks.util.bool.ReductionRule;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.almworks.util.bool.BoolOperation.AND;
import static com.almworks.util.bool.BoolOperation.OR;

/**
 * Uses distributivity properties of conjunction and disjunction to swap top-level conjunction with
 * nested disjunction and vice versa:
 * <pre>
 * instance(AND):
 *   A & (B | C) =&gt; (A & B) | (A & C)
 * instance(OR):
 *   A | (B & C) =&gt; (A | B) & (A | C)
 * </pre>
 * <p/>
 * <b>NB:</b> Be careful not to place both of these rules in a rule chain, or you will end up with an infinite reduction cycle.
 */
public class DistributivityRule<P> extends ReductionRule<P> {
  private static final DistributivityRule INSTANCE_AND = new DistributivityRule(AND);
  private static final DistributivityRule INSTANCE_OR = new DistributivityRule(OR);

  /**
   * Returns the rule instance
   *
   * @param operation top-level operation to be converted and moved to the second level
   */
  public static <P> DistributivityRule<P> instance(BoolOperation operation) {
    assert operation == AND || operation == OR;
    return operation == AND ? INSTANCE_AND : INSTANCE_OR;
  }

  /**
   * Top-level operation. After reduction, its complementary will be top level.
   */
  private final BoolOperation myOperation;

  public DistributivityRule(@NotNull BoolOperation operation) {
    myOperation = operation;
  }

  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    BoolExpr.Operation<P> op = expression.asOperation(myOperation);
    if (op == null)
      return false;
    if (op.getArguments().size() < 2)
      return false;

    BoolExpr.Operation<P> coop = findCooperation(op);
    if (coop == null)
      return false;

    // make a list of all op's children except coop
    List<BoolExpr<P>> opChildren = Collections15.arrayList(op.getArguments());
    opChildren.remove(coop);

    // apply this rule to children first
    opChildren = applyAll(opChildren);

    // apply this rule to coop's children as well
    List<BoolExpr<P>> coopChildren = applyAll(coop.getArguments());

    // do the job
    builder.setOperation(op.getOperation().getComplementary(), op.isNegated());
    for (BoolExpr<P> c2 : coopChildren) {
      builder.add().setOperation(myOperation, false).add(c2).addAll(opChildren).up();
    }
    return true;
  }

  /**
   * Among operation's arguments, finds a co-operation (complementary operation) that
   * can be used in the rule (not negated, with at least 2 children).
   */
  @Nullable
  private BoolExpr.Operation<P> findCooperation(BoolExpr.Operation<P> op) {
    BoolOperation cooperation = op.getOperation().getComplementary();
    for (BoolExpr<P> arg : op.getArguments()) {
      BoolExpr.Operation<P> t = arg.asOperation(cooperation);
      if (t != null && !t.isNegated() && t.getArguments().size() >= 2) {
        return t;
      }
    }
    return null;
  }
}