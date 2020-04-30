package com.almworks.api.constraint;

import com.almworks.api.reduction.*;
import com.almworks.items.api.DBAttribute;
import com.almworks.util.Pair;
import com.almworks.util.collections.Comparing;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author dyoma
 */
public interface DateConstraint extends OneFieldConstraint {
  TypedKey<DateConstraint> BEFORE = TypedKey.create("before");
  TypedKey<DateConstraint> AFTER = TypedKey.create("after");

  TypedKey<? extends DateConstraint> getType();

  Date getDate();

  class Simple implements DateConstraint {
    private final Date myDate;
    private final TypedKey<? extends DateConstraint> myType;
    private final DBAttribute myAttribute;

    public Simple(Date date, TypedKey<? extends DateConstraint> type, DBAttribute attribute) {
      myDate = date;
      myType = type;
      myAttribute = attribute;
    }

    public TypedKey<? extends DateConstraint> getType() {
      return myType;
    }

    public DBAttribute getAttribute() {
      return myAttribute;
    }

    public Date getDate() {
      return myDate;
    }

    @NotNull
    public static DateConstraint before(Date date, DBAttribute attribute) {
      return new Simple(date, BEFORE, attribute);
    }

    @NotNull
    public static DateConstraint after(Date date, DBAttribute attribute) {
      return new Simple(date, AFTER, attribute);
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof DateConstraint))
        return false;
      DateConstraint other = (DateConstraint) obj;
      return other.getType() == myType && Util.equals(other.getAttribute(), myAttribute) &&
        Util.equals(other.getDate(), myDate);
    }

    public int hashCode() {
      return Comparing.hashCode(myType) ^ Comparing.hashCode(myAttribute) ^ Comparing.hashCode(myDate);
    }

    public String toString() {
      String typeString;
      if (myType == AFTER)
        typeString = "after";
      else if (myType == BEFORE)
        typeString = "before";
      else
        typeString = "Unknwon<" + String.valueOf(myType) + ">";
      return "Date[" + String.valueOf(myAttribute) + " " + typeString + ": " + String.valueOf(myDate) + "]";
    }
  }


  Rule CONVERT_TO_POSITIVE = new Rule() {
    @Nullable
    public ConstraintTreeElement process(ConstraintTreeElement element) {
      if (!element.isNegated())
        return null;
      if (!(element instanceof ConstraintTreeLeaf))
        return null;
      ConstraintTreeLeaf leaf = (ConstraintTreeLeaf) element;
      DateConstraint after = Constraints.cast(AFTER, leaf.getConstraint());
      DateConstraint before = Constraints.cast(BEFORE, leaf.getConstraint());
      assert after == null || before == null;
      if (after == null && before == null)
        return null;
      boolean isLow = after != null;
      DateConstraint constraint = isLow ? after : before;
      Calendar c = Calendar.getInstance();
      c.setTime(constraint.getDate());
      c.add(Calendar.DAY_OF_YEAR, isLow ? -1 : 1);
      return new ConstraintTreeLeaf(new Simple(c.getTime(), isLow ? BEFORE : AFTER, constraint.getAttribute()));
    }
  };

  Rule UNITE_RANGES = new CollectRangesRule(true);
  Rule INTERSECT_RANGES = new CollectRangesRule(false);
  List<Rule> ALL_RULES = Arrays.asList(CONVERT_TO_POSITIVE, UNITE_RANGES, INTERSECT_RANGES);


  public static class CollectRangesRule extends BaseCollectRule<Map<DBAttribute, Pair<Date, Date>>> {
    private final boolean myUnite;

    public CollectRangesRule(boolean unite) {
      super(unite ? CompositeConstraint.OR : CompositeConstraint.AND);
      myUnite = unite;
    }

    protected ConstraintTreeElement applyResult(ConstraintTreeNode group,
      @NotNull Map<DBAttribute, Pair<Date, Date>> context)
    {
      for (Map.Entry<DBAttribute, Pair<Date, Date>> entry : context.entrySet()) {
        Pair<Date, Date> pair = entry.getValue();
        Date after = pair.getFirst();
        Date before = pair.getSecond();
        if (after == null && before == null)
          continue;
        if (before != null && after != null && !before.equals(after))  
          if ((myUnite && !before.before(after)) || (!myUnite && !after.before(before)))
            return ConstraintTreeLeaf.createTrue(!myUnite);
        DBAttribute attribute = entry.getKey();
        if (before != null)
          group.addChild(new ConstraintTreeLeaf(Simple.before(before, attribute)));
        if (after != null)
          group.addChild(new ConstraintTreeLeaf(Simple.after(after, attribute)));
      }
      return null;
    }

    protected Map<DBAttribute, Pair<Date, Date>> processChild(ConstraintTreeNode.ChildrenIterator iterator,
      Map<DBAttribute, Pair<Date, Date>> context)
    {
      DateConstraint after = iterator.getCurrentLeaf(AFTER);
      DateConstraint before = iterator.getCurrentLeaf(BEFORE);
      if (after == null && before == null)
        return context;
      boolean isAfter = after != null;
      DateConstraint constraint = isAfter ? after : before;
      Pair<Date, Date> prev = context != null ? context.get(constraint.getAttribute()) : null;
      Date prevBound;
      if (prev == null)
        prevBound = null;
      else
        prevBound = isAfter ? prev.getFirst() : prev.getSecond();
      if (prev == null)
        prev = Pair.nullNull();
      iterator.removeCurrent();
      Date nextBound;
      Date date = constraint.getDate();
      if (prevBound == null)
        nextBound = date;
      else {
        boolean toPast = myUnite ? isAfter : !isAfter;
        if (toPast)
          nextBound = date.before(prevBound) ? date : prevBound;
        else
          nextBound = prevBound.before(date) ? date : prevBound;
      }
      Pair<Date, Date> next = isAfter ? prev.copyWithFirst(nextBound) : prev.copyWithSecond(nextBound);
      if (context == null)
        context = Collections15.hashMap();
      context.put(constraint.getAttribute(), next);
      return context;
    }
  }
}