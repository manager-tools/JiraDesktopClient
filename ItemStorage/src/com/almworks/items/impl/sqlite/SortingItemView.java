package com.almworks.items.impl.sqlite;

import com.almworks.integers.*;
import com.almworks.integers.segmented.LongSegmentedArray;
import com.almworks.items.impl.dbadapter.AbstractGroupsDist;
import com.almworks.items.impl.dbadapter.GroupsDistInsertingDecorator;
import com.almworks.items.impl.dbadapter.GroupsDistRemovingDecorator;
import com.almworks.items.impl.dbadapter.ItemView;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.commons.IntIntFunction2;
import com.almworks.util.commons.IntProcedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.AWTRequired;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.threads.Threads;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static com.almworks.items.impl.sqlite.IntCollectionFactory.createLongList;

/**
 * Base SortingItemView is not dependent on the database events.
 */
public class SortingItemView implements ItemView {
  // locked with myLifecycle:
  private final Lifecycle myLifecycle = new Lifecycle(false);
  private final PrioritizedListeners<Listener> myListeners = new PrioritizedListeners<Listener>();
  private final AtomicInteger myPriority = new AtomicInteger(Integer.MIN_VALUE);

  /**
   * Current item view, as seen from AWT thread.
   */
  @NotNull @AWTRequired
  private ItemViewFrame myFrameAWT;

  @AWTRequired
  private boolean myLoaded;

  /**
   * Current item view, as seen from DB thread. When there are no pending events, it's equal to myStateAWT.
   */
  @NotNull
  private ItemViewFrame myFrameDB;

  public SortingItemView() {
    myFrameAWT = myFrameDB = new ItemViewFrame();
  }

  @AWTRequired
  public LongList getItems() {
    return myFrameAWT.getItems();
  }

  @AWTRequired
  @Nullable
  public IntList getDepths() {
    return myFrameAWT.getDepths();
  }

  public AbstractGroupsDist getGroups() {
    return myFrameAWT.getGroups();
  }

  @AWTRequired
  public boolean isLoaded() {
    synchronized (myLifecycle) {
      return myLoaded;
    }
  }

