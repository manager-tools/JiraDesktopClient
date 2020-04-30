package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public abstract class BaseCollectRule<C> implements Rule {
  @Nullable
  private final TypedKey<? extends CompositeConstraint> myGroup;

  public BaseCollectRule(@Nullable TypedKey<? extends CompositeConstraint> group) {
    myGroup = group;
  }

  @Nullable
  public ConstraintTreeElement process(ConstraintTreeElement element) {
    ConstraintTreeNode group;
    if (myGroup != null) {
      group = ConstraintTreeNode.cast(myGroup, element);
      if (group == null)
        return null;
    } else {
      if (!(element instanceof ConstraintTreeNode))
        return null;
      group = (ConstraintTreeNode) element;
    }
    ConstraintTreeNode.ChildrenIterator it = group.iterator();
    C context = null;
    while (it.forward()) {
      if (!(it.getCurrent() instanceof ConstraintTreeLeaf))
        continue;
      context = processChild(it, context);
    }
    if (context == null)
      return null;
    return applyResult(group, context);
  }

  @Nullable
  protected abstract ConstraintTreeElement applyResult(ConstraintTreeNode group, @NotNull C context);

  @Nullable
  protected abstract C processChild(ConstraintTreeNode.ChildrenIterator iterator, @Nullable C context);
}
