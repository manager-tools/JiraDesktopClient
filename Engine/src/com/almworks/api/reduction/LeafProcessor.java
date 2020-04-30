package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: dyoma
 * Date: Mar 22, 2010
 * Time: 4:37:46 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class LeafProcessor {
  private final List<ConstraintTreeNode> myStack = Collections15.arrayList();
  private ConstraintTreeLeaf myCurrent = null;

  public final ConstraintTreeElement process(ConstraintTreeElement element) {
    if (element instanceof ConstraintTreeLeaf) return doProcessLeaf((ConstraintTreeLeaf) element);
    if (!(element instanceof ConstraintTreeNode)) {
      Log.error("Wrong node " + element);
      return element;
    }
    return processNode((ConstraintTreeNode) element);
  }

  protected final boolean isEffectivlyNegated() {
    if (myCurrent == null) {
      Log.error(new Throwable());
      return false;
    }
    ConstraintTreeElement e = myCurrent;
    boolean neg = false;
    while (e != null) {
      if (e.isNegated()) neg = !neg;
      e = e.getParent();
    }
    return neg;
  }

  protected final boolean isDescendantOfOr() {
    return isDescendantOf(CompositeConstraint.OR);
  }

  protected final boolean isDescendantOfAnd() {
    return isDescendantOf(CompositeConstraint.AND);
  }

  protected final boolean isDescendantOf(TypedKey key) {
    for (ConstraintTreeNode node : myStack) {
      if (node.getType() == key) return true;
    }
    return false;
  }

  private ConstraintTreeNode processNode(ConstraintTreeNode node) {
    myStack.add(node);
    try {
      int i = 0;
      while (i < node.getChildCount()) {
        ConstraintTreeElement child = node.getChildAt(i);
        ConstraintTreeElement replacement;
        if (child instanceof ConstraintTreeNode) replacement = processNode((ConstraintTreeNode) child);
        else if (child instanceof ConstraintTreeLeaf) replacement = doProcessLeaf((ConstraintTreeLeaf) child);
        else {
          Log.error("Wrong node " + child);
          replacement = child;
        }
        if (child == replacement) i++;
        else if (replacement == null) node.removeChild(child);
        else {
          node.replaceChildAt(replacement, i);
          i++;
        }
      }
      return node;
    } finally {
      myStack.remove(myStack.size() - 1);
    }
  }

  private ConstraintTreeElement doProcessLeaf(ConstraintTreeLeaf leaf) {
    myCurrent = leaf;
    try {
      return processLeaf(leaf);
    } finally {
      myCurrent = null;
    }
  }

  protected abstract ConstraintTreeElement processLeaf(ConstraintTreeLeaf leaf);
}
