package com.almworks.util.bool;

import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function2;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.almworks.util.bool.BoolOperation.AND;
import static com.almworks.util.bool.BoolOperation.OR;
import static org.almworks.util.Collections15.arrayList;

/**
 * BooleanExpression is used to specify a combination of predicates P grouped by logical operations. Main use of this
 * class is to abstract the composition of logical expression and to enable generic boolean reductions.
 * <p/>
 * BooleanExpression is an abstract class, with all necessary subclasses provided as inner classes. You can create
 * your own extension of BooleanExpression (though it's unlikely you really need to).
 * <p/>
 * An instance of BooleanExpression is immutable and thread-safe. Predicates P are also required to be immutable.
 * <p/>
 * BooleanExpression overrides equals() and hashCode(), so you can cache them. Composite equals is based on the equals()
 * method of P class. Hash code is cached for speed. In composite boolean expression, equality is not dependent on the
 * order of children (inherently commutative).
 * <p/>
 * To build a BooleanExpression, you can use static methods, provided by this class, or use {@link BoolExprBuilder}.
 *
 * @param <P> predicate type, must be immutable
 * @see ReductionRule
 * @see BoolExprBuilder
 */
@SuppressWarnings({"ClassReferencesSubclass"})
public abstract class BoolExpr<P> {
  /**
   * If true, the boolean expression defined by the subclass is inverted. This is used instead of
   * defining NOT operation.
   */
  private final boolean myNegated;

  /**
   * Cached hash code, or 0 if not initialized.
   */
  private volatile int myHashCode;

  protected BoolExpr(boolean negated) {
    myNegated = negated;
  }

  /**
   * This method should construct the same expression with inverted "negated" flag.
   * <p/>
   * Invariant: <pre>a.equalsWithoutNegated(a.negate()) && !a.equals(a.negate())</pre>
   *
   * @return negated this
   */
  public abstract BoolExpr<P> negate();

  /**
   * The implementation should implement basic equals() logic, ignoring "negated" flag. This method
   * does not implement any reductions, only checks state.
   *
   * @param expr the other object
   * @return true iff the other object is the same as this, but maybe negated
   */
  protected abstract boolean equalsWithoutNegated(BoolExpr<?> expr);

  /**
   * This method should create hash code based on the subclass state. Ignore "negated" flag. Do not implement
   * caching - it's done in the hashCode().
   *
   * @return hash code of the subclass state, compatible with {@link #equalsWithoutNegated}.
   */
  protected abstract int hashCodeWithoutNegated();

  /**
   * Write state to string builder, to be used in toString(). It's better be compatible with BooleanStringParser
   * used in tests. Don't forget to inspect "negated" flag.
   */
  protected abstract void toString(StringBuilder builder);

  /**
   * Evaluate expression, using given evaluator for terminal predicates.
   *
   * @param termEvaluator a condition that returns true if P evaluates to true; false otherwise
   * @return the result of evaluation
   */
  public abstract boolean eval(Condition<P> termEvaluator);

  public abstract <T> T foldTerms(T seed, Function2<P, T, T> f);

  /**
   * Returns true if this expression is negated.
   */
  public final boolean isNegated() {
    return myNegated;
  }

  /**
   * Converts (casts) this expression to an instance of {@link Operation}. If not covertible, returns
   * null.
   * <p/>
   * Optionally, you can request to restrict the returned operation to be only AND or OR by using op parameter. If
   * this is an operation of a type different from op, null is returned.
   *
   * @param op limit the returned operation to be exactly of this type; if null, no limits
   * @return an operation interface
   */
  @Nullable
  public Operation<P> asOperation(@Nullable BoolOperation op) {
    return null;
  }

  /**
   * Shortcut for asOperation(null).
   *
   * @see #asOperation(BoolOperation)
   */
  @Nullable
  public final Operation<P> asOperation() {
    return asOperation(null);
  }

  /**
   * Converts (casts) this expression to an instance of {@link Term}. If not convertible, returns
   * null.
   *
   * @return a term interface
   */
  @Nullable
  public Term<P> asTerm() {
    return null;
  }

  /**
   * Returns terminal predicate, if this is an instance of Term, otherwise returns null.
   */
  @Nullable
  public P getTerm() {
    return null;
  }

  /**
   * Returns true if expr is a negated version of this. Does not perform any reductions! Only checks using
   * equalsWithoutNegated.
   */
  public final boolean equalsNegated(BoolExpr<P> expr) {
    if (expr == null || expr == this || myNegated == expr.isNegated())
      return false;
    return equalsWithoutNegated(expr);
  }

  /**
   * Template method, transferring call to {@link #toString(StringBuilder)}
   */
  @Override
  public final String toString() {
    StringBuilder builder = new StringBuilder();
    toString(builder);
    return builder.toString();
  }

