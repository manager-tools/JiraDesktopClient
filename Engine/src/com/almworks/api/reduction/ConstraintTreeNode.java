package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.Constraints;
import com.almworks.util.collections.Convertors;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Vasya
 */
public class ConstraintTreeNode extends ConstraintTreeElement {
  private final List<ConstraintTreeElement> myChildren = Collections15.arrayList();
  private TypedKey<? extends CompositeConstraint> myType;

  public void addChild(ConstraintTreeElement element) {
    addChild(element, myChildren.size());
  }

  public ConstraintTreeLeaf addLeaf(Constraint constraint) {
    return addLeaf(constraint, false);
  }


  public ConstraintTreeLeaf addLeaf(Constraint constraint, boolean negated) {
    assert !(constraint instanceof CompositeConstraint) : constraint;
    ConstraintTreeLeaf leaf = new ConstraintTreeLeaf(constraint);
    addChild(leaf);
    if (negated)
      leaf.negate();
    return leaf;
  }

  public TypedKey<? extends CompositeConstraint> getType() {
    return myType;
  }

  public void addAllChildren(List<? extends ConstraintTreeElement> children) {
    for (ConstraintTreeElement child : children) {
      addChild(child, myChildren.size());
    }
  }

  public void addChild(ConstraintTreeElement element, int index) {
    makeOwnChild(element);
    myChildren.add(index, element);
  }

  private void makeOwnChild(@NotNull ConstraintTreeElement element) {
    assert !myChildren.contains(element) : element + " all:" + myChildren;
    ConstraintTreeNode oldParent = element.getParent();
    if (oldParent != null)
      oldParent.removeChild(element);
    element.setParent(this);
  }

  public boolean removeChild(ConstraintTreeElement element) {
    return removeChildAt(myChildren.indexOf(element));
  }

  public boolean removeChildAt(int index) {
    if (index < 0 || index >= myChildren.size()) return false;
    ConstraintTreeElement element = myChildren.get(index);
    assert element.getParent() == this : element;
    element.setParent(null);
    myChildren.remove(index);
    return true;
  }

  @NotNull
  protected Constraint createConstraintImpl() {
    int size = myChildren.size();
    if (size == 0)
      return Constraint.NO_CONSTRAINT;
    if (size == 1)
      return myChildren.get(0).createConstraint();
    List<Constraint> children = Collections15.arrayList();
    for (ConstraintTreeElement element : myChildren)
      children.add(element.createConstraint());
    return new CompositeConstraint.Simple(myType, children);
  }

  public void setType(TypedKey<? extends CompositeConstraint> type) {
    myType = type;
  }

  public ConstraintTreeNode getCopy() {
    ConstraintTreeNode result = new ConstraintTreeNode();
    result.setType(myType);
    if (isNegated())
      result.negate();
    for (ConstraintTreeElement element : myChildren)
      result.addChild(element.getCopy());
    return result;
  }

  public List<ConstraintTreeElement> getChildren() {
    return Collections.unmodifiableList(myChildren);
  }

  public ConstraintTreeElement getChildAt(int index) {
    return myChildren.get(index);
  }

  public List<ConstraintTreeElement> copyChildren() {
    return Collections15.arrayList(myChildren);
  }

  public String toString() {
    final String separator;
    if (CompositeConstraint.AND.equals(myType))
      separator = " ^ ";
    else if (CompositeConstraint.OR.equals(myType))
      separator = " v ";
    else if (myType == null)
      separator = " <null> ";
    else
      separator = " " + myType.getName() + " ";
    return (isNegated() ? "!" : "") + "(" + TextUtil.separate(myChildren, separator, Convertors.getToString()) + ")";
  }

  @Nullable
  public static TypedKey<? extends CompositeConstraint> getType(ConstraintTreeElement element) {
    if (!(element instanceof ConstraintTreeNode))
      return null;
    return ((ConstraintTreeNode) element).myType;
  }

  @Nullable
  public static ConstraintTreeNode cast(TypedKey<? extends CompositeConstraint> type, ConstraintTreeElement element) {
    TypedKey<? extends CompositeConstraint> elementType = getType(element);
    return type == elementType && elementType != null ? (ConstraintTreeNode) element : null;
  }

  @Nullable
  public static ConstraintTreeLeaf castLeaf(TypedKey<? extends Constraint> type, ConstraintTreeElement element) {
    if (!(element instanceof ConstraintTreeLeaf))
      return null;
    ConstraintTreeLeaf leaf = (ConstraintTreeLeaf) element;
    Constraint constraint = Constraints.cast(type, leaf.getConstraint());
    return constraint != null ? leaf : null;
  }

  public int getChildCount() {
    return myChildren.size();
  }

  /**
   * @param oldChild
   * @param newChild if null does nothing
   */
  public void replaceChild(@NotNull ConstraintTreeElement oldChild, @Nullable ConstraintTreeElement newChild) {
    if (newChild == null)
      return;
    if (newChild == oldChild)
      return;
    assert oldChild.getParent() == this : oldChild;
    int index = myChildren.indexOf(oldChild);
    replaceChildAt(newChild, index);
  }

  public void replaceChildAt(ConstraintTreeElement newChild, int index) {
    assert index >= 0 && index < myChildren.size() : index;
    ConstraintTreeElement oldChild = myChildren.get(index);
    makeOwnChild(newChild);
    myChildren.set(index, newChild);
    oldChild.setParent(null);
  }

  public ChildrenIterator iterator() {
    return new ChildrenIterator();
  }

  public class ChildrenIterator {
    private int myIndex = -1;
    private ConstraintTreeElement myCurrent = null;

    public boolean forward() {
      myIndex++;
      if (myIndex >= getChildCount())
        return false;
      myCurrent = getChildAt(myIndex);
      return true;
    }

    public ConstraintTreeElement getCurrent() {
      return myCurrent;
    }

    @Nullable
    public <T extends Constraint> T getCurrentLeaf(TypedKey<T> type) {
      if (!(myCurrent instanceof ConstraintTreeLeaf))
        return null;
      return Constraints.cast(type, ((ConstraintTreeLeaf) myCurrent).getConstraint());
    }

    public void removeCurrent() {
      if (myCurrent == null)
        return;
      if (!removeChild(myCurrent))
        return;
      myCurrent = null;
      myIndex--;
    }

    @Nullable
    public ConstraintTreeNode getParent(TypedKey<? extends CompositeConstraint> key) {
      return cast(key,ConstraintTreeNode.this);
    }

    public ConstraintTreeLeaf replaceCurrentWithLeaf(Constraint constraint, boolean negated) {
      ConstraintTreeLeaf leaf = new ConstraintTreeLeaf(constraint);
      if (negated)
        leaf.negate();
      assert myChildren.get(myIndex) == myCurrent;
      replaceChildAt(leaf, myIndex);
      myCurrent = leaf;
      return leaf;
    }
  }
}
