package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.AttributeReference;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.items.cache.util.ItemImageCollector;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import org.almworks.util.detach.DetachComposite;

import java.util.Set;

public class DnDChangeCollector implements ItemImageCollector.ImageFactory<DnDChange> {
  private static final AttributeReference<?> ATTRIBUTE_LOADER = AttributeReference.create(DnDChange.CUBE_ATTRIBUTE);
  private static final DataLoader<Long> MODEL_KEY = new ItemAttribute(DnDChange.MODEL_KEY);
  private static final DataLoader<Long> CONSTRAINT = new ItemAttribute(DnDChange.CONSTRAINT);
  private static final DataLoader<Long> ENUM_TYPE = new ItemAttribute(DnDChange.ENUM_TYPE);
  private static final DataLoader<String> CONFIG = AttributeLoader.create(DnDChange.CONFIG);
  private static final DataLoader<String> NOT_SUPPORTED = AttributeLoader.create(DnDChange.NOT_SUPPORTED_MESSAGE);

  private final ItemImageCollector<DnDChange> myChanges;

  public DnDChangeCollector(DBImage image) {
    QueryImageSlice slice = image.createQuerySlice(DPNotNull.create(DnDChange.CUBE_ATTRIBUTE));
    slice.addData(ATTRIBUTE_LOADER, MODEL_KEY, CONSTRAINT, ENUM_TYPE, CONFIG, NOT_SUPPORTED, Applicability.ATTRIBUTE);
    myChanges = ItemImageCollector.create(slice, this, false);
  }

  public void start(DetachComposite life) {
    myChanges.start(life);
  }

  public DnDChange getDnDChange(DBAttribute<?> attribute) {
    ImageSlice slice = myChanges.getSlice();
    int index = slice.findIndexByValue(0, ATTRIBUTE_LOADER, attribute);
    if (index < 0) return null;
    int next = slice.findIndexByValue(index + 1, ATTRIBUTE_LOADER, attribute);
    if (next >= 0) LogHelper.error("Duplicated attribute", attribute, getChangeAtIndex(index), getChangeAtIndex(next));
    return getChangeAtIndex(index);
  }

  private DnDChange getChangeAtIndex(int index) {
    return myChanges.getImage(myChanges.getSlice().getItem(index));
  }

  @Override
  public boolean update(DnDChange image, long item) {
    LogHelper.error("DnDChange update not supported", item, image);
    return false;
  }

  @Override
  public void onRemoved(DnDChange image) {
  }

  @Override
  public DnDChange create(long item) {
    ImageSlice slice = myChanges.getSlice();
    DBAttribute<?> attribute = slice.getValue(item, ATTRIBUTE_LOADER);
    if (attribute == null) {
      LogHelper.error("Missing DnD change attribute", item);
      return null;
    }
    int index = 0;
    while ((index = slice.findIndexByValue(index, ATTRIBUTE_LOADER, attribute)) >= 0) {
      long anItem = slice.getItem(index);
      LogHelper.assertError(anItem == item, "Duplicated changes for attribute", item, anItem, attribute);
      index++;
    }
    long modelKey = slice.getNNValue(item, MODEL_KEY, 0l);
    long constraint = slice.getNNValue(item, CONSTRAINT, 0l);
    long enumType = slice.getNNValue(item, ENUM_TYPE, 0l);
    Applicability applicability = slice.getValue(item, Applicability.ATTRIBUTE);
    String notSupported = slice.getValue(item, NOT_SUPPORTED);
    if (notSupported == null && (modelKey == 0 || constraint == 0 || enumType == 0 || applicability == null)) {
      LogHelper.error("Missing data", modelKey, constraint, enumType, applicability);
      return null;
    }
    DBAttribute<Long> itemRef = BadUtil.castScalar(Long.class, attribute);
    String config = slice.getValue(item, CONFIG);
    if (itemRef != null) {
      if (notSupported != null) return new NotSupportedDnD.Single(itemRef, modelKey, notSupported);
      return new DefaultSingleDnDChange(itemRef, new DnDVariants(enumType, config), modelKey, constraint, applicability);
    } else {
      DBAttribute<Set<Long>> refSet = BadUtil.castSetAttribute(Long.class, attribute);
      if (refSet == null) {
        LogHelper.error("Unknown attribute", attribute);
        return null;
      }
      if (notSupported != null) return new NotSupportedDnD.Multi(refSet, modelKey, notSupported);
      return new DefaultMultiDnDChange(refSet, new DnDVariants(enumType, config), modelKey, constraint, applicability);
    }
  }
}
