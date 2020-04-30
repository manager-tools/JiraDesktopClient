package com.almworks.items.impl.sqlite;

import com.almworks.integers.*;
import com.almworks.items.impl.dbadapter.GroupsDist;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.commons.IntIntFunction;
import com.almworks.util.commons.IntIntFunction2;
import com.almworks.util.commons.IntProcedure2;
import org.almworks.util.Util;

import static com.almworks.items.impl.sqlite.IntCollectionFactory.createLongList;

class ItemTreeBuilder implements IntIntFunction {
  private static final long INVALID_ITEM = Long.MAX_VALUE;

  /**
   * Source data
   */
  private LongList mySourceItems;
  private LongList mySourceGraph;
  private GroupsDist myGroups;

  /**
   * Contains map from item ids to their parents. All items participating in the tree,
   * including roots. Roots are mapped onto 0.
   */
  private LongParallelListMap myItemParents;

  /**
   * contains index of item[i] in the source (sorted) array
   */
  private WritableIntList mySourceIndexes;

  /**
   * contains diff-indexes of permutation of items in order of (parentItem, sourceIndex)
   * that is items[itemsReindexed[i]] is the item with least parent and least source index
   * <p/>
   * a diff-index is a difference to be applied to index to receive new index, that is
   * <code>I' = myItemsReindexed[I] + I</code>
   */
  private WritableIntList myItemsReindexed;

  /**
   * Contains depths of items
   */
  private WritableIntList myResultDepths;

  /**
   * Contains Rein
   */
  private WritableIntList myResultTreeIndexes;

  /**
   * used for searching
   */
  private transient long mySoughtParent;

  public ItemTreeBuilder(LongList sourceItems, LongList sourceGraph) {
    mySourceItems = sourceItems;
    mySourceGraph = sourceGraph;
  }

  public static LongList restrictGraph(LongList restrictItems, LongList graph) {
    if(restrictItems.isEmpty()) return graph;

    LongArray restrictItemsSet = new LongArray();
    restrictItemsSet.addAll(restrictItems);
    restrictItemsSet.sortUnique();

    WritableLongList restrictedGraph = createLongList();
    int start = findGraphIndex(restrictItems.get(0), graph);
    if(start < 0) start  = -start -1;
    start <<= 1;
    for (int i = start; i < graph.size(); ) {
      long from = graph.get(i++);
      assert i < graph.size();
      long to = graph.get(i++);
      if (restrictItemsSet.binarySearch(from) >= 0 && restrictItemsSet.binarySearch(to) >= 0) {
        restrictedGraph.add(from);
        restrictedGraph.add(to);
      }
    }
    return restrictedGraph;
  }

  public int invoke(int rindex) {
    int idx = myItemsReindexed.get(rindex) + rindex;
    return Util.compareLongs(myItemParents.getValue(idx), mySoughtParent);
  }

  private void addChildren(WritableIntList treeIndexes, long parent, int depth, WritableIntList depths) {
    mySoughtParent = parent;
    int count = myItemParents.size();
    int r = CollectionUtil.binarySearch(count, this);
    mySoughtParent = 0;
    if (r >= 0) {
      if (r > 0 && myItemParents.getValue(myItemsReindexed.get(r - 1) + r - 1) == parent) {
        // search back for the beginning of value, similar to binary search
        // see {@link AbstractIntList.binarySearch}
        int higheq = r - 1;
        int loweq = 0;
        while (higheq > loweq) {
          int p = (higheq + loweq) >> 1;
          long v = myItemParents.getValue(myItemsReindexed.get(p) + p);
          if (v == parent) {
            higheq = p;
          } else {
            assert v < parent : v + " " + parent;
            loweq = p + 1;
          }
        }
        r = higheq;
      }

      // todo convert recursion to cycle
      for (IntIterator ii = myItemsReindexed.iterator(r); r < count; r++) {
        assert ii.hasNext() : r;
        int idx = ii.nextValue() + r;
        if (myItemParents.getValue(idx) == parent) {
          long item = myItemParents.getKey(idx);
          treeIndexes.add(mySourceIndexes.get(idx) - treeIndexes.size());
          depths.add(depth);
          addChildren(treeIndexes, item, depth + 1, depths);
        } else {
          break;
        }
      }
    }
  }

