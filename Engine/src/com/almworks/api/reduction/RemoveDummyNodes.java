package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author dyoma
 */
public class RemoveDummyNodes implements Rule {
  public static final Rule INSTANCE = new RemoveDummyNodes();

  @Nullable
  public ConstraintTreeElement process(ConstraintTreeElement element) {
    if (!(element instanceof ConstraintTreeNode))
      return null;
    ConstraintTreeNode node = (ConstraintTreeNode) element;
    if (node.getChildCount() == 0)
      return ConstraintTreeLeaf.createTrue(node.isNegated());
    assert node.getChildCount() > 0;
    if (node.getChildCount() == 1) {
      ConstraintTreeElement child = node.getChildAt(0);
      if (node.isNegated())
        child.negate();
      return child;
    }
    boolean wasChanged = removeSame(node);
    TypedKey<? extends CompositeConstraint> nodeType = node.getType();
    assert CompositeConstraint.AND.equals(nodeType) || CompositeConstraint.OR.equals(nodeType) : nodeType;
    for (int i = 0; i < node.getChildCount(); i++) {
      ConstraintTreeElement child = node.getChildAt(i);
      //noinspection ChainOfInstanceofChecks
      if (child instanceof ConstraintTreeNode)
        wasChanged |= simplifyNode((ConstraintTreeNode) child);
      else if (child instanceof ConstraintTreeLeaf) {
        ConstraintTreeLeaf leaf = (ConstraintTreeLeaf) child;
        if (leaf.getConstraint().getType() != Constraint.TRUE)
          continue;
        boolean isTrue = !leaf.isNegated();
        ConstraintTreeElement replacement; // null means just remove leaf, not null - replace node
        if (CompositeConstraint.OR.equals(nodeType)) {
          replacement = isTrue ? ConstraintTreeLeaf.createTrue() : null;
        } else if (CompositeConstraint.AND.equals(nodeType)) {
          replacement = isTrue ? null : ConstraintTreeLeaf.createFalse();
        } else
          continue; // Unknown type
        if (replacement == null) {
          node.removeChild(leaf);
          i--;
          wasChanged = true;
        } else {
          if (node.isNegated())
            replacement.negate();
          return replacement;
        }
      }
    }
    return wasChanged ? element : null;
  }

  private boolean simplifyNode(ConstraintTreeNode childNode) {
    ConstraintTreeNode node = childNode.getParent();
    assert node != null;
    int childCount = childNode.getChildCount();
    if (childCount == 0) {
      node.removeChild(childNode);
      return true;
    } else if (childCount == 1) {
      ConstraintTreeElement child = childNode.getChildAt(0);
      if (childNode.isNegated())
        child.negate();
      node.replaceChild(childNode, child);
      return true;
    }
    return false;
  }

  private boolean removeSame(ConstraintTreeNode node) {
    TypedKey<? extends CompositeConstraint> nodeType = node.getType();
    if (!CompositeConstraint.AND.equals(nodeType) && !CompositeConstraint.OR.equals(nodeType))
      return false;
    Map<Constraint, ConstraintTreeLeaf> map = Collections15.hashMap();
    boolean wasChanged = false;
    for (int i = 0; i < node.getChildCount(); i++) {
      ConstraintTreeElement child = node.getChildAt(i);
      if (!(child instanceof ConstraintTreeLeaf))
        continue;
      ConstraintTreeLeaf leaf = (ConstraintTreeLeaf) child;
      Constraint mapKey = leaf.getConstraint();
      ConstraintTreeLeaf prevChild = map.get(mapKey);
      if (prevChild == null) {
        map.put(mapKey, leaf);
        continue;
      }
      if (prevChild.isNegated() == child.isNegated()) {
        node.removeChild(leaf);
      } else {
        ConstraintTreeLeaf replacement;
        if (CompositeConstraint.AND.equals(nodeType))
          replacement = ConstraintTreeLeaf.createFalse();
        else if (CompositeConstraint.OR.equals(nodeType))
          replacement = ConstraintTreeLeaf.createTrue();
        else {
          assert false : nodeType;
          continue;
        }
        node.removeChild(prevChild);
        node.replaceChild(child, replacement);
        map.remove(mapKey);
        map.put(replacement.getConstraint(), replacement);
      }
      wasChanged = true;
      i--;
    }
    return wasChanged;
  }
}
