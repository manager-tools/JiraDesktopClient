package com.almworks.explorer.tree;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.ItemsPreview;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.integers.IntArray;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBReader;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DistributionPreviewComputation {
  private final BaseEnumConstraintDescriptor myDescriptor;
  private final DBFilter myFilter;
  private final List<DistributionQueryNodeImpl> myChildren;
  private final List<Object> myResolved = Collections15.arrayList();

  public DistributionPreviewComputation(BaseEnumConstraintDescriptor descriptor, DBFilter filter,
    List<DistributionQueryNodeImpl> childQueries) {
    LogHelper.assertError(filter != null);
    myDescriptor = descriptor;
    myFilter = filter;
    myChildren = childQueries;
  }

  public void perform(Lifespan lifespan, DBReader reader, ItemHypercube cube) {
    if (lifespan.isEnded()) return;
    if (myFilter == null) return;
    collectQueriedItems(cube);
    CountCollector count = CountCollector.create(myResolved);
    if (count == null) {
//      for (int i = 0, myChildrenSize = myChildren.size(); i < myChildrenSize; i++) {
//        DistributionQueryNodeImpl child = myChildren.get(i);
//        child.setPreview(new ItemsPreview.Unavailable());
//      }
      return;
    }
    long start = System.currentTimeMillis();
    LongArray items = myFilter.query(reader).copyItemsSorted();
    long duration = System.currentTimeMillis() - start;
    Log.debug("Distribution count: " + duration + "ms/" + items.size() + "count " + myChildren.size() + "childCount " + myFilter.getExpr());
    DBAttribute attribute = myDescriptor.getAttribute();
    if (!Long.class.equals(attribute.getScalarClass())) {
      Log.error("Wrong attribute " + attribute);
      setNoPreview();
      return;
    }
    DBAttribute<Long> parentAttribute = myDescriptor.getParentAttribute();
    switch (attribute.getComposition()) {
    case SCALAR: readScalarValues(count, reader, attribute, parentAttribute, items); break;
    case SET:
    case LIST: readCollectionValues(count, reader, attribute, items); break;
    default:
      Log.error("Wrong attribute " + attribute);
      setNoPreview();
      return;
    }
    ThreadGate.AWT.execute(new SetPreviewRunnable(count));
  }

  private void readCollectionValues(CountCollector count, DBReader reader, DBAttribute<? extends Collection<Long>> attribute, LongArray items) {
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      Collection<Long> values = attribute.getValue(item, reader);
      if (values == null) count.incNullCount();
      else for (Long value : values) count.incValueCount(value);
    }
  }

  private void readScalarValues(CountCollector count, DBReader reader, DBAttribute<Long> attribute, DBAttribute<Long> parentAttribute, LongArray items) {
    for(int i = 0; i < items.size(); i++) {
      final long item = items.get(i);
      Long value = attribute.getValue(item, reader);
      if (value == null) count.incNullCount();
      while(value != null && value > 0L) {
        count.incValueCount(value);
        value = parentAttribute != null ? parentAttribute.getValue(value, reader) : null;
      }
    }
  }

  private void setNoPreview() {
    Log.warn("Setting no preview " + myDescriptor + " " + myChildren.size());
    for (DistributionQueryNodeImpl child : myChildren) child.setPreview(new ItemsPreview.Unavailable());
  }

  private void collectQueriedItems(ItemHypercube cube) {
    for (DistributionQueryNodeImpl child : myChildren) {
      ItemsPreview preview = child.getPreview(false);
      Object resolvedItems;
      if (preview == null || !preview.isValid()) {
        Pair<ConstraintDescriptor, ItemKey> pair = child.getAttributeValue();
        if (pair == null || !myDescriptor.equals(pair.getFirst())) {
          LogHelper.assertWarning(pair == null, "Unexpected descriptor " + pair + " " + myDescriptor);
          child.setPreview(new ItemsPreview.Unavailable());
          resolvedItems = null;
        } else {
          Object items = child.getItems(cube);
          if (items instanceof List) resolvedItems = ((List) items).isEmpty() ? null : items;
          else if (items instanceof ResolvedItem) resolvedItems = items;
          else if (items instanceof ItemKey) resolvedItems = myDescriptor.isNullKey((ItemKey) items) ? Collections.emptyList() : null;
          else {
            LogHelper.assertError(items == null, items);
            resolvedItems = null;
          }
          if (resolvedItems == null) child.setPreview(new CountPreview(0));
        }
      } else resolvedItems = null;
      myResolved.add(resolvedItems);
    }
    assert myChildren.size() == myResolved.size();
  }

  private class SetPreviewRunnable implements Runnable {
    private GenericNode myCurrentParent = null;
    private int myLastIndex = 0;
    private int myParentChildCount = 0;
    private final IntArray myChangedIndexes = new IntArray();
    private final CountCollector myCount;

    public SetPreviewRunnable(CountCollector count) {
      myCount = count;
    }

    @Override
    public void run() {
      Threads.assertAWTThread();
      for (int i = 0, myChildrenSize = myChildren.size(); i < myChildrenSize; i++) {
        DistributionQueryNodeImpl child = myChildren.get(i);
        Object items = myResolved.get(i);
        setCurrentParent(child.getParent());
        if (items != null) {
          int count = myCount.getCount(items);
          child.setPreviewSilent(new CountPreview(count));
          markChildChanged(child);
        }
        myLastIndex++;
      }
      setCurrentParent(null);
    }

    private void markChildChanged(DistributionQueryNodeImpl child) {
      GenericNode parent = child.getParent();
      if (parent == null) return;
      if (parent != myCurrentParent) {
        child.fireTreeNodeChanged();
        return;
      }
      int index;
      if (myLastIndex < myParentChildCount) {
        GenericNode expected = parent.getChildAt(myLastIndex);
        index = expected == child ? myLastIndex : -1;
      } else index = -1;
      if (index < 0) {
        index = parent.getTreeNode().getIndex(child.getTreeNode());
        if (index >= 0) myLastIndex = index;
      }
      myChangedIndexes.add(index);
    }

    private void setCurrentParent(GenericNode parent) {
      if (myCurrentParent == parent) return;
      if (myCurrentParent != null) {
        myCurrentParent.getTreeNode().fireChildrenChanged(myChangedIndexes);
        myCurrentParent.fireTreeNodeChanged();
      }
      myChangedIndexes.clear();
      myCurrentParent = parent;
      myLastIndex = 0;
      myParentChildCount = parent != null ? parent.getChildrenCount() : 0;
    }
  }

  private static class CountCollector {
    private final LongList myAllValues;
    private final boolean myCountNulls;
    private final IntArray myCount = new IntArray();
    private int myNullCount = 0;

    public CountCollector(LongList values, boolean countNull) {
      myAllValues = values;
      myCountNulls = countNull;
      myCount.insertMultiple(0, 0, myAllValues.size());
    }

    private int getCount(long item) {
      int index = myAllValues.indexOf(item);
      return myCount.get(index);
    }

    public int getCount(Object items) {
      if (items instanceof List) {
        List<ItemKey> list = (List<ItemKey>) items;
        if (list.isEmpty()) return myNullCount;
        int count = 0;
        for (ItemKey key : list) {
          long item = key.getResolvedItem();
          if (item <= 0) LogHelper.error("Expected resolved", key, item);
          count += getCount(item);
        }
        return count;
      } else if (items instanceof ResolvedItem) {
        return getCount(((ResolvedItem) items).getResolvedItem());
      } else {
        LogHelper.assertError(items == null, items);
        return 0;
      }
    }

    public void incValueCount(Long value) {
      if (value == null || value <= 0) return;
      int index = myAllValues.indexOf(value);
      if (index >= 0) myCount.set(index, myCount.get(index) + 1);
    }

    public void incNullCount() {
      if (myCountNulls) myNullCount++;
    }

    @Nullable
    public static CountCollector create(List<Object> task) {
      if (task == null || task.isEmpty()) return null;
      boolean countNull = false;
      LongSetBuilder values = new LongSetBuilder();
      for (Object itemOrList : task) {
        if (itemOrList == null) continue;
        if (itemOrList instanceof List) {
          List list = (List) itemOrList;
          if (list.isEmpty()) countNull = true;
          else for (Object item : list) addItem(values, item);
        } else addItem(values, itemOrList);
      }
      return values.isEmpty() && !countNull ? null : new CountCollector(values.commitToArray(), countNull);
    }

    private static void addItem(LongSetBuilder target, Object item) {
      ResolvedItem resolved = Util.castNullable(ResolvedItem.class, item);
      long value = resolved != null ? resolved.getResolvedItem() : 0;
      if (value <= 0) LogHelper.error("Expected resolved", item, value);
      else target.add(value);
    }
  }
}
