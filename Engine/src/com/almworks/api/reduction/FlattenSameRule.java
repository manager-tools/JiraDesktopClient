package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import org.almworks.util.TypedKey;

import java.util.List;

/**
 * @author dyoma
 */
public class FlattenSameRule implements Rule {
  public static final Rule FLATTEN_ANDS = new FlattenSameRule(CompositeConstraint.AND);
  public static final Rule FLATTEN_ORS = new FlattenSameRule(CompositeConstraint.OR);

  private final TypedKey<? extends CompositeConstraint> myType;

  public FlattenSameRule(TypedKey<? extends CompositeConstraint> type) {
    myType = type;
  }

  public ConstraintTreeElement process(ConstraintTreeElement element) {
    ConstraintTreeNode node = ConstraintTreeNode.cast(myType, element);
    if (node == null)
      return null;
    assert node.getChildCount() > 0 : element;
    boolean wasChanged = false;
    for (int i = 0; i < node.getChildCount(); i++) {
      ConstraintTreeElement child = node.getChildAt(i);
      ConstraintTreeNode childNode = ConstraintTreeNode.cast(myType, child);
      if (childNode == null || childNode.isNegated())
        continue;
      final List<ConstraintTreeElement> ch = childNode.getChildren();
      for (int j = 0; j < ch.size(); j++)
        node.addChild(ch.get(j).getCopy(), i + j);
      node.removeChild(childNode);
      wasChanged = true;
    }
    return wasChanged ? node : null;
  }
}
