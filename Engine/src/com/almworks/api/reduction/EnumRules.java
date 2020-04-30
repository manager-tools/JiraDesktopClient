package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraints;
import com.almworks.api.constraint.EnumConstraint;
import com.almworks.api.constraint.FieldEqualsConstraint;
import com.almworks.items.api.DBAttribute;
import com.almworks.util.Pair;
import com.almworks.util.collections.MultiMap;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author dyoma
 */
public class EnumRules {
  public static final List<Rule> UNITE;
  static {
    List<Rule> rules = Collections15.arrayList();
    rules.add(UniteEnums.INSTANCE);
    rules.addAll(ReductionUtil.SIMPLIFY);
    UNITE = Collections.unmodifiableList(rules);
  }


  public static class Collect2 extends BaseCollectRule<MultiMap<Pair<Boolean, DBAttribute>, Long>> {
    private final Collection<? extends DBAttribute<?>> myAttributes;

    public Collect2(@Nullable Collection<? extends DBAttribute<?>> attributes) {
      super(null);
      myAttributes = attributes;
    }

    protected ConstraintTreeElement applyResult(ConstraintTreeNode group, @NotNull MultiMap<Pair<Boolean, DBAttribute>, Long> context) {
      for (Pair<Boolean, DBAttribute> attr : context.keySet())
        group.addLeaf(new EnumConstraint(attr.getSecond(), context.getAll(attr)), attr.getFirst());
      return group;
    }

    protected MultiMap<Pair<Boolean, DBAttribute>, Long> processChild(
      ConstraintTreeNode.ChildrenIterator iterator, MultiMap<Pair<Boolean, DBAttribute>, Long>  context) 
    {
      FieldEqualsConstraint equals = iterator.getCurrentLeaf(FieldEqualsConstraint.EQUALS_TO);
      if (equals == null) return context;
      DBAttribute attribute = equals.getAttribute();
      if (!shouldCollect(attribute)) return context;
      ConstraintTreeNode parent = iterator.getParent(CompositeConstraint.OR);
      boolean negated = iterator.getCurrent().isNegated();
      Long expectedValue = equals.getExpectedValue();
      if (parent == null) {
        if (context == null)
          context = MultiMap.create();
        iterator.replaceCurrentWithLeaf(new EnumConstraint(attribute, Collections.singletonList(expectedValue)), negated);
        return context;
      }
      if (context == null)
        context = MultiMap.create();
      context.add(Pair.create(negated, attribute), expectedValue);
      iterator.removeCurrent();
      return context;
    }

    protected boolean shouldCollect(DBAttribute attribute) {
      return myAttributes == null || myAttributes.contains(attribute);
    }
  }

  private static class UniteEnums implements Rule {
    public static final Rule INSTANCE = new UniteEnums();

    @Override
    public ConstraintTreeElement process(ConstraintTreeElement element) {
      final Boolean or = isOrNode(element);
      if(or == null) {
        // not a node, or unknown type of node
        return null;
      }

      final ConstraintTreeNode node = (ConstraintTreeNode) element;
      final DBAttribute[] attributes = new DBAttribute[node.getChildCount()];
      final ConstraintTreeLeaf[] positive = new ConstraintTreeLeaf[node.getChildCount()];
      final ConstraintTreeLeaf[] negative = new ConstraintTreeLeaf[node.getChildCount()];

      boolean changed = false;
      for(int i = 0; i < node.getChildCount(); i++) {
        final ConstraintTreeElement child = node.getChildAt(i);
        final DBAttribute attr = getEnumLeafAttribute(child);
        if(attr == null) {
          // not a leaf, not an enum constraint
          continue;
        }

        final ConstraintTreeLeaf leaf = (ConstraintTreeLeaf) child;
        final ConstraintTreeLeaf[] array = leaf.isNegated() ? negative : positive;

        final int index = ArrayUtil.indexOf(attributes, 0, i, attr);
        if(index < 0) {
          // haven't seen this attribute yet
          attributes[i] = attr;
          array[i] = leaf;
          continue;
        }

        final ConstraintTreeLeaf prev = array[index];
        if(prev == null) {
          // haven't seen a child with the same negated-ness for this attribute yet
          array[index] = leaf;
          continue;
        }

        final boolean unite = or.booleanValue() != leaf.isNegated();
        final ConstraintTreeLeaf replacement = unite(prev, leaf, unite);
        if(replacement == null) {
          // this shouldn't happen because of the above code
          continue;
        }

        if(replacement == leaf) {
          // prev has empty values list
          node.removeChild(prev);
          i--;
          changed = true;
          array[index] = leaf;
          continue;
        }

        // replacement == prev (i.e. leaf has empty values list) OR replacement is a new leaf
        node.removeChild(leaf);
        i--;
        if(replacement != prev) {
          // replacement is a new leaf
          node.replaceChild(prev, replacement);
          array[index] = replacement;
        }
        changed = true;
      }

      return changed ? node : null;
    }

    private Boolean isOrNode(ConstraintTreeElement element) {
      if(element instanceof ConstraintTreeNode) {
        final TypedKey<? extends CompositeConstraint> type = ((ConstraintTreeNode)element).getType();
        if(type == CompositeConstraint.OR) {
          return true;
        } else if(type == CompositeConstraint.AND) {
          return false;
        }
      }
      return null;
    }

    private DBAttribute getEnumLeafAttribute(ConstraintTreeElement child) {
      if(child instanceof ConstraintTreeLeaf) {
        final ConstraintTreeLeaf leaf = (ConstraintTreeLeaf) child;
        final EnumConstraint constraint = Constraints.cast(EnumConstraint.ENUM_CONSTRAINT, leaf.getConstraint());
        if(constraint != null) {
          return constraint.getAttribute();
        }
      }
      return null;
    }

    @Nullable
    private ConstraintTreeLeaf unite(ConstraintTreeLeaf prev, ConstraintTreeLeaf leaf, boolean unite) {
      final boolean negated = prev.isNegated();
      if(negated != leaf.isNegated()) {
        assert false; // enforced by the caller
        return null;
      }

      final EnumConstraint e1 = Constraints.cast(EnumConstraint.ENUM_CONSTRAINT, prev.getConstraint());
      final EnumConstraint e2 = Constraints.cast(EnumConstraint.ENUM_CONSTRAINT, leaf.getConstraint());
      if(e1 == null || e2 == null || !Util.equals(e1.getAttribute(), e2.getAttribute())) {
        assert false; // enforced by the caller
        return null;
      }

      final List<Long> v1 = e1.getValues();
      final List<Long> v2 = e2.getValues();
      if(v1.isEmpty()) {
        return leaf;
      }
      if(v2.isEmpty()) {
        return prev;
      }

      final Set<Long> result = Collections15.hashSet(v1);
      if(unite) {
        result.addAll(v2);
      } else {
        result.retainAll(v2);
      }

      final ConstraintTreeLeaf repl = new ConstraintTreeLeaf(
        new EnumConstraint(e1.getAttribute(), Collections15.arrayList(result)));
      if(negated) {
        repl.negate();
      }
      return repl;
    }
  }

}