  /**
   * Template method that checks the equality. Uses hash code for speed.
   */
  @Override
  public final boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != getClass())
      return false;
    BoolExpr<?> that = (BoolExpr<?>) obj;
    if (that.isNegated() != myNegated)
      return false;
    if (hashCode() != that.hashCode())
      return false;
    return equalsWithoutNegated(that);
  }

  /**
   * Template method for hash code, also performs caching.
   */
  @SuppressWarnings({"NonFinalFieldReferencedInHashCode", "ReuseOfLocalVariable"})
  @Override
  public final int hashCode() {
    int h = myHashCode;
    if (h != 0)
      return h;
    h = hashCodeWithoutNegated();
    if (myNegated)
      h ^= 0xAAAAAAAA;
    if (h == 0)
      h = -1;
    myHashCode = h;
    return h;
  }

  /* ********************************************************************************************** */
  // method-chain utilities
  public final BoolExpr<P> and(@NotNull BoolExpr<P> that) {
    return combineOperations(AND, this, that);
  }

  public final BoolExpr<P> or(@NotNull BoolExpr<P> that) {
    return combineOperations(OR, this, that);
  }

  private static <P> BoolExpr<P> combineOperations(BoolOperation operation, BoolExpr<P> exp1, BoolExpr<P> exp2) {
    List<BoolExpr<P>> arguments = Collections15.arrayList();
    combineArguments(operation, exp1, arguments);
    combineArguments(operation, exp2, arguments);
    return operation(operation, arguments, false, true);
  }

  private static <P> void combineArguments(BoolOperation operation, BoolExpr<P> expr, List<BoolExpr<P>> arguments) {
    if (!expr.isNegated()) {
      Operation<P> op = expr.asOperation(operation);
      if (op != null) {
        arguments.addAll(op.getArguments());
        return;
      }
    }
    arguments.add(expr);
  }

  /* ********************************************************************************************** */
  // static utilities

  /**
   * Creates singular expression based on terminal predicate.
   *
   * @param predicate terminal predicate, must be immutable
   * @param negated   if true, the expression is negated
   * @param <P>       predicate type
   * @return boolean expression consisting only of predicate
   */
  public static <P> Term<P> term(@NotNull P predicate, boolean negated) {
    return new Term<P>(predicate, negated);
  }

  /**
   * Creates singular expression based on terminal predicate.
   *
   * @param predicate terminal predicate, must be immutable
   * @param <P>       predicate type
   * @return boolean expression consisting only of predicate
   */
  public static <P> Term<P> term(@NotNull P predicate) {
    return term(predicate, false);
  }

  /**
   * Convenience method for creating a list of Term expressions from predicates
   */
  public static <P> List<Term<P>> termList(P... predicates) {
    if (predicates == null || predicates.length == 0)
      return Collections15.emptyList();
    List<Term<P>> r = Collections15.arrayList(predicates.length);
    for (P predicate : predicates) {
      if (predicate != null) {
        r.add(term(predicate));
      }
    }
    return r;
  }

  /**
   * Constructs composite boolean expression based on other expressions and grouping operation (AND or OR).
   *
   * @param operation    AND or OR
   * @param arguments    other expressions, must be not null
   * @param negated      if true, the resulting expression is negated
   * @param canReuseList if false, arguments list will be copied; if true, it will be used in the state of the expression. Set to false when in doubt.
   * @param <P>          predicate type
   * @return boolean expression that groups all the argument with the specified operation
   */
  public static <P> Operation<P> operation(BoolOperation operation,
    List<? extends BoolExpr<P>> arguments, boolean negated, boolean canReuseList)
  {
    List<BoolExpr<P>> list =
      canReuseList ? Collections.unmodifiableList(arguments) : Collections15.unmodifiableListCopy(arguments);
    return new Operation<P>(operation, list, negated);
  }


  /**
   * Convenience method for grouping terminal predicates with AND
   */
  public static <P> Operation<P> andTerms(P ... terms) {
    return operation(AND, termList(terms), false, true);
  }

  /**
   * Convenience method for grouping terminal predicates with OR
   */
  public static <P> Operation<P> orTerms(P ... terms) {
    return operation(OR, termList(terms), false, true);
  }

  /**
   * Convenience method for grouping expressions with AND
   */
  @SuppressWarnings({"StaticMethodNamingConvention"})
  public static <P> BoolExpr<P> and(BoolExpr<P>... expressions) {
    return operation(AND, Arrays.asList(expressions), false, true);
  }
  
  public static <P> Operation<P> and(Collection<? extends BoolExpr<P>> terms) {
    return operation(AND, Collections15.arrayList(terms), false, true);
  }

  /**
   * Convenience method for grouping expressions with OR
   */
  @SuppressWarnings({"StaticMethodNamingConvention"})
  public static <P> Operation<P> or(BoolExpr<P>... expressions) {
    return operation(OR, Arrays.asList(expressions), false, true);
  }

  public static <P> Operation<P> or(Collection<? extends BoolExpr<P>> terms) {
    return operation(OR, Collections15.arrayList(terms), false, true);
  }

  /**
   * Returns TRUE as an expression based on P predicate type.
   *
   * @param <P> predicate type
   * @return boolean expression identical to TRUE
   */
  @SuppressWarnings({"StaticMethodNamingConvention"})
  public static <P> Literal<P> TRUE() {
    return (Literal<P>) Literal.TRUE;
  }

  /**
   * Returns FALSE as an expression based on P predicate type.
   *
   * @param <P> predicate type
   * @return boolean expression identical to FALSE
   */
  @SuppressWarnings({"StaticMethodNamingConvention"})
  public static <P> Literal<P> FALSE() {
    return (Literal<P>) Literal.FALSE;
  }

  /**
   * Converts boolean value to constant expression TRUE or FALSE
   */
  public static <P> Literal<P> literal(boolean value) {
    return value ? Literal.TRUE : Literal.FALSE;
  }

  /**
   * Returns a condition over P-based boolean expressions that selects only operations, possibly of a specified type.
   *
   * @param operation if not null, condition will accept only operations of a specified type; otherwise, any operation will be accepted
   * @param <P>       predicate type
   * @return a condition selecting operations
   */
  @SuppressWarnings({"StaticMethodNamingConvention"})
  public static <P> Condition<BoolExpr<P>> IS_OPERATION(@Nullable BoolOperation operation) {
    if (operation == null)
      return (Condition<BoolExpr<P>>) CheckCondition.IS_OPERATION;
    else if (operation == AND)
      return (Condition<BoolExpr<P>>) CheckCondition.IS_AND;
    else if (operation == OR)
      return (Condition<BoolExpr<P>>) CheckCondition.IS_OR;
    assert false : operation;
    return Condition.never();
  }

  /**
   * Returns a condition over P-based boolean expressions that selects only terminal expressions.
   *
   * @param <P> predicate type
   * @return a condition selecting terms
   */
  @SuppressWarnings({"StaticMethodNamingConvention"})
  public static <P> Condition<BoolExpr<P>> IS_TERM() {
    return (Condition<BoolExpr<P>>) CheckCondition.IS_TERM;
  }

  /* ********************************************************************************************** */


  /**
   * An expression that groups other expressions with a boolean operation AND or OR.
   *
   * @param <P> predicate type
   */
  public static class Operation<P> extends BoolExpr<P> {
    /**
     * Unmodifiable, reusable list.
     */
    @NotNull
    private final List<BoolExpr<P>> myArguments;

    /**
     * AND or OR
     */
    @NotNull
    private final BoolOperation myOperation;

    private Operation(BoolOperation operation, List<BoolExpr<P>> arguments, boolean negated) {
      super(negated);
      myOperation = operation;
      myArguments = arguments;
    }

    public Operation<P> negate() {
      return new Operation<P>(myOperation, myArguments, !isNegated());
    }

    @NotNull
    @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
    public List<BoolExpr<P>> getArguments() {
      return myArguments;
    }

    @NotNull
    public BoolOperation getOperation() {
      return myOperation;
    }

    public boolean eval(Condition<P> termEvaluator) {
      if (myArguments.isEmpty()) {
        // run Reductions.simplify() before evaluating if you get this assertion
//        assert false : "cannot evaluate no-operand operator, simplify first";
        Log.warn("cannot evaluate " + this);
        return true;
      }
      boolean result = myArguments.get(0).eval(termEvaluator);
      boolean stableResult = myOperation != AND; // true if OR, false if AND
      for (int i = 1; result != stableResult && i < myArguments.size(); i++) {
        boolean r = myArguments.get(i).eval(termEvaluator);
        result = myOperation == AND ? result && r : result || r;
      }
      return result ^ isNegated();
    }

    @Override
    public <T> T foldTerms(T seed, Function2<P, T, T> f) {
      for (BoolExpr<P> argument : myArguments) {
        seed = argument.foldTerms(seed, f);
      }
      return seed;
    }


    @SuppressWarnings({"RefusedBequest"})
    @Nullable
    @Override
    public Operation<P> asOperation(BoolOperation op) {
      return op == null || op == myOperation ? this : null;
    }

    protected boolean equalsWithoutNegated(BoolExpr<?> expr) {
      if (!(expr instanceof Operation))
        return false;
      Operation<?> that = (Operation<?>) expr;
      if (that.getOperation() != myOperation)
        return false;
      // todo double-check both arrays vs. additional allocation
      List<? extends BoolExpr<?>> thatArgs = that.getArguments();
      if (myArguments.size() != thatArgs.size())
        return false;
      for (BoolExpr<?> arg : thatArgs) {
        //noinspection SuspiciousMethodCalls
        if (!myArguments.contains(arg)) {
          return false;
        }
      }
      for (BoolExpr<P> arg : myArguments) {
        //noinspection SuspiciousMethodCalls
        if (!thatArgs.contains(arg)) {
          return false;
        }
      }
      return true;
    }

    @SuppressWarnings({"MagicNumber"})
    protected int hashCodeWithoutNegated() {
      int hash = myOperation.hashCode();
      List<BoolExpr<P>> sortedChildren = arrayList(myArguments);
      Collections.sort(sortedChildren, Containers.hashCodeComparator());
      return hash * 10007 + sortedChildren.hashCode();
    }

    protected void toString(StringBuilder builder) {
      String c = myOperation == AND ? " & " : " | ";
      if (isNegated())
        builder.append('!');
      builder.append('(');
      int count = myArguments.size();
      if (count < 2)
        builder.append(c);
      boolean first = true;
      for (BoolExpr<P> argument : myArguments) {
        if (first) {
          first = false;
        } else {
          builder.append(c);
        }
        argument.toString(builder);
      }
      builder.append(')');
    }
  }


  /**
   * Wraps predicate P as a terminal expression
   *
   * @param <P> predicate type
   */
  public static class Term<P> extends BoolExpr<P> {
    private static final int MAX_TO_STRING = 100;

    @NotNull
    private final P myTerm;

    private Term(@NotNull P term, boolean negated) {
      super(negated);
      myTerm = term;
    }

    public BoolExpr<P> negate() {
      return new Term<P>(myTerm, !isNegated());
    }

    @NotNull
    public P getTerm() {
      return myTerm;
    }

    @Override
    public Term<P> asTerm() {
      return this;
    }

    protected boolean equalsWithoutNegated(BoolExpr<?> expr) {
      if (!(expr instanceof Term))
        return false;
      return Util.equals(myTerm, ((Term) expr).myTerm);
    }

    protected int hashCodeWithoutNegated() {
      return myTerm == null ? 0 : myTerm.hashCode();
    }

    protected void toString(StringBuilder builder) {
      if (isNegated())
        builder.append('!');
      String s = String.valueOf(myTerm);
      if (s.length() > MAX_TO_STRING) {
        builder.append(s.substring(0, MAX_TO_STRING - 3)).append("...");
      } else {
        builder.append(s);
      }
    }

    public boolean eval(Condition<P> termEvaluator) {
      return termEvaluator.isAccepted(myTerm) ^ isNegated();
    }

    @Override
    public <T> T foldTerms(T seed, Function2<P, T, T> f) {
      return f.invoke(myTerm, seed);
    }
  }


  /**
   * Represents identical TRUE or FALSE expression.
   *
   * @param <P> predicate type, doesn't make much sense though
   */
  public static class Literal<P> extends BoolExpr<P> {
    private static final Literal TRUE = new Literal(false);
    private static final Literal FALSE = new Literal(true);

    private Literal(boolean negated) {
      super(negated);
    }

    public Literal<P> negate() {
      assert this == TRUE || this == FALSE : this;
      return this == TRUE ? FALSE : TRUE;
    }

    protected boolean equalsWithoutNegated(BoolExpr<?> expr) {
      return expr instanceof Literal;
    }

    @SuppressWarnings({"MagicNumber"})
    protected int hashCodeWithoutNegated() {
      return 0x87654321;
    }


    protected void toString(StringBuilder builder) {
      builder.append(isNegated() ? "0" : "1");
    }

    public boolean eval(Condition<P> termEvaluator) {
      return !isNegated();
    }

    @Override
    public <T> T foldTerms(T seed, Function2<P, T, T> f) {
      return seed;
    }
  }


  private static class CheckCondition extends Condition<BoolExpr> {
    private static final Condition IS_TERM = new CheckCondition(true, null);
    private static final Condition IS_OPERATION = new CheckCondition(false, null);
    private static final Condition IS_AND = new CheckCondition(false, AND);
    private static final Condition IS_OR = new CheckCondition(false, OR);

    private final boolean myTerm;
    private final BoolOperation myOperation;

    private CheckCondition(boolean term, BoolOperation operation) {
      myTerm = term;
      myOperation = operation;
    }

    @Override
    public boolean isAccepted(BoolExpr value) {
      if (value == null)
        return false;
      return myTerm ? value.asTerm() != null : value.asOperation(myOperation) != null;
    }
  }
}
