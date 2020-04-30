package com.almworks.api.constraint;

import com.almworks.api.engine.Engine;
import com.almworks.api.reduction.*;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.tests.BaseTestCase;

import java.util.Date;

/**
 * @author dyoma
 */
public class DateRulesTests extends BaseTestCase {
  private static final DBNamespace NS = Engine.NS.subNs("test.date");
  private static final DBAttribute myAttr = NS.date("date", "Date", false);

  public void testConvertToPositive() {
    DateConstraint constraint = after(3);
    ConstraintNegation.Simple negated = new ConstraintNegation.Simple(constraint);
    CompositeConstraint.Simple and = CompositeConstraint.Simple.and(negated, constraint);
    ConstraintTreeNode reduced = reduceComposite(and, DateConstraint.CONVERT_TO_POSITIVE);
    DateConstraint converted = (DateConstraint) reduced.getChildAt(0).createConstraint();
    Constraint kept = reduced.getChildAt(1).createConstraint();
    assertEquals(kept, constraint);
    assertEquals(DateConstraint.BEFORE, converted.getType());
    assertEquals(date(2), converted.getDate());
  }

  public void testIntersectRandes() {
    ConstraintTreeNode reduced = reduceComposite(
      CompositeConstraint.Simple.and(after(3), after(4), before(6), before(8)), DateConstraint.INTERSECT_RANGES);
    ConstraintTreeElement after = reduced.getChildAt(1);
    ConstraintTreeElement before = reduced.getChildAt(0);
    checkDay(after, 4);
    checkDay(before, 6);

    ConstraintTreeLeaf aFalse = (ConstraintTreeLeaf) reduceAny(
      CompositeConstraint.Simple.and(after(6), after(8), before(3), before(4)), DateConstraint.INTERSECT_RANGES);
    assertEquals(Constraint.TRUE, aFalse.getConstraint().getType());
    assertTrue(aFalse.isNegated());
  }

  public void testOneDay() {
    ConstraintTreeNode reduced = reduceComposite(
      CompositeConstraint.Simple.and(after(3), before(3)), DateConstraint.INTERSECT_RANGES);
    ConstraintTreeElement after = reduced.getChildAt(1);
    ConstraintTreeElement before = reduced.getChildAt(0);
    checkDay(after, 3);
    checkDay(before, 3);
  }

  public void testUnitRanges() {
    ConstraintTreeNode reduced = reduceComposite(
      CompositeConstraint.Simple.or(after(6), after(8), before(3), before(4)), DateConstraint.UNITE_RANGES);
    ConstraintTreeElement after = reduced.getChildAt(1);
    ConstraintTreeElement before = reduced.getChildAt(0);
    checkDay(after, 6);
    checkDay(before, 4);

    ConstraintTreeLeaf aTrue = (ConstraintTreeLeaf) reduceAny(
      CompositeConstraint.Simple.or(after(3), after(4), before(6), before(8)), DateConstraint.UNITE_RANGES);
    assertEquals(Constraint.TRUE, aTrue.getConstraint().getType());
    assertFalse(aTrue.isNegated());
  }

  private void checkDay(ConstraintTreeElement element, int day) {
    DateConstraint constraint = (DateConstraint) element.createConstraint();
    assertEquals(date(day), constraint.getDate());
  }

  private ConstraintTreeNode reduceComposite(Constraint constraint, Rule rule) {
    return (ConstraintTreeNode) reduceAny(constraint, rule);
  }

  private ConstraintTreeElement reduceAny(Constraint constraint, Rule rule) {
    return ReductionUtil.reduce(ConstraintTreeElement.createTree(constraint), rule);
  }

  private DateConstraint after(int day) {
    return DateConstraint.Simple.after(date(day), myAttr);
  }

  private Date date(int day) {
    return new Date(2000, 2, day);
  }

  private DateConstraint before(int day) {
    return DateConstraint.Simple.before(date(day), myAttr);
  }
}
