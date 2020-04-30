package com.almworks.items.gui.meta;

import com.almworks.api.application.LoadedItem;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.util.CachedItem;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.columns.ColumnComparator;
import com.almworks.items.gui.meta.schema.columns.ColumnRenderer;
import com.almworks.items.gui.meta.schema.columns.Columns;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.ColumnTooltipProvider;
import com.almworks.util.model.ValueModel;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.util.Comparator;

public class ColumnsCollector extends BaseCollector<TableColumnAccessor<LoadedItem, ?>, ColumnsCollector.Holder> {
  private static final DataLoader<ColumnSizePolicy> SIZE_POLICY = SerializedObjectAttribute.create(
    ColumnSizePolicy.class, Columns.SIZE_POLICY);
  private static final DataLoader<Convertor> CONVERTOR = SerializedObjectAttribute.create(Convertor.class,
    Columns.CONVERTOR);
  private static final DataLoader<ColumnTooltipProvider<LoadedItem>> TOOLTIP_PROVIDER =
    (DataLoader) SerializedObjectAttribute.create(ColumnTooltipProvider.class, Columns.TOOLTIP_PROVIDER);

  private ColumnsCollector(ImageSlice slice) {
    super(slice);
  }

  public AListModel<TableColumnAccessor<LoadedItem, ?>> filterModel(final Lifespan life, final ValueModel<? extends ScopeFilter> scope) {
    return ModelScopeFilter.filterAndConvert(life, myModel, scope, toValue);
  }

  public static ColumnsCollector create(DBImage image) {
    return new ColumnsCollector(image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, Columns.DB_TYPE)));
  }

  @Override
  protected void initSlice(ImageSlice slice) {
    slice.addAttributes(Columns.ID, Columns.NAME, Columns.HEADER_TEXT, Columns.HEADER_TOOLTIP);
    slice.addData(ColumnRenderer.LOADER, ColumnComparator.LOADER,
      SIZE_POLICY,
      CONVERTOR,
      TOOLTIP_PROVIDER
    );
  }

  @Override
  protected Holder createHolder(long item) {
    return new Holder(item);
  }

  private static final Comparator<LoadedItem> DEFAULT_COLUMN_ORDER = new Comparator<LoadedItem>() {
    @Override
    public int compare(LoadedItem o1, LoadedItem o2) {
      return Util.compareLongs(getItemSafe(o1), getItemSafe(o2));
    }
    private long getItemSafe(LoadedItem o1) {
      return o1 != null ?  o1.getItem() : 0;
    }
  };

  class Holder extends BaseCollector.BaseHolder<TableColumnAccessor<LoadedItem, ?>> implements CachedItem {
    public Holder(long item) {
      super(item);
    }

    public <T> T getValue(DBAttribute<T> attribute) {
      return myModel.getSlice().getValue(myItem, attribute);
    }

    public <T> T getValue(DataLoader<T> loader) {
      return myModel.getSlice().getValue(myItem, loader);
    }

    @Override
    public DBImage getImage() {
      return myModel.getImage();
    }

    @Override
    protected TableColumnAccessor<LoadedItem, ?> createValue() {
      return createColumn();
    }

    private <T> TableColumnAccessor<LoadedItem, T> createColumn() {
      TableColumnBuilder<LoadedItem, T> builder = new TableColumnBuilder<LoadedItem, T>()
        .setId(getValue(Columns.ID))
        .setName(getValue(Columns.NAME))
        .setHeaderText(getValue(Columns.HEADER_TEXT))
        .setDefaultComparator(DEFAULT_COLUMN_ORDER)
        .setTooltipProvider(getValue(TOOLTIP_PROVIDER))
        .setHeaderTooltip(getValue(Columns.HEADER_TOOLTIP))
        .setReorderable(true);
      Convertor convertor = getValue(CONVERTOR);
      if (convertor != null) builder.setConvertor(convertor);
      ColumnRenderer rendererSetup = getValue(ColumnRenderer.LOADER);
      if (rendererSetup != null)
        if (!rendererSetup.setupRenderer(builder, convertor)) return null;
      ColumnComparator comparator = getValue(ColumnComparator.LOADER);
      if (comparator != null)
        if (!comparator.setupComparator(builder, convertor)) return null;
      // As the following properties are mandatory, we cannot pass nulls to the builder
      ColumnSizePolicy sizePolicy = getValue(SIZE_POLICY);
      if (sizePolicy != null) builder.setSizePolicy(sizePolicy);
      if (!builder.verify()) {
        LogHelper.error("Cannot create column", builder);
        return null;
      }
      return builder.createColumn();
    }
  }
}
