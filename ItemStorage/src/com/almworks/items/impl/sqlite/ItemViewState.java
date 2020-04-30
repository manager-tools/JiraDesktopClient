package com.almworks.items.impl.sqlite;

import com.almworks.integers.IntList;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListDiffIndexedDecorator;
import com.almworks.items.impl.dbadapter.AbstractGroupsDist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemViewState {
  /**
   * Contains item list as returned from DB with sorting, before tree is applied.
   */
  @NotNull
  private final LongList myItemsList;

  @Nullable
  private final LongList myItemGraph;

  /**
   * If there's a tree, contains reindexing decorator
   */
  @Nullable
  private final LongListDiffIndexedDecorator myItemsTree;

  /**
   * Contains items as they are seen by the user. Either == myItemsList or myItemsTree
   */
  @NotNull
  private final LongList myItems;

  @Nullable
  private final IntList myDepths;

  /**
   * Distribution of items between groups.
   * One may think of it as a 2D-array, where myGroups[j][i] is group ID in the grouping j for the item with index i in the items list.
   */
  @Nullable
  private final AbstractGroupsDist myGroups;

  public ItemViewState(LongList itemList) {
    this(itemList, null, null, null, null);
  }

  public ItemViewState(LongList itemList, LongList itemGraph, LongListDiffIndexedDecorator itemTree, IntList depths, AbstractGroupsDist groups) {
    assert !(itemTree == null ^ depths == null) : itemTree + " " + depths; 
    assert !(itemTree == null ^ itemGraph == null) : itemTree + " " + depths;
    myItemsList = itemList;
    myItemGraph = itemGraph;
    myItemsTree = itemTree;
    myDepths = depths;
    myGroups = groups;
    myItems = myItemsTree == null ? myItemsList : myItemsTree;
  }

  public LongList getItems() {
    return myItems;
  }

  @Nullable
  public IntList getDepths() {
    return myDepths;
  }

  @NotNull
  public LongList getItemList() {
    return myItemsList;
  }

  @Nullable
  public LongListDiffIndexedDecorator getItemsTree() {
    return myItemsTree;
  }

  @Nullable
  public LongList getItemGraph() {
    return myItemGraph;
  }

  @Nullable
  public AbstractGroupsDist getGroups() {
    return myGroups;
  }
}