  public void addListener(Lifespan life, int priority, final Listener listener) {
    if (life.isEnded())
      return;
    boolean started;
    Lifespan lifespan;
    synchronized (myLifecycle) {
      if (myListeners.isEmpty()) {
        // clear myLoaded before first client attaches
        myLoaded = false;
      }
      myListeners.add(listener, priority);
      started = myLifecycle.cycleStart();
      lifespan = myLifecycle.lifespan();
    }
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        boolean hasElements = myListeners.remove(listener);
        if (!hasElements) {
          boolean ended = myLifecycle.cycleEnd();
          if (ended) {
            lifeEnded();
            synchronized (myLifecycle) {
              if (!myLifecycle.isCycleStarted()) {
                // clear myLoaded when last client detaches
                // lifecycle is double-checked because a new lifespan may have starte since cycleEnd()
                myLoaded = false;
              }
            }
          }
        }
        updatePriority();
      }
    });
    if (started) {
      myPriority.set(myListeners.getTotalPriority());
      lifeStarted(lifespan);
    }
    updatePriority();
  }

  @ThreadSafe
  public void setPriority(int priority, Listener listener) {
    myListeners.setPriority(priority, listener);
    updatePriority();
  }

  private void updatePriority() {
    int priority = myListeners.getTotalPriority();
    int oldValue = myPriority.getAndSet(priority);
    if (oldValue != priority) {
      priorityChanged(priority);
    }
  }

  protected void priorityChanged(int priority) {
  }

  protected int getPriority() {
    return myPriority.get();
  }

  @ThreadSafe
  protected void lifeStarted(Lifespan life) {
  }

  @ThreadSafe
  protected void lifeEnded() {
  }

  protected boolean hasListeners() {
    return !myListeners.isEmpty();
  }

  protected void setStateDB(final ItemViewFrame frame) {
    myFrameDB = frame;
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        setStateAWT(frame);
      }
    });
  }

  @AWTRequired
  private void setStateAWT(ItemViewFrame newFrame) {
    Threads.assertAWTThread();
    assert newFrame.myEvent != null : this + " " + newFrame;
    assert newFrame.myState == null : this + " " + newFrame;
    myLoaded = true;
    ViewChange event = newFrame.myEvent;
    myFrameAWT = newFrame;
    for (Listener listener : myListeners) {
      listener.onUpdate(event);
    }
    newFrame.flatten();
  }

  protected void reload(LongList items, @Nullable LongList itemGraph, @Nullable AbstractGroupsDist groups) {
    IntList depths = null;
    LongListDiffIndexedDecorator itemTree = null;
    if (itemGraph != null) {
      if (itemGraph.isEmpty()) {
        IntSameValuesList zeroes = new IntSameValuesList();
        zeroes.insertMultiple(0, 0, items.size());
        itemTree = new LongListDiffIndexedDecorator(items, zeroes);
        depths = zeroes;
      } else {
        TreeInfo treeInfo = new TreeInfo(items, itemGraph, groups).build();
        itemTree = treeInfo.getItemTree();
        depths = treeInfo.getDepths();
      }
    }
    setStateDB(
      new ItemViewFrame(new ReplaceEvent(myFrameDB, new ItemViewState(items, itemGraph, itemTree, depths, groups))));
  }

  protected void update(@NotNull LongList inserted, @NotNull LongList removed, @NotNull LongList updated,
    @Nullable Inserter inserter, @Nullable LongList graphUpdate, boolean replaceGraph) throws SQLiteException
  {
    LongList initial = myFrameDB.getItemsList();
    LongList current = initial;

    int removedCount = removed.size();
    int updatedCount = updated.size();
    int insertedCount = inserted.size();

    LongListRemovingDecorator removeDecorator = null;
    LongListInsertingDecorator updateDecorator = null;
    LongListInsertingDecorator insertDecorator = null;

    AbstractGroupsDist groupsUpdate = myFrameDB.getGroups();

    if (removedCount + updatedCount > 0) {
      WritableIntList indexesRU = IntCollectionFactory.createList();
      for (LongListIterator ii = current.iterator(); ii.hasNext();) {
        long item = ii.nextValue();
        if (removedCount > 0 && removed.binarySearch(item) >= 0) {
          indexesRU.add(ii.index());
        } else if (updatedCount > 0 && updated.binarySearch(item) >= 0) {
          indexesRU.add(ii.index());
        }
      }
      IntListRemovingDecorator.prepareSortedIndices(indexesRU);
      removeDecorator = LongListRemovingDecorator.createFromPrepared(current, indexesRU);
      current = removeDecorator;
      if (groupsUpdate != null) {
        groupsUpdate = GroupsDistRemovingDecorator.create(groupsUpdate, indexesRU);
      }
    }

    if (updatedCount > 0) {
      assert inserter != null;
      updateDecorator = new LongListInsertingDecorator(current);
      GroupsDistInsertingDecorator groupsUpdateDecorator =
        groupsUpdate != null ? new GroupsDistInsertingDecorator(groupsUpdate) : null;
      inserter.insert(updated, updateDecorator, groupsUpdateDecorator);
      current = updateDecorator;
      groupsUpdate = groupsUpdateDecorator;
    }

    if (insertedCount > 0) {
      assert inserter != null;
      insertDecorator = new LongListInsertingDecorator(current);
      GroupsDistInsertingDecorator groupsInsertDecorator =
        groupsUpdate != null ? new GroupsDistInsertingDecorator(groupsUpdate) : null;
      inserter.insert(inserted, insertDecorator, groupsInsertDecorator);
      current = insertDecorator;
      groupsUpdate = groupsInsertDecorator;
    }

    LongListDiffIndexedDecorator itemTree = null;
    WritableIntList depths = null;
    LongList itemGraph = myFrameDB.getItemsGraph();
    assert (itemGraph == null) == (graphUpdate == null) : itemGraph + " " + graphUpdate;
    if (itemGraph != null) {
      // todo we can update itemGraph instead of creating new graph, because itemGraph is confined to DB thread
      // and not needed for anything except this update

      // todo if graph is not updated, do not rebuild tree. that would require changing DiffIndexedIntListDecorator
      // to reflect inserts, removals and updates without changing tree strucutre: may be tricky, but worth doing
      LongList newGraph;
      if (replaceGraph) {
        newGraph = graphUpdate;
      } else {
        newGraph = ItemTreeBuilder.updateGraph(itemGraph, graphUpdate, removed, inserted, updated);
      }
      itemGraph = newGraph;
      TreeInfo treeInfo = new TreeInfo(current, itemGraph, groupsUpdate).build();
      itemTree = treeInfo.getItemTree();
      depths = treeInfo.getDepths();
    }

    UpdateEvent update =
      new UpdateEvent(initial, myFrameDB.getItems(), removed, removeDecorator, updateDecorator, insertDecorator,
        itemTree, depths, itemGraph, groupsUpdate);
    setStateDB(new ItemViewFrame(update));
  }

  protected interface Inserter {
    /**
     * Groups are both read (to insert the item in the correct place) and written (groupIDs for the inserted item).
     */
    void insert(LongList source, LongListInsertingDecorator target, @Nullable GroupsDistInsertingDecorator groups)
      throws SQLiteException;
  }


  private abstract static class ViewChange implements Event {
    private final LongList myPrevItemList;
    private final LongList myPrevItems;

    protected ViewChange(LongList prevItemList, LongList prevItems) {
      myPrevItemList = prevItemList;
      myPrevItems = prevItems;
    }

    public LongList getPrevItemList() {
      return myPrevItemList;
    }

    public LongList getPrevItems() {
      return myPrevItems;
    }

    public abstract LongList getItemList();

    public abstract IntList getDepths();

    public abstract LongList getItemGraph();

    @Nullable
    public abstract LongListDiffIndexedDecorator getItemTree();

    @Nullable
    public abstract AbstractGroupsDist getGroups();

    public LongList getItems() {
      LongListDiffIndexedDecorator tree = getItemTree();
      return tree != null ? tree : getItemList();
    }
  }


  protected static class ItemViewFrame {
    /**
     * When myState is not null, IVF is a "final" state that holds IDs of items. myEvent must be null.
     */
    @Nullable
    private ItemViewState myState;

    /**
     * When myEvent is not null, IVF is a "changing" state from the old IVS (referenced in myEvent). ?myState? must be null.
     */
    @Nullable
    private ViewChange myEvent;

    private ItemViewFrame() {
      myState = new ItemViewState(new LongArray());
    }

    private ItemViewFrame(ViewChange event) {
      myEvent = event;
    }

    @NotNull
    @ThreadSafe
    public synchronized LongList getItemsList() {
      if (myState != null) {
        return myState.getItemList();
      } else if (myEvent != null) {
        return myEvent.getItemList();
      } else {
        // must not be used when disposed through flatten() from the next IVS
        assert false : this;
        return LongList.EMPTY;
      }
    }

    @Nullable
    @ThreadSafe
    public synchronized IntList getDepths() {
      if (myState != null) {
        return myState.getDepths();
      } else if (myEvent != null) {
        return myEvent.getDepths();
      } else {
        assert false : this;
        return null;
      }
    }

    /**
     * This method removes applies myEvent changes to the parent collection, then forgets the event.
     */
    @ThreadSafe
    public synchronized void flatten() {
      // flatten should be called only for the "next" IVS after the top-level array element
      if (myEvent == null || myState != null) {
        assert false : this;
        return;
      }

      LongListDiffIndexedDecorator tree = myEvent.getItemTree();
      // todo effective flattening in-place
      LongList items = myEvent.getItemList();
      LongList newItems;
      if (items instanceof LongSegmentedArray) {
        newItems = items;
      } else {
        WritableLongList list = IntCollectionFactory.createLongList(items.size());
        list.addAll(items);
        newItems = list;

        // todo disposing used collections; blunt disposal leads to concurrent exceptions
//        IntCollectionFactory.dispose(items);
        if (tree != null) {
          tree = new LongListDiffIndexedDecorator(newItems, tree.getIndexes());
        }
      }
      AbstractGroupsDist groups = myEvent.getGroups();
      if (groups != null) {
        groups = groups.undecorate();
      }
      myState = new ItemViewState(newItems, myEvent.getItemGraph(), tree, myEvent.getDepths(), groups);
      myEvent = null;
    }

    public synchronized LongList getItemsGraph() {
      if (myState != null) {
        return myState.getItemGraph();
      } else if (myEvent != null) {
        return myEvent.getItemGraph();
      } else {
        assert false : this;
        return null;
      }
    }

    public synchronized LongList getItems() {
      if (myState != null) {
        return myState.getItems();
      } else if (myEvent != null) {
        return myEvent.getItems();
      } else {
        // must not be used when disposed through flatten() from the next IVS
        assert false : this;
        return LongList.EMPTY;
      }
    }

    @Nullable
    @ThreadSafe
    public synchronized AbstractGroupsDist getGroups() {
      if (myState != null) {
        return myState.getGroups();
      } else if (myEvent != null) {
        return myEvent.getGroups();
      } else {
        assert false : this;
        return null;
      }
    }
  }


  static class ReplaceEvent extends ViewChange {
    private final ItemViewState myNewState;

    public ReplaceEvent(ItemViewFrame prevState, ItemViewState newState) {
      super(prevState.getItemsList(), prevState.getItems());
      myNewState = newState;
    }

    public void getRange(int firstIndex, ItemAcceptor acceptor) {
      // todo
      LongList items = myNewState.getItems();
      LongListIterator it = items.iterator(firstIndex, items.size());
      while (it.hasNext()) {
        if (!acceptor.accept(firstIndex, it.nextValue(), ItemState.INSERTED))
          return;
        firstIndex++;
      }
    }

    public int getNewIndex(int oldIndex) {
      return -1;
    }

    public int getCountDelta() {
      return myNewState.getItems().size() - getPrevItems().size();
    }

    public LongIterator getRemovedItems() {
      return getPrevItems().iterator();
    }

    public LongList getItemList() {
      return myNewState.getItemList();
    }

    public IntList getDepths() {
      return myNewState.getDepths();
    }

    public LongList getItemGraph() {
      return myNewState.getItemGraph();
    }

    @Nullable
    public LongListDiffIndexedDecorator getItemTree() {
      return myNewState.getItemsTree();
    }

    @Override
    public AbstractGroupsDist getGroups() {
      return myNewState.getGroups();
    }
  }


  static class UpdateEvent extends ViewChange {
    private final LongList myRemovedItems;

    @Nullable
    private final LongListRemovingDecorator myRemoveDecorator;
    @Nullable
    private final LongListInsertingDecorator myUpdateDecorator;
    @Nullable
    private final LongListInsertingDecorator myInsertDecorator;
    @Nullable
    private final LongListDiffIndexedDecorator myItemTree;
    @Nullable
    private final WritableIntList myDepths;
    @Nullable
    private final LongList myItemGraph;
    @Nullable
    private final AbstractGroupsDist myGroups;

    UpdateEvent(LongList prevItemList, LongList prevItems, LongList removedItems,
      @Nullable LongListRemovingDecorator removeDecorator, @Nullable LongListInsertingDecorator updateDecorator,
      @Nullable LongListInsertingDecorator insertDecorator, LongListDiffIndexedDecorator itemTree, WritableIntList depths,
      LongList itemGraph, @Nullable AbstractGroupsDist groups)
    {
      super(prevItemList, prevItems);
      myRemovedItems = removedItems;
      myRemoveDecorator = removeDecorator;
      myUpdateDecorator = updateDecorator;
      myInsertDecorator = insertDecorator;
      myItemTree = itemTree;
      myDepths = depths;
      myItemGraph = itemGraph;
      myGroups = groups;
    }

    public int getNewIndex(int oldIndex) {
      if (myItemTree == null) {
        // search by decorators
        int result = myRemoveDecorator != null ? myRemoveDecorator.getNewIndex(oldIndex) : oldIndex;
        if (myUpdateDecorator != null && result >= 0)
          result = myUpdateDecorator.getNewIndex(result);
        if (myInsertDecorator != null && result >= 0)
          result = myInsertDecorator.getNewIndex(result);
        return result;
      } else {
        // linear search, trying to guess that the position in the new list should be almost the same in most cases
        LongList prev = getPrevItems();
        LongList updated = getItems();
        if (updated.isEmpty())
          return -1;
        long item = prev.get(oldIndex);
        int ss = Math.min(oldIndex, updated.size() - 1);
        LongListIterator forward = updated.iterator(ss);
        LongListIterator backward = updated.iterator(0, ss + 1);
        backward.move(ss);
        while (forward.hasNext() || backward.index() > 0) {
          if (forward.hasNext() && forward.nextValue() == item)
            return forward.index();
          if (backward.index() > 0) {
            backward.move(-1);
            if (backward.get(0) == item)
              return backward.index();
          }
        }
        return -1;
      }
    }

    public void getRange(final int firstIndex, final ItemAcceptor acceptor) {
      if (myItemTree != null) {
        assert false;
        // does not work with trees
        // todo remove method
        return;
      }

      LongList topCollection = getItemList();

      // todo hack: get rid of "source" in IntVisitor
      LongList prev = getPrevItemList();
      if (prev instanceof AbstractLongListDecorator)
        prev = ((AbstractLongListDecorator) prev).getBase();

      final LongList prevCollection = prev;

      AbstractLongListDecorator.iterate(topCollection, firstIndex, topCollection.size(),
        new AbstractLongListDecorator.LongVisitor() {
          int index = firstIndex;

          public boolean accept(long value, LongList source) {
            boolean result = acceptor.accept(index, value, getItemState(source, prevCollection));
            index++;
            return result;
          }
        });
    }

    private ItemState getItemState(Object source, LongList oldCollection) {
      ItemState state;
      if (source == oldCollection)
        state = ItemState.NOT_CHANGED;
      else if (myUpdateDecorator != null && source == myUpdateDecorator)
        state = ItemState.UPDATED;
      else if (myInsertDecorator != null && source == myInsertDecorator)
        state = ItemState.INSERTED;
      else {
        assert false : source;
        state = ItemState.UPDATED;
      }
      return state;
    }

    public LongList getItemList() {
      if (myInsertDecorator != null)
        return myInsertDecorator;
      if (myUpdateDecorator != null)
        return myUpdateDecorator;
      if (myRemoveDecorator != null)
        return myRemoveDecorator;
      assert false;
      return getPrevItemList();
    }

    public IntList getDepths() {
      return myDepths;
    }

    public LongList getItemGraph() {
      return myItemGraph;
    }

    @Nullable
    public LongListDiffIndexedDecorator getItemTree() {
      return myItemTree;
    }

    public AbstractGroupsDist getGroups() {
      return myGroups;
    }

    public int getCountDelta() {
      int result = 0;
      if (myInsertDecorator != null)
        result += myInsertDecorator.getInsertCount();
      if (myUpdateDecorator != null)
        result += myUpdateDecorator.getInsertCount();
      if (myRemoveDecorator != null)
        result -= myRemoveDecorator.getRemoveCount();
      return result;
    }

    public LongIterator getRemovedItems() {
      return myRemovedItems.iterator();
    }
  }


  /**
   * Builds tree-related information either when (multiple) grouping is defined or not.
   */
  private static class TreeInfo {
    @NotNull
    private final LongList myItems;
    @NotNull
    private final LongList myItemGraph;
    @Nullable
    private final AbstractGroupsDist myGroups;
    private WritableIntList myDepths;
    private LongListDiffIndexedDecorator myItemTree;

    public TreeInfo(@NotNull LongList items, @NotNull LongList itemGraph, @Nullable AbstractGroupsDist groups) {
      myItems = items;
      myItemGraph = itemGraph;
      myGroups = groups;
    }

    public WritableIntList getDepths() {
      return myDepths;
    }

    public LongListDiffIndexedDecorator getItemTree() {
      return myItemTree;
    }

    public TreeInfo build() {
      LongList graph = myItemGraph;
      if (myGroups != null) {
        graph = restrictGraph();
      }
      assert checkGraph(graph, myItems);
      ItemTreeBuilder builder = new ItemTreeBuilder(myItems, graph);
      builder.build();
      IntList treeIndexes = builder.getResultTreeIndexes();
      myItemTree = new LongListDiffIndexedDecorator(myItems, treeIndexes);
      myDepths = builder.getResultDepths();
      return this;
    }

    /**
     * Restricts the graph so that the links do not go outside groups
     */
    private WritableLongList restrictGraph() {
      int count = myItems.size();
      WritableLongList graph = createLongList();
      AbstractGroupsDist.Iterator it = myGroups.iterator();
      int groupEnd = it.nextValue();
      while (it.hasNext()) {
        int groupStart = groupEnd;
        groupEnd = it.nextValue();
        graph.addAll(ItemTreeBuilder.restrictGraph(myItems.subList(groupStart, groupEnd), myItemGraph));
      }
      graph.addAll(ItemTreeBuilder.restrictGraph(myItems.subList(groupEnd, count), myItemGraph));

      restoreGraphInvariants(graph);
      return graph;
    }

    private static void restoreGraphInvariants(final WritableLongList graph) {
      CollectionUtil.quicksort(graph.size() >> 1, new IntIntFunction2() {
        public int invoke(int a, int b) {
          return org.almworks.util.Util.compareLongs(graph.get(a << 1), graph.get(b << 1));
        }
      }, new IntProcedure2() {
        public void invoke(int a, int b) {
          graph.swap(a << 1, b << 1);
          graph.swap((a << 1) + 1, (b << 1) + 1);
        }
      });
    }

    private static boolean checkGraph(LongList graph, LongList items) {
      long[] resultSet = items.toNativeArray();
      Arrays.sort(resultSet);
      long lastFrom = Integer.MIN_VALUE;
      for (LongIterator ii = graph.iterator(); ii.hasNext();) {
        long from = ii.nextValue();
        assert Arrays.binarySearch(resultSet, from) >= 0 : from;
        assert from >= lastFrom : lastFrom + " " + from;
        lastFrom = from;
        if (!ii.hasNext()) {
          assert false : graph;
        } else {
          long to = ii.nextValue();
          assert Arrays.binarySearch(resultSet, to) >= 0 : to;
        }
      }
      return true;
    }
  }
}
