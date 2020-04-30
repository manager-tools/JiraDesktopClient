package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldEqualsConstraint;
import com.almworks.api.constraint.FieldSubsetConstraint;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPIntersects;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.images.Icons;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * @author dyoma
 */
public interface EnumConstraintKind {
  EnumConstraintKind INCLUSION = new InclusionEnumKind();
  EnumConstraintKind INTERSECTION = new IntersectionEnumKind();

  String INCLUSION_OPERATION = "in";
  String INTERSECTION_OPERATION = "intersects";
  String TREE_OPERATION = "under";

  BoolExpr<DP> createFilter(List<Long> items, DBAttribute attribute);

  Constraint createConstraint(List<Long> items, DBAttribute attribute);

  String getFormulaOperation();

  Icon getIcon();

  DBAttribute<Long> getParentAttribute();

  TypedKey<Set<Long>> getSubtreeKey();

  public static class InclusionEnumKind implements EnumConstraintKind {
    public BoolExpr<DP> createFilter(List<Long> items, DBAttribute attribute) {
      assert Long.class.equals(attribute.getValueClass()) : attribute;
      assert attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR;
      return DPEquals.<Long>equalOneOf(attribute, items);
    }

    public Constraint createConstraint(List<Long> items, DBAttribute attribute) {
      int size = items.size();
      if (size == 0) {
        return Constraint.FALSE;
      } else if (size == 1) {
        return FieldEqualsConstraint.Simple.create(attribute, items.get(0));
      } else {
        FieldEqualsConstraint[] result = new FieldEqualsConstraint[size];
        for (int i = 0; i < size; i++) {
          Long artifact = items.get(i);
          result[i] = FieldEqualsConstraint.Simple.create(attribute, artifact);
        }
        return CompositeConstraint.Simple.or(result);
      }
    }

    public String getFormulaOperation() {
      return INCLUSION_OPERATION;
    }

    public Icon getIcon() {
      return Icons.QUERY_CONDITION_ENUM_ATTR;
    }

    @Override
    public DBAttribute<Long> getParentAttribute() {
      return null;
    }

    @Override
    public TypedKey<Set<Long>> getSubtreeKey() {
      return null;
    }
  }

  public static class IntersectionEnumKind implements EnumConstraintKind {
    public BoolExpr<DP> createFilter(List<Long> items, final DBAttribute attribute) {
      assert attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR : attribute;
      return DPIntersects.create(attribute, items);
    }

    public Constraint createConstraint(List<Long> items, DBAttribute attribute) {
      return FieldSubsetConstraint.Simple.intersection(attribute, items);
    }

    public String getFormulaOperation() {
      return INTERSECTION_OPERATION;
    }

    public Icon getIcon() {
      return Icons.QUERY_CONDITION_ENUM_SET;
    }

    public String getReadonlyPrimitiveActionName(boolean replaceMultiEnum, boolean clear) {
      return clear ? "clear" : (replaceMultiEnum ? "replace" : "add");
    }

    @Override
    public DBAttribute<Long> getParentAttribute() {
      return null;
    }

    @Override
    public TypedKey<Set<Long>> getSubtreeKey() {
      return null;
    }
  }
}
