package com.almworks.api.constraint;

import com.almworks.api.reduction.ConstraintTreeElement;
import com.almworks.api.reduction.Rule;
import com.almworks.api.reduction.SpecificLeafRule;
import com.almworks.items.api.DBAttribute;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ReplaceIntersectionWithEqualsRule extends SpecificLeafRule<FieldSubsetConstraint> {
  public static final Rule REPLACE_ANY = new ReplaceIntersectionWithEqualsRule(null);
  private final Collection<DBAttribute> myAttributes;

  private ReplaceIntersectionWithEqualsRule(Collection<? extends DBAttribute<?>> attributes) {
    super(FieldSubsetConstraint.INTERSECTION);
    myAttributes = attributes != null ? new ArrayList<DBAttribute>(attributes) : null;
  }

  protected ConstraintTreeElement process(FieldSubsetConstraint constraint, boolean negated) {
    if (!shouldProcess(constraint)) return null;
    Set<Long> subset = constraint.getSubset();
    if (subset.size() == 0) {
      return ConstraintTreeElement.createTree(Constraint.NO_CONSTRAINT);
    }
    DBAttribute attribute = constraint.getAttribute();

    List<Constraint> options = Collections15.arrayList();
    for (Long value : subset) {
      options.add(FieldEqualsConstraint.Simple.create(attribute, value));
    }
    Constraint c = CompositeConstraint.Simple.or(options);
    if (negated) {
      c = new ConstraintNegation.Simple(c);
    }

    return ConstraintTreeElement.createTree(c);
  }

  private boolean shouldProcess(FieldSubsetConstraint constraint) {
    return myAttributes == null || myAttributes.contains(constraint.getAttribute());
  }
}
