package com.almworks.items.api;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static org.almworks.util.Collections15.arrayList;

/**
 * DP stands for Database Predicate.
 * <p>
 * Several things to take care of when creating a new subclass of DP:
 * <ul>
 * <li>Make sure equals() and hashCode() provide instance-agnostic identity. When comparing values (T), which can
 * be <code>byte[]</code>, use {@link com.almworks.items.util.DatabaseUtil#valueEquals} and {@link com.almworks.items.util.DatabaseUtil#valueHash}.
 * <li>If the constraint is expressable in SQL, make sure a {@link com.almworks.items.impl.sqlite.filter.ExtractionOperatorFactory}
 * for your DP is registered with {@link com.almworks.items.impl.DBConfiguration#registerStandardFilterConvertors()}.
 * <li>Override {@link #addAffectingAttributes(java.util.Collection)} to make DP cache-effective.
 * <li>Override toString() (use {@link com.almworks.items.util.DatabaseUtil#valueString})
 * </ul>
 */
public abstract class DP {
  /**
   * Checks whether the predicate is true for the item in the given transaction.
   */
  public abstract boolean accept(long item, DBReader reader);

  /**
   * Collects attributes that have effect on the predicate's accept() function. This function is used to check whether
   * a write affects the result of a filter.
   *
   * @return true <strong>if and only if</strong> the result of accept() is solely defined by the checked item's
   * values for a set of attributes, which are added to the target collection. 
   */
  public boolean addAffectingAttributes(Collection<? super DBAttribute> target) {
    return false;
  }

  /**
   * DP that depends on volatile factors resolves in the context of current transaction using "current" factor values.<br>
   * Resolution must be the same in the same transaction for {@link #equalDP(DP) equal} DPs for all calls of this method.
   * "Current factor values" means that these are the values of the factors at some point inside the transaction, and they do not change during it.
   * Implementation note: if several DPs depend on some common factor, its value should be fixed in the reader's transaction cache.<br>
   * If DP does not depend on volatile factors, resolution should be {@code null}.<br>
   * <br>
   * Subscription (if provided) should be notified when the relevant factors change.
   * @param reader current transaction
   * @param subscription optional subscription (specify null if not needed)
   * @return resolution in form of expression or null - if no conversion required
   */
  @Nullable
  public BoolExpr<DP> resolve(DBReader reader, @Nullable ResolutionSubscription subscription) {
    return null;
  }

  public final BoolExpr<DP> term() {
    return BoolExpr.term(this);
  }

  public static Evaluator evaluator(long item, DBReader reader) {
    return new Evaluator(item, reader);
  }

  public static Evaluator evaluator(DBReader reader) {
    return new Evaluator(0, reader);
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!getClass().equals(obj.getClass())) return false;
    return equalDP((DP)obj);
  }

  @Override
  public final int hashCode() {
    return hashCodeDP();
  }

  /**
   * DPs are equal when:<br>
   * 1. For any item within any transaction both DPs accept or reject the item.<br>
   * 2. Both DP's {@link #resolve(DBReader)} return equal DP within any transaction
   * @param other
   * @return
   */
  protected abstract boolean equalDP(DP other);

  protected abstract int hashCodeDP();

  /**
   * Utility method to recursively resolve existing BoolExpr&lt;DP&gt;.
   * */
  @NotNull
  public static BoolExpr<DP> resolve(@NotNull BoolExpr<DP> filter, final DBReader reader, @Nullable ResolutionSubscription subscription) {
    DP term = filter.getTerm();
    if (term != null) {
      BoolExpr<DP> resolution = term.resolve(reader, subscription);
      if (resolution != null && filter.isNegated()) resolution = resolution.negate();
      assert resolution != filter && !filter.equals(resolution) : "should return null if no resolution";
      return resolution == null || resolution == filter ? filter : resolve(resolution, reader, subscription);
    } else {
      BoolExpr.Operation<DP> op = filter.asOperation();
      if (op != null) {
        List<BoolExpr<DP>> resolvedArgs = arrayList();
        for (BoolExpr<DP> arg : op.getArguments()) {
          resolvedArgs.add(resolve(arg, reader, subscription));
        }
        return BoolExpr.operation(op.getOperation(), resolvedArgs, op.isNegated(), true);
      }
    }
    return filter;
  }

  public static class Evaluator extends Condition<DP> {
    private final DBReader myReader;
    private long myItem;

    public Evaluator(long item, DBReader reader) {
      myItem = item;
      myReader = reader;
    }

    @Override
    public boolean isAccepted(DP value) {
      return value.accept(myItem, myReader);
    }

    public Evaluator set(long item) {
      myItem = item;
      return this;
    }
  }

  public interface ResolutionSubscription extends ChangeListener {
    public Lifespan getLife();
  }
}
