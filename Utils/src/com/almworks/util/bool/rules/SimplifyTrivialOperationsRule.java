package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.BoolOperation;
import com.almworks.util.bool.ReductionRule;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.almworks.util.bool.BoolOperation.AND;
import static com.almworks.util.bool.BoolOperation.OR;

/**
 * Simplifies a number of singular and boundary conditions:
 * <ul>
 * <li>Replaces single-child operations with the child itself</li>
 * <li>A & A =&gt; A; A | A =&gt; A</li>
 * <li>A & !A =&gt; FALSE; A | !A =&gt; TRUE</li>
 * <li>A & TRUE =&gt; A; A & FALSE =&gt; FALSE</li>
 * <li>A | TRUE =&gt; TRUE; A | FALSE =&gt; A</li>
 * </ul>
 */
public class SimplifyTrivialOperationsRule<P> extends ReductionRule<P> {
  private static final SimplifyTrivialOperationsRule INSTANCE = new SimplifyTrivialOperationsRule();

  public static <P> ReductionRule<P> instance() {
    return INSTANCE;
  }

  // todo refactor method to make it simpler; the only way is to introduce execution object (too many local vars)
  @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    BoolExpr.Operation<P> op = expression.asOperation();
    if (op == null)
      return false;
    List<BoolExpr<P>> args = op.getArguments();
    int argsCount = args.size();

    // will hold valid children, if any changes are detected
    Set<BoolExpr<P>> children = null;

    // will be set to true if any changes take place
    boolean changed = false;

    // will be set to non-null if expression is identical to TRUE or FALSE before negation (will hold the value)
    Boolean trivial = null;

    // will be set to the value of the last encountered literal child (needed in case (TRUE | TRUE))
    Boolean lastLiteral = null;

    // for each child, see if we need to do anything
    for (int i = 0; i < argsCount; i++) {
      BoolExpr<P> arg = args.get(i);

      // if true, don't add this child
      boolean skip = false;

      if (arg instanceof BoolExpr.Literal) {
        // check for trivial result
        trivial = checkLiteral(arg, op.getOperation());

        // remember literal value
        lastLiteral = arg == BoolExpr.TRUE();

        // don't add in any case
        skip = true;
      } else {
        // look for A & !A, A | !A
        trivial = findSameNegated(arg, children, op.getOperation());

        if (trivial == null) {
          // build children set
          if (children == null) {
            children = Collections15.hashSet();
          }
          if (!children.add(arg)) {
            // not added => already have this child, skip
            skip = true;
          }
        }
      }
      if (trivial != null) {
        // if we already have trivial result, ignore the rest of the arguments
        break;
      }
      if (skip) {
        // once something is skipped, we start using builder
        if (!changed) {
          changed = true;
          builder.likeOperation(op);
          builder.addAll(args.subList(0, i));
        }
      } else if (changed) {
        builder.add(arg);
      }
    }

    // finalize

    // not trivial, no children, but have last literal, example: "1 | 1"
    if (trivial == null && (children == null || children.isEmpty()) && lastLiteral != null) {
      trivial = lastLiteral;
    }

    if (trivial != null) {
      // trivial result - don't forget negation
      builder.clearCurrent();
      builder.setExpression(trivial ^ op.isNegated() ? BoolExpr.<P>TRUE() : BoolExpr.<P>FALSE());
    } else if (children != null && children.size() == 1) {
      // single child - replace with it
      changed = true;
      builder.clearCurrent();
      builder.setExpression(children.iterator().next()).setNegated(op.isNegated());
    }
    
    return changed || trivial != null;
  }

  /**
   * Looks for already processed children that are negated versions of arg. If found, calculate the result
   * (either TRUE or FALSE), based on the used boolean operation.
   *
   * @param arg expression to look for
   * @param previous a set of previously processed children
   * @param operation aggregating operation
   * @return null if not found, trivial result if found
   */
  @Nullable
  private Boolean findSameNegated(BoolExpr<P> arg, @Nullable Set<BoolExpr<P>> previous, BoolOperation operation) {
    if (previous != null) {
      for (BoolExpr<P> prev : previous) {
        if (arg.equalsNegated(prev)) {
          // can be simplified to operation != AND, but this is more readable
          return operation == AND ? Boolean.FALSE : Boolean.TRUE;
        }
      }
    }
    return null;
  }

  /**
   * Checks if literal argument (TRUE or FALSE) places as an argument to the specified operation
   * yields constant TRUE or FALSE.
   */
  @Nullable
  private Boolean checkLiteral(BoolExpr<P> arg, BoolOperation operation) {
    if (arg == BoolExpr.TRUE() && operation == OR) {
      // A | TRUE <=> TRUE
      return Boolean.TRUE;
    } else if (arg == BoolExpr.FALSE() && operation == AND) {
      // A & FALSE <=> FALSE
      return Boolean.FALSE;
    } else {
      // unknown
      return null;
    }
  }
}
