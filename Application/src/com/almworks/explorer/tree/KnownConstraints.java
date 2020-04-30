package com.almworks.explorer.tree;

import com.almworks.api.constraint.*;
import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author dyoma
 */
public class KnownConstraints {
  private static final Map<TypedKey, Condition<? extends Constraint>> ourValidChecker;

  public static boolean isValid(@Nullable Constraint constraint) {
    if (constraint == null)
      return false;
    TypedKey<? extends Constraint> type = constraint.getType();
    Condition<Constraint> checker = (Condition<Constraint>) ourValidChecker.get(type);
    assert checker != null : type;
    return checker.isAccepted(constraint);
  }

  static {
    Map<TypedKey, Condition<? extends Constraint>> checkers = Collections15.hashMap();
    Condition<CompositeConstraint> compositeChecker = new Condition<CompositeConstraint>() {
      public boolean isAccepted(CompositeConstraint value) {
        List<? extends Constraint> children = value.getChildren();
        if (children == null)
          return false;
        for (Constraint constraint : children) {
          if (constraint == null || !isValid(constraint))
            return false;
        }
        return true;
      }
    };
    checkers.put(CompositeConstraint.AND, compositeChecker);
    checkers.put(CompositeConstraint.OR, compositeChecker);
    checkers.put(ConstraintNegation.NEGATION, new Condition<ConstraintNegation>() {
      public boolean isAccepted(ConstraintNegation negation) {
        Constraint constraint = negation.getNegated();
        return constraint != null && isValid(constraint);
      }
    });

    OneFieldChecker<FieldIntConstraint> intChecker = new OneFieldChecker<FieldIntConstraint>() {
      protected boolean checkSpecial(FieldIntConstraint value) {
        return value.getIntValue() != null;
      }
    };
    checkers.put(FieldIntConstraint.INT_EQUALS, intChecker);
    checkers.put(FieldIntConstraint.INT_GREATER, intChecker);
    checkers.put(FieldIntConstraint.INT_LESS, intChecker);
    checkers.put(FieldIntConstraint.INT_GREATER_EQUAL, intChecker);
    checkers.put(FieldIntConstraint.INT_LESS_EQUAL, intChecker);

    OneFieldChecker<FieldSubstringsConstraint> substringsChecker = new OneFieldChecker<FieldSubstringsConstraint>() {
      protected boolean checkSpecial(FieldSubstringsConstraint value) {
        List<String> substrings = value.getSubstrings();
        if (substrings == null || substrings.isEmpty())
          return false;
        for (String str : substrings) {
          if (str == null)
            return false;
        }
        return true;
      }
    };
    checkers.put(FieldSubstringsConstraint.MATCHES_ANY, substringsChecker);
    checkers.put(FieldSubstringsConstraint.MATCHES_ALL, substringsChecker);

    checkers.put(FieldEqualsConstraint.EQUALS_TO, new OneFieldChecker<FieldEqualsConstraint>() {
      protected boolean checkSpecial(FieldEqualsConstraint value) {
        return value.getExpectedValue() != null;
      }
    });

    checkers.put(Constraint.TRUE, Condition.<Constraint>always());

    Condition<DateConstraint> dateChecker = new OneFieldChecker<DateConstraint>() {
      protected boolean checkSpecial(DateConstraint value) {
        return value.getDate() != null;
      }
    };
    checkers.put(DateConstraint.BEFORE, dateChecker);
    checkers.put(DateConstraint.AFTER, dateChecker);
    checkers.put(FieldSubsetConstraint.INTERSECTION, new OneFieldChecker<FieldSubsetConstraint>() {
      protected boolean checkSpecial(FieldSubsetConstraint value) {
        Set<Long> subset = value.getSubset();
        for (Long artifact : subset) {
          if (artifact == null)
            return false;
        }
        return true;
      }
    });

    checkers.put(BlackBoxConstraint.BLACK_BOX, Condition.<BlackBoxConstraint>always());
    checkers.put(IsEmptyConstraint.IS_EMPTY, Condition.<IsEmptyConstraint>always());
    checkers.put(ContainsTextConstraint.CONTAINS_TEXT, Condition.<Constraint>always());

    ourValidChecker = Collections.unmodifiableMap(checkers);
  }

  private static abstract class OneFieldChecker<T extends OneFieldConstraint> extends Condition<T> {
    public final boolean isAccepted(T value) {
      if (value.getAttribute() == null)
        return false;
      return checkSpecial(value);
    }

    protected abstract boolean checkSpecial(T value);
  }
}