  /**
   * Makes myItemParents a tree by marking invalid nodes
   */
  private void reduceToTree() {
    LongArray path = new LongArray((int) Math.sqrt(myItemParents.size()));
    int N = mySourceGraph.size() >> 1;
    for (int gi = 0; gi < N; gi++) {
      if (detectCycleOrMultipleParents(gi, mySourceGraph, path)) {
        markInvalid(gi, mySourceGraph);
      }
    }
  }


  private void markInvalid(int index, LongList itemGraph) {
    long to = itemGraph.get((index << 1) + 1);
    // check if the target is already marked
    int mi = myItemParents.findKey(to);
    if (mi < 0) {
      assert false;
    } else {
      long p = myItemParents.getValue(mi);
      if (p == INVALID_ITEM)
        return;
      myItemParents.setValue(mi, INVALID_ITEM);
      int cs = findGraphIndex(to, itemGraph);
      if (cs >= 0) {
        for (int c = cs; c < (itemGraph.size() >> 1); c++) {
          long nextFrom = itemGraph.get(c << 1);
          if (nextFrom != to)
            break;
          markInvalid(c, itemGraph);
        }
      }
    }
  }

  // todo remake to effective non-recursive marking
  private boolean detectCycleOrMultipleParents(int index, LongList itemGraph, LongArray path) {
    int abs = index << 1;
    long from = itemGraph.get(abs);
    long to = itemGraph.get(abs + 1);

    // check if the target is already marked
    int mi = myItemParents.findKey(to);
    if (mi < 0) {
      assert false : mi;
      return true;
    }
    long parent = myItemParents.getValue(mi);
    if (parent == INVALID_ITEM) {
      return true;
    }

    // detect cycles
    if (to == from || path.binarySearch(to) >= 0) {
      return true;
    }

    path.addSorted(from);
    try {
      int cs = findGraphIndex(to, itemGraph);
      if (cs >= 0) {
        for (int c = cs; c < (itemGraph.size() >> 1); c++) {
          long nextFrom = itemGraph.get(c << 1);
          if (nextFrom != to)
            break;
          if (detectCycleOrMultipleParents(c, itemGraph, path))
            return true;
        }
      }
    } finally {
      assert path.binarySearch(from) >= 0 : from + " " + path;
      path.remove(from);
    }

    return false;
  }

  private static int findGraphIndex(final long item, final LongList itemGraph) {
    // todo garbage
    int r = CollectionUtil.binarySearch(itemGraph.size() >> 1, new IntIntFunction() {
      public int invoke(int index) {
        return Util.compareLongs(itemGraph.get(index << 1), item);
      }
    });
    // todo O(N)
    while (r > 0 && itemGraph.get((r - 1) << 1) == item) {
      r--;
    }
    return r;
  }

  private void removeInvalid() {
    for (LongParallelListMap.Iterator ii = myItemParents.iterator(); ii.hasNext(); ) {
      ii.next();
      // todo collect ranges of invalid items and remove with one operation
      if (ii.right() == INVALID_ITEM) {
        ii.remove();
      }
    }
  }

  /**
   * Finds indexes for elements of the tree in the source collection
   */
  public void assignIndexes() {
    assert mySourceIndexes == null;
    int count = myItemParents.size();
    mySourceIndexes = IntCollectionFactory.createList(count);
    mySourceIndexes.expand(0, count);
    int found = 0;
    LongIterator ii = mySourceItems.iterator();
    for (int i = 0; ii.hasNext() && found < count; i++) {
      long item = ii.nextValue();
      int idx = myItemParents.findKey(item);
      if (idx >= 0) {
        mySourceIndexes.set(idx, i);
        found++;
      }
    }
    assert found == count : found + " " + count;
  }

