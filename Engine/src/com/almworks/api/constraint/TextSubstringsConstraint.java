package com.almworks.api.constraint;

import com.almworks.api.reduction.*;
import com.almworks.items.api.DBAttribute;
import com.almworks.util.Pair;
import com.almworks.util.collections.MultiMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author dyoma
 */
public class TextSubstringsConstraint implements Constraint {
  public static final TypedKey<TextSubstringsConstraint> MATCHES_ALL = TypedKey.create("allSubstrings");
  public static final TypedKey<TextSubstringsConstraint> MATCHES_ANY = TypedKey.create("anySubstring");
  private final List<String> mySubstrings;
  private final Set<DBAttribute> myAttributes;
  private final TypedKey<TextSubstringsConstraint> myType;

  public TextSubstringsConstraint(Collection<DBAttribute> attributes, Collection<String> substrings,
    TypedKey<TextSubstringsConstraint> type) {
    myAttributes = Collections15.unmodifiableSetCopy(attributes);
    mySubstrings = Collections15.unmodifiableListCopy(substrings);
    myType = type;
  }

  public TypedKey<? extends TextSubstringsConstraint> getType() {
    return myType;
  }

  public Set<DBAttribute> getAttributes() {
    return myAttributes;
  }

  public List<String> getSubstrings() {
    return mySubstrings;
  }

  public static class CollectRule extends BaseCollectRule<MultiMap<Pair<Set<String>, Integer>, DBAttribute>> {
    private static final Integer ALL_POSITIVE = 1;
    private static final Integer ANY_POSITIVE = 2;
    private final Set<DBAttribute<?>> myTextAttributes;

    public CollectRule(DBAttribute<?> ... attributes) {
      this(Arrays.asList(attributes));
    }

    public CollectRule(Collection<? extends DBAttribute<?>> attributes) {
      super(CompositeConstraint.OR);
      myTextAttributes = Collections15.unmodifiableSetCopy(attributes);
    }

    protected MultiMap<Pair<Set<String>, Integer>, DBAttribute> processChild(
      ConstraintTreeNode.ChildrenIterator iterator, MultiMap<Pair<Set<String>, Integer>, DBAttribute> context)
    {
      FieldSubstringsConstraint constraint = iterator.getCurrentLeaf(FieldSubstringsConstraint.MATCHES_ALL);
      boolean all;
      if (constraint != null)
        all = true;
      else {
        constraint = iterator.getCurrentLeaf(FieldSubstringsConstraint.MATCHES_ANY);
        if (constraint == null)
          return context;
        all = false;
      }
      if (!myTextAttributes.contains(constraint.getAttribute()))
        return context;
      Set<String> strings = Collections15.hashSet(constraint.getSubstrings());
      if (strings.isEmpty()) {
        iterator.removeCurrent();
        return context;
      }
      if (strings.size() == 1)
        all = true;
      Integer intKey = all ? ALL_POSITIVE : ANY_POSITIVE;
      if (iterator.getCurrent().isNegated())
        intKey *= -1;
      Pair<Set<String>, Integer> key = Pair.create(strings, intKey);
      if (context == null)
        context = MultiMap.create();
      context.add(key, constraint.getAttribute());
      iterator.removeCurrent();
      return context;
    }

    protected ConstraintTreeElement applyResult(ConstraintTreeNode group,
      @NotNull MultiMap<Pair<Set<String>, Integer>, DBAttribute> context)
    {
      for (Pair<Set<String>, Integer> pair : context.keySet()) {
        Integer intKey = pair.getSecond();
        TypedKey<TextSubstringsConstraint> type;
        boolean negated = intKey < 0;
        int typeKey = Math.abs(intKey);
        if (typeKey == ALL_POSITIVE)
          type = MATCHES_ALL;
        else if (typeKey == ANY_POSITIVE)
          type = MATCHES_ANY;
        else {
          assert false : intKey;
          return null;
        }
        ConstraintTreeLeaf newLeaf =
          group.addLeaf(new TextSubstringsConstraint(context.getAll(pair), pair.getFirst(), type));
        if (negated)
          newLeaf.negate();
      }
      return group;
    }
  }

  public static class ConvertOneField implements Rule {
    private final Set<DBAttribute<?>> myAttributes;

    public ConvertOneField(Collection<? extends DBAttribute<?>> attributes) {
      myAttributes = Collections15.unmodifiableSetCopy(attributes);
    }

    @Nullable
    public ConstraintTreeElement process(ConstraintTreeElement element) {
      ConstraintTreeLeaf substr = ConstraintTreeNode.castLeaf(FieldSubstringsConstraint.MATCHES_ALL, element);
      boolean all;
      if (substr != null)
        all = true;
      else {
        substr = ConstraintTreeNode.castLeaf(FieldSubstringsConstraint.MATCHES_ANY, element);
        if (substr == null)
          return null;
        all = false;
      }
      FieldSubstringsConstraint constraint = (FieldSubstringsConstraint) substr.getConstraint();
      DBAttribute attribute = constraint.getAttribute();
      if (!myAttributes.contains(attribute))
        return null;
      List<String> strings = constraint.getSubstrings();
      if (strings == null || strings.isEmpty())
        return null;
      if (strings.size() == 1)
        all = true;
      ConstraintTreeLeaf result = new ConstraintTreeLeaf(
        new TextSubstringsConstraint(Collections.singleton(attribute), strings, all ? MATCHES_ALL : MATCHES_ANY));
      if (element.isNegated())
        result.negate();
      return result;
    }
  }
}
