package com.almworks.items.api;

import org.almworks.util.Util;

public interface ItemReference {
  long findItem(DBReader reader);

  ItemReference NO_ITEM = new ItemReference() {
    @Override
    public long findItem(DBReader reader) {
      return 0;
    }
  };

  class Item implements ItemReference {
    private final long myItem;

    public Item(long item) {
      myItem = item;
    }

    @Override
    public long findItem(DBReader reader) {
      return myItem;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (!(obj instanceof Item)) return false;
      Item other = (Item) obj;
      return myItem == other.myItem;
    }

    @Override
    public int hashCode() {
      return ((int)myItem) ^ ((int)(myItem >> 32));
    }
  }

  class GetItem implements ItemReference {
    private final ItemReference mySource;
    private final DBAttribute<Long> myAttribute;

    public GetItem(ItemReference source, DBAttribute<Long> attribute) {
      mySource = source;
      myAttribute = attribute;
    }

    @Override
    public long findItem(DBReader reader) {
      long source = mySource.findItem(reader);
      if (source <= 0) return 0;
      Long value = myAttribute.getValue(source, reader);
      return value != null && value > 0 ? value : 0;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      GetItem other = Util.castNullable(GetItem.class, obj);
      return other != null && Util.equals(mySource, other.mySource) && Util.equals(myAttribute, other.myAttribute);
    }

    @Override
    public int hashCode() {
      return mySource.hashCode() ^ myAttribute.hashCode();
    }
  }
}