  private void reindex() {
    assert myItemsReindexed == null;
    int count = myItemParents.size();
    myItemsReindexed = IntCollectionFactory.createSameValuesList(count);
    myItemsReindexed.insertMultiple(0, 0, count);

    // todo redo CollectionUtil.quicksort to use named interfaces for compare and swap
    // so we can mixin them into this class
    IntIntFunction2 compare = new IntIntFunction2() {
      public int invoke(int i1, int i2) {
        int ii1 = myItemsReindexed.get(i1) + i1;
        int ii2 = myItemsReindexed.get(i2) + i2;
        int diff = Util.compareLongs(myItemParents.getValue(ii1), myItemParents.getValue(ii2));
        if (diff != 0)
          return diff;
        return Util.compareInts(mySourceIndexes.get(ii1), mySourceIndexes.get(ii2));
      }
    };
    IntProcedure2 swap = new IntProcedure2() {
      public void invoke(int i1, int i2) {
        if (i1 != i2) {
          int v1 = myItemsReindexed.get(i1) + i1;
          int v2 = myItemsReindexed.get(i2) + i2;
          myItemsReindexed.set(i1, v2 - i1);
          myItemsReindexed.set(i2, v1 - i2);
        }
      }
    };
    CollectionUtil.quicksort(count, compare, swap);
  }

  /**
   * This method assumes items is disposable and has super-fast method remove(0),
   * following SegmentedIntArray implementation.
   */
  private void buildFinalTree() {
    int sz = mySourceItems.size();
    WritableIntList treeIndexes = IntCollectionFactory.createSameValuesList(sz);
    WritableIntList depths = IntCollectionFactory.createSameValuesList(sz);
    int zeroDepthCount = 0;
    int sourceIndex = 0;
    for (LongListIterator ii = mySourceItems.iterator(); ii.hasNext(); sourceIndex++) {
      long item = ii.nextValue();
      int tidx = myItemParents.findKey(item);
      if (tidx < 0) {
        zeroDepthCount++;
      } else {
        if (zeroDepthCount > 0) {
          int p = treeIndexes.size();
          treeIndexes.insertMultiple(p, sourceIndex - zeroDepthCount - p, zeroDepthCount);
          depths.insertMultiple(depths.size(), 0, zeroDepthCount);
          zeroDepthCount = 0;
        }
        if (myItemParents.getValue(tidx) == 0) {
          // root
          treeIndexes.add(sourceIndex - treeIndexes.size());
          depths.add(0);
          addChildren(treeIndexes, item, 1, depths);
        } else {
          // ignore - the item will be added later
        }
      }
    }
    if (zeroDepthCount > 0) {
      int p = treeIndexes.size();
      treeIndexes.insertMultiple(p, sourceIndex - zeroDepthCount - p, zeroDepthCount);
      depths.insertMultiple(depths.size(), 0, zeroDepthCount);
    }
    myResultTreeIndexes = treeIndexes;
    myResultDepths = depths;
    assert myResultTreeIndexes.size() == sz : sz + " " + myResultTreeIndexes.size();
    assert myResultDepths.size() == sz : sz + " " + myResultDepths.size();
  }

  /**
   * Fills in myItemParents
   */
  private void putEdges() {
    assert myItemParents == null;
    myItemParents = IntCollectionFactory.createListLongMap(mySourceGraph.size());
    for (LongIterator ii = mySourceGraph.iterator(); ii.hasNext();) {
      long from = ii.nextValue();
      assert ii.hasNext();
      long to = ii.nextValue();
      int idx = myItemParents.findKey(from);
      if (idx < 0) {
        myItemParents.insertAt(-idx - 1, from, 0);
      }
      int idx1 = myItemParents.findKey(to);
      if (idx1 < 0) {
        myItemParents.insertAt(-idx1 - 1, to, from);
      } else {
        long p = myItemParents.getValue(idx1);
        if (p == 0) {
          myItemParents.setValue(idx1, from);
        } else if (p != from && p != INVALID_ITEM) {
          myItemParents.setValue(idx1, INVALID_ITEM);
        }
      }
    }
  }

