package com.almworks.items.cache.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.util.advmodel.AListModel;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

public class CachedItemImpl implements CachedItem {
  private static final ItemSetModel.ItemWrapperFactory<CachedItem> CACHED_ITEM_FACTORY = new ItemSetModel.ItemWrapperFactory<CachedItem>() {
    @Override
    public CachedItem getForItem(ImageSlice slice, long item) {
      return new CachedItemImpl(slice.getImage(), item);
    }

    @Override
    public void afterChange(LongList removed, LongList changed, LongList added) {}
  };
  private final DBImage myImage;
  private final long myItem;

  public CachedItemImpl(DBImage image, long item) {
    myImage = image;
    myItem = item;
  }

  public static AListModel<CachedItem> createModel(Lifespan life, ImageSlice slice) {
    return ItemSetModel.create(life, slice, CACHED_ITEM_FACTORY);
  }

  public static ItemSetModel<CachedItem> notStartedModel(ImageSlice slice) {
    return new ItemSetModel<CachedItem>(slice, CACHED_ITEM_FACTORY);
  }

  @Override
  public <T> T getValue(DataLoader<T> loader) {
    return myImage.getValue(myItem, loader);
  }

  @Override
  public <T> T getValue(DBAttribute<T> attribute) {
    return myImage.getValue(myItem, attribute);
  }

  @Override
  public long getItem() {
    return myItem;
  }

  @Override
  public DBImage getImage() {
    return myImage;
  }

  @Override
  public int hashCode() {
    return myImage.hashCode() ^ ((int)myItem);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    CachedItemImpl other = Util.castNullable(CachedItemImpl.class, obj);
    return other != null && myItem == other.myItem && myImage.equals(other.myImage);
  }
}
