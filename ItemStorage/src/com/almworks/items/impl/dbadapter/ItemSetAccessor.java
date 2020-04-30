package com.almworks.items.impl.dbadapter;

import com.almworks.integers.LongIterator;
import com.almworks.items.impl.dbadapter.util.SingleItemSetAccessor;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.LongObjFunction;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public interface ItemSetAccessor extends Iterable<ItemAccessor> {
  ItemSetAccessor EMPTY = new EmptySetAccessor();
  Convertor<ItemAccessor, ItemSetAccessor> SINGULAR_SET = new Convertor<ItemAccessor, ItemSetAccessor>() {
    public ItemSetAccessor convert(ItemAccessor value) {
      return value == null ? EMPTY : new SingleItemSetAccessor(value);
    }
  };

  <T> T visit(Visitor<T> visitor);

  LongIterator getItems();

  int getCount();

  @Nullable
  ItemAccessor getFirst();

  boolean isEmpty();

  interface Visitor<T> {
    T visit(ItemAccessor item, @NotNull boolean[] wantMore);
  }


  class EmptySetAccessor implements ItemSetAccessor {
    public <T> T visit(Visitor<T> visitor) {
      return null;
    }

    public LongIterator getItems() {
      return LongIterator.EMPTY;
    }

    public int getCount() {
      return 0;
    }

    @Nullable
    public ItemAccessor getFirst() {
      return null;
    }

    public boolean isEmpty() {
      return true;
    }

    public Iterator<ItemAccessor> iterator() {
      return Collections15.emptyIterator();
    }
  }

  class ItemCollectionIterator implements Iterator<ItemAccessor> {
    private final LongIterator myIt;
    private final LongObjFunction<? extends ItemAccessor> myGetter;

    public ItemCollectionIterator(LongIterator it, LongObjFunction<? extends ItemAccessor> getter) {
      myIt = it;
      myGetter = getter;
    }

    public boolean hasNext() {
      return myIt.hasNext();
    }

    public ItemAccessor next() {
      long item = myIt.nextValue();
      //noinspection ReturnOfNull
      return item >= 0 ? myGetter.invoke(item) : null;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
