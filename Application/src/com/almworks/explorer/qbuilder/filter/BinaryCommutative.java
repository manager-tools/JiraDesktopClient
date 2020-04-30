package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.UnresolvedNameException;
import com.almworks.api.application.qb.CannotSuggestNameException;
import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.EditorGroupNode;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
public abstract class BinaryCommutative extends CompositeFilterNode {
  public static final InfixFormulaSerializer AND_SERIALIZER = new InfixFormulaSerializer("&") {
    public BinaryCommutative create(List<FilterNode> children) {
      return new And(children);
    }
  };

  public static final InfixFormulaSerializer OR_SERIALIZER = new InfixFormulaSerializer("|") {
    public BinaryCommutative create(List<FilterNode> children) {
      return new Or(children);
    }
  };

  /**
   * must come in reverse priority order
   */
  static final CompositeFormulaSerializer[] COMMUTATIVE_OPERATIONS =
    new CompositeFormulaSerializer[] {Or.OR_SERIALIZER, And.AND_SERIALIZER, NeitherFilterNode.SERIALIZER};
  private final TypedKey<? extends CompositeConstraint> myConstraintType;

  protected BinaryCommutative(List<FilterNode> arguments, TypedKey<? extends CompositeConstraint> constraintType,
    CompositeFormulaSerializer serializer)
  {
    super(arguments, serializer);
    myConstraintType = constraintType;
  }

  @Nullable
  public BoolExpr<DP> createFilter(ItemHypercube hypercube) throws UnresolvedNameException {
    Iterator<FilterNode> iterator = getChildren().iterator();
    if (!iterator.hasNext())
      return null;
    BoolExpr<DP> result = iterator.next().createFilter(hypercube);
    while (iterator.hasNext() && result != null) {
      FilterNode filterNode = iterator.next();
      BoolExpr<DP> nextFilter = filterNode.createFilter(hypercube);
      if (nextFilter != null)
        result = createOperation(result, nextFilter);
    }
    return result;
  }

  protected abstract BoolExpr<DP> createOperation(BoolExpr<DP> allPrev, BoolExpr<DP> next);

  @Nullable
  public CompositeConstraint createConstraint(ItemHypercube cube) {
    List<Constraint> constraints = Collections15.arrayList();
    for (FilterNode node : getChildren()) {
      Constraint constraint = node.createConstraint(cube);
      if (constraint == null)
        return null;
      constraints.add(constraint);
    }
    return new CompositeConstraint.Simple(myConstraintType, constraints);
  }

  @Nullable
  public String getSuggestedName(Map<TypedKey<?>, ?> hints) throws CannotSuggestNameException {
    List<FilterNode> list = getChildren();
    if (list == null || list.size() == 0)
      return null;

    if (list.size() == 1)
      return list.get(0).getSuggestedName(hints);

    for (FilterNode element : list) {
      if (element instanceof CompositeFilterNode) {
        // the name would be too complex
        throw new CannotSuggestNameException();
      }
    }

    StringBuffer buffer = new StringBuffer();
    for (FilterNode element : list) {
      String elementName = element.getSuggestedName(hints);
      if (elementName != null) {
        if (buffer.length() > 0){
          buffer.append(' ').append(myConstraintType.getName()).append(' ');
        }
        buffer.append(elementName);
      }
    }

    return buffer.length() == 0 ? null : buffer.toString();
  }


  public static class And extends BinaryCommutative {
    public And(List<FilterNode> arguments) {
      super(arguments, CompositeConstraint.AND, AND_SERIALIZER);
    }

    protected GroupingEditorNode createEmptyNode(EditorContext context) {
      return EditorGroupNode.createAnd(context, null);
    }

    protected BoolExpr<DP> createOperation(BoolExpr<DP> allPrev, BoolExpr<DP> next) {
      return allPrev.and(next);
    }

    public FilterNode createCopy() {
      return new And(createChildrenCopy());
    }
  }


  public static class Or extends BinaryCommutative {
    public Or(List<FilterNode> arguments) {
      super(arguments, CompositeConstraint.OR, OR_SERIALIZER);
    }

    protected GroupingEditorNode createEmptyNode(EditorContext context) {
      return EditorGroupNode.createOr(context, null);
    }

    protected BoolExpr<DP> createOperation(BoolExpr<DP> allPrev, BoolExpr<DP> next) {
      return allPrev.or(next);
    }

    public FilterNode createCopy() {
      return new Or(createChildrenCopy());
    }
  }
}
