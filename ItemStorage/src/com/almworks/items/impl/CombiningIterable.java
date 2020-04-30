package com.almworks.items.impl;

import com.almworks.integers.LongIntersectionIterator;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongMinusIterator;
import org.jetbrains.annotations.NotNull;

public class CombiningIterable implements LongIterable {
  private final LongIterable myInputItems;
  private final LongIterable myReferredItems;
  private final boolean myExclude;

  public CombiningIterable(LongIterable inputItems, LongIterable referredItems, boolean exclude) {
    myInputItems = inputItems;
    myReferredItems = referredItems;
    myExclude = exclude;
  }

  @NotNull
  public LongIterator iterator() {
    return myExclude ? new LongMinusIterator(myInputItems.iterator(), myReferredItems.iterator()) :
      new LongIntersectionIterator(myInputItems.iterator(), myReferredItems.iterator());
  }
}
