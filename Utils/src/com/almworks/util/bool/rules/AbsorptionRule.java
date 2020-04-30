package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.ReductionRule;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Simplifies using absorption rule:
 * <pre>
 * a & (a | b) &lt;=&gt; a
 * a | (a & b) &lt;=&gt; a
 * </pre>
 * <p>
 * Uses <code>equals()</code> to check for expression equality
 */
public class AbsorptionRule<P> extends ReductionRule<P> {
  private static final AbsorptionRule INSTANCE = new AbsorptionRule();

  public static <P> ReductionRule<P> instance() {
    return (ReductionRule<P>) INSTANCE;
  }

  // todo maybe extract base class that implements "skipping", this would reduce the complexity
  @SuppressWarnings({"OverlyComplexMethod"})
  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    BoolExpr.Operation<P> op = expression.asOperation();
    if (op == null)
      return false;
    List<BoolExpr<P>> args = op.getArguments();
    if (args.size() < 2)
      return false;

    // this will be the set containing all of op's arguments, initialized only when needed
    Set<BoolExpr<P>> lookup = null;
    
    boolean changed = false;
    for (int i = 0; i < args.size(); i++) {
      BoolExpr<P> arg = args.get(i);
      boolean skip = false;
      BoolExpr.Operation<P> coop = arg.asOperation(op.getOperation().getComplementary());
      if (coop != null && !coop.isNegated() && coop.getArguments().size() >= 2) {
        if (lookup == null)
          lookup = Collections15.hashSet(args);
        // see if any of the op's arguments are equal to any of the coop's argument
        // if yes, the whole coop may be ignored
        for (BoolExpr<P> coArg : coop.getArguments()) {
          if (lookup.contains(coArg)) {
            skip = true;
            break;
          }
        }
      }
      // don't build until we hit first skipped arg, then catch up
      if (skip) {
        if (!changed) {
          builder.likeOperation(op);
          builder.addAll(args.subList(0, i));
          changed = true;
        }
      } else if (changed) {
        builder.add(arg);
      }
    }
    
    return changed;
  }
}