  public void build() {
    assert mySourceGraph != null;
    assert mySourceItems != null;

    putEdges();
    reduceToTree();
    removeInvalid();
    assignIndexes();
    reindex();
    buildFinalTree();

    dispose();
  }

  private void dispose() {
    IntCollectionFactory.dispose(myItemParents);
    myItemParents = null;
    IntCollectionFactory.dispose(myItemsReindexed);
    myItemsReindexed = null;
    IntCollectionFactory.dispose(mySourceIndexes);
    mySourceIndexes = null;
  }

  public WritableIntList getResultDepths() {
    assert myResultDepths != null;
    return myResultDepths;
  }


  public IntList getResultTreeIndexes() {
    assert myResultTreeIndexes != null;
    return myResultTreeIndexes;
  }

  // todo unit test
  static LongList updateGraph(LongList graph, LongList graphUpdate, LongList removedSorted, LongList insertedSorted,
    LongList updatedSorted)
  {
    WritableLongList result = IntCollectionFactory.createLongList(graph.size());

    LongListIterator sgii = graph.iterator();
    long nextsgs = sgii.hasNext() ? sgii.nextValue() : -1;
    long nextsgt = sgii.hasNext() ? sgii.nextValue() : -1;
    assert (nextsgs < 0) == (nextsgt < 0) : nextsgs + " " + nextsgt + " " + graph + " " + sgii;

    LongListIterator ugii = graphUpdate.iterator();
    long nextugs = ugii.hasNext() ? ugii.nextValue() : -1;
    long nextugt = ugii.hasNext() ? ugii.nextValue() : -1;
    assert (nextugs < 0) == (nextugt < 0) : nextugs + " " + nextugt + " " + graphUpdate + " " + ugii;

    while (nextsgs >= 0 || nextugs >= 0) {
      while (nextsgs >= 0 && (nextsgs <= nextugs || nextugs < 0)) {
        boolean ignore = nextsgs == nextugs || contained(nextsgs, removedSorted, insertedSorted, updatedSorted);
        // ignore == true if source is removed or updated -- get from updatedGraph
        long current = nextsgs;
        while (nextsgs == current) {
          if (!ignore && !contained(nextsgt, removedSorted, insertedSorted, updatedSorted)) {
            // assert source in not removed or updated
            // assert target is not removed or updated
            result.add(nextsgs);
            result.add(nextsgt);
          }
          nextsgs = sgii.hasNext() ? sgii.nextValue() : -1;
          nextsgt = sgii.hasNext() ? sgii.nextValue() : -1;
          assert (nextsgs < 0) == (nextsgt < 0) : nextsgs + " " + nextsgt + " " + graph + " " + sgii;
        }
      }
      while (nextugs >= 0 && (nextugs < nextsgs || nextsgs < 0)) {
        result.add(nextugs);
        result.add(nextugt);
        nextugs = ugii.hasNext() ? ugii.nextValue() : -1;
        nextugt = ugii.hasNext() ? ugii.nextValue() : -1;
        assert (nextugs < 0) == (nextugt < 0) : nextugs + " " + nextugt + " " + graphUpdate + " " + ugii;
      }
    }
    return result;
  }

  static boolean contained(long item, LongList removedSorted, LongList insertedSorted, LongList updatedSorted) {
    return removedSorted.binarySearch(item) >= 0 || insertedSorted.binarySearch(item) >= 0 || updatedSorted.binarySearch(item) >= 0;
  }
}
