package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.constraint.Constraint;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.images.Icons;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class EnumSubtreeKind implements EnumConstraintKind {
  private final DBAttribute<Long> myParentAttr;
  private final TypedKey<Set<Long>> mySubtreeKey;
  private final Modifiable myModifiable;

  public EnumSubtreeKind(DBAttribute<Long> parentAttr, TypedKey<Set<Long>> subtreeKey, Modifiable modifiable) {
    myParentAttr = parentAttr;
    mySubtreeKey = subtreeKey;
    myModifiable = modifiable;
  }

  @Override
  public Constraint createConstraint(List<Long> items, DBAttribute attribute) {
//    FieldEqualsConstraint[] result = new FieldEqualsConstraint[items.size()];
//    for(int i = 0; i < items.size(); i++) {
//      result[i] = FieldEqualsConstraint.Simple.create(attribute, items.get(i));
//    }
//    return CompositeConstraint.Simple.or(result);
    return EnumConstraintKind.INCLUSION.createConstraint(items, attribute);
  }

  @Override
  public BoolExpr<DP> createFilter(List<Long> items, DBAttribute attribute) {
    if(attribute.getScalarClass() == Long.class && attribute.getComposition() == DBAttribute.ScalarComposition.SCALAR) {
      return new DPUnder(attribute, myParentAttr, items, myModifiable).term();
    }
    assert false : attribute;
    return null;
  }

  @Override
  public DBAttribute<Long> getParentAttribute() {
    return myParentAttr;
  }

  @Override
  public TypedKey<Set<Long>> getSubtreeKey() {
    return mySubtreeKey;
  }

  @Override
  public String getFormulaOperation() {
    return TREE_OPERATION;
  }

  @Override
  public Icon getIcon() {
    return Icons.QUERY_CONDITION_ENUM_TREE;
  }

  private static class DPUnder extends DP {
    @NotNull private final DBAttribute<Long> myAttribute;
    @NotNull private final DBAttribute<Long> myParentAttribute;
    @NotNull private final Set<Long> myItems;
    @NotNull private final Modifiable myModifiable;

    private DPUnder(
      @NotNull DBAttribute<Long> attribute, @NotNull DBAttribute<Long> parentAttribute,
      @NotNull Collection<Long> items, Modifiable modifiable)
    {
      myAttribute = attribute;
      myParentAttribute = parentAttribute;
      myItems = Collections15.hashSet(items);
      myModifiable = modifiable;
    }

    @Override
    public BoolExpr<DP> resolve(DBReader reader, @Nullable final ResolutionSubscription subscription) {
      if(subscription != null) {
        myModifiable.addChangeListener(subscription.getLife(), subscription);
      }

      Set<Long> items = Collections15.hashSet();
      for (Long item : myItems) {
        resolveItem(reader, item, items);
      }
      return DPEquals.equalOneOf(myAttribute, items);
    }

    private void resolveItem(DBReader reader, long item, Set<Long> items) {
      items.add(item);
      final LongArray children = reader.query(DPEquals.create(myParentAttribute, item)).copyItemsSorted();
      for(final LongIterator it = children.iterator(); it.hasNext();) {
        resolveItem(reader, it.nextValue(), items);
      }
    }

    @Override
    public boolean accept(long item, DBReader reader) {
      Log.error(this + ": usage of unresolved DP");
      return false;
    }

    @Override
    protected boolean equalDP(DP other) {
      if(other == this) {
        return true;
      }
      if(other instanceof DPUnder) {
        final DPUnder otherDP = (DPUnder) other;
        return myAttribute.equals(otherDP.myAttribute)
          && myParentAttribute.equals(otherDP.myParentAttribute)
          && myItems.equals(otherDP.myItems);
      }
      return false;
    }

    @Override
    protected int hashCodeDP() {
      int h = myAttribute.hashCode();
      h = 29 * h + myParentAttribute.hashCode();
      h = 29 * h + myItems.hashCode();
      return h;
    }

    @Override
    public String toString() {
      return myAttribute + " inSubtreesOf " + myItems;
    }
  }
}
