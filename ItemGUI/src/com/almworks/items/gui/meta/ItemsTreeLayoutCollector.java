package com.almworks.items.gui.meta;

import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.engine.DBCommons;
import com.almworks.explorer.TableTreeStructure;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.ItemSetModel;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.ItemsTreeLayouts;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.TreeStructure;
import com.almworks.util.exec.ThreadGate;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.detach.Lifespan;

class ItemsTreeLayoutCollector {
  @SuppressWarnings({"unchecked"})
  public static final DataLoader<TableTreeStructure> STRUCTURE
    = (DataLoader) SerializedObjectAttribute.create(TreeStructure.class, ItemsTreeLayouts.STRUCTURE_DATA);

  private final ItemSetModel<ItemsTreeLayout> myModel;
  private final SortedListDecorator<ItemsTreeLayout> mySortedNnModel;

  public ItemsTreeLayoutCollector(DBImage image) {
    QueryImageSlice slice = image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, ItemsTreeLayouts.DB_TYPE));
    myModel = new ItemSetModel<ItemsTreeLayout>(slice, new LayoutFactory());
    mySortedNnModel = SortedListDecorator.createEmpty();
  }

  public void start(final Lifespan life) {
    ImageSlice slice = myModel.getSlice();
    slice.ensureStarted(life);
    slice.addData(STRUCTURE, DBCommons.OWNER);
    slice.addAttributes(ItemsTreeLayouts.NAME, ItemsTreeLayouts.ID, ItemsTreeLayouts.ORDER);
    myModel.start(life);
    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        FilteringListDecorator<ItemsTreeLayout> nnModel = FilteringListDecorator.create(life, myModel, Condition.<Object>notNull());
        mySortedNnModel.setComparator(Containers.<ItemsTreeLayout>comparablesComparator());
        mySortedNnModel.setSource(life, nnModel);
      }
    });
  }

  public AListModel<ItemsTreeLayout> getModel() {
    return mySortedNnModel;
  }

  private class LayoutFactory implements ItemSetModel.ItemWrapperFactory<ItemsTreeLayout> {
    private final TLongObjectHashMap<ItemsTreeLayout> myLayouts = new TLongObjectHashMap<ItemsTreeLayout>();

    @Override
    public ItemsTreeLayout getForItem(ImageSlice slice, long item) {
      ItemsTreeLayout layout = myLayouts.get(item);
      if (layout == null) {
        int order = getOrder(slice, item);
        layout = ItemsTreeLayout.create(
          slice.getValue(item, STRUCTURE),
          slice.getValue(item, ItemsTreeLayouts.NAME),
          slice.getValue(item, ItemsTreeLayouts.ID),
          slice.getValue(item, DBCommons.OWNER),
          order);
        myLayouts.put(item, layout);
      }
      return layout;
    }

    private int getOrder(ImageSlice slice, long item) {
      Integer order = slice.getValue(item, ItemsTreeLayouts.ORDER);
      return order != null ? order : (int)item;
    }

    @Override
    public void afterChange(LongList removed, LongList changed, LongList added) {
      for (LongListIterator i = removed.iterator(); i.hasNext(); ) {
        myLayouts.remove(i.nextValue());
      }
      for (LongListIterator i = changed.iterator(); i.hasNext(); ) {
        long item = i.nextValue();
        ItemsTreeLayout layout = myLayouts.get(item);
        if (layout != null) {
          ImageSlice slice = myModel.getSlice();
          layout.update(
            slice.getValue(item, STRUCTURE),
            slice.getValue(item, ItemsTreeLayouts.NAME),
            slice.getValue(item, ItemsTreeLayouts.ID),
            slice.getNNValue(item, DBCommons.OWNER, 0l),
            getOrder(slice, item));
        } else {
          LogHelper.warning("ITLC: Missed add of element", item);
          // do nothing -- ItemSetModel should know about the element index, if anyone asks for it, we'll create it in getForItem
        }
      }
    }
  }
}
