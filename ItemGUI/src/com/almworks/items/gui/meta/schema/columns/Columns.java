package com.almworks.items.gui.meta.schema.columns;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.DBCommons;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.ModelKeyCollector;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ColumnTooltipProvider;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.models.TableColumnAccessor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * Describes structure of a DB item corresponding to an item table column({@code TableColumnAccessor<LoadedItem, ?>}).<br>
 * @see com.almworks.util.models.TableColumnBuilder#createColumn()
 */
public class Columns {
  private static final DBNamespace NS = MetaModule.NS.subNs("columns");
  static final DBNamespace NS_FEATURES = NS.subNs("features");
  public static final DBItemType DB_TYPE = NS.type();

  /** {@link TableColumnAccessor#getId() Column ID} as stored in table configuration. Mandatory. */
  public static final DBAttribute<String> ID = NS.string("id");
  /**
   * Contains {@link ColumnRenderer columnRenderer}, loaded by {@link ColumnRenderer#LOADER}.
   * @see ColumnRenderer
   */
  public static final DBAttribute<byte[]> RENDERER_SETUP = NS.bytes("rendererSetup");
  /** {@link TableColumnAccessor#getSizePolicy() Size policy}. Mandatory.
   * @see SizePolicies */
  public static final DBAttribute<byte[]> SIZE_POLICY = NS.bytes("sizePolicy");
  /** Converts row value (element) to cell value (value). Mandatory.
   * Sequence should restore to {@link Convertor Convertor&lt;LoadedItem, Object&gt;}
   * @see #convertListCount(com.almworks.items.sync.util.identity.DBStaticObject)
   * @see #convertModelKeyValue(com.almworks.items.sync.util.identity.DBStaticObject)
   */
  public static final DBAttribute<byte[]> CONVERTOR = NS.bytes("convertor");

  /** {@link TableColumnAccessor#getName() Column name}. Optional. */
  public static final DBAttribute<String> NAME = NS.string("name");
  /** {@link TableColumnAccessor#getColumnHeaderText() Header text}. Optional. */
  public static final DBAttribute<String> HEADER_TEXT = NS.string("headerText");

  /**
   * Contains {@link ColumnComparator columnComparator}, loaded by {@link ColumnComparator#LOADER}. Null comparator means column is not sortable.
   * @see ColumnComparator
   */
  public static final DBAttribute<byte[]> COMPARATOR = NS.bytes("comparator");
  /** Provides tooltip inside the cell ({@link ColumnTooltipProvider ColumnTooltipProvider&lt;LoadedItem&gt;}). Optional.
   * @see #FEATURE_CONVERTING_TOOLTIP_PROVIDER
   * @see #convertingTooltipProvider(DBIdentity)
   * @see ColumnTooltipProvider
   * @see LoadedItem
   * */
  public static final DBAttribute<byte[]> TOOLTIP_PROVIDER = NS.bytes("tooltipProvider");
  /** Tooltip text for the column header. Optional. */
  public static final DBAttribute<String> HEADER_TOOLTIP = NS.string("headerTooltip");

  /** Uses the specified convertor to convert column element (LoadedItem) into a {@link ColumnTooltipProvider}. */
  public static final DBIdentity FEATURE_CONVERTING_TOOLTIP_PROVIDER = feature("tooltipProvider.converting");

  private static final DBIdentity FEATURE_CONVERT_LIST_COUNT = feature("convert.listCount");
  private static final DBIdentity FEATURE_CONVERT_MODEL_KEY = feature("convert.modelKey");

  static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }

  public static DBStaticObject create(DBIdentity owner, String id, String displayName, String headerText, ScalarSequence sizePolicy, ScalarSequence convertor, ScalarSequence renderer, @Nullable ScalarSequence comparator, @Nullable ScalarSequence tooltipProvider, @Nullable String headerTooltip) {
    DBStaticObject.Builder builder = new DBStaticObject.Builder()
      .putSequence(Columns.SIZE_POLICY, sizePolicy)
      .putSequence(Columns.CONVERTOR, convertor)
      .putSequence(Columns.RENDERER_SETUP, renderer)
      .put(Columns.NAME, displayName)
      .put(Columns.HEADER_TEXT, headerText);
    if (comparator != null) builder.putSequence(Columns.COMPARATOR, comparator);
    if (tooltipProvider != null) builder.putSequence(TOOLTIP_PROVIDER, tooltipProvider);
    if (headerTooltip != null) builder.put(HEADER_TOOLTIP, headerTooltip);
    return builder.create(Columns.createId(owner, id));
  }

  public static DBIdentity createId(DBIdentity owner, String id) {
    return new DBIdentity.Builder().put(DBAttribute.TYPE, DB_TYPE).put(DBCommons.OWNER.getAttribute(), owner).put(ID, id).create();
  }

  /**
   * Converts list modelKey value to list size.
   * @param modelKey identified {@code ModelKey<? extends Collection>}
   * @return value for {@link #CONVERTOR}
   */
  public static ScalarSequence convertListCount(DBStaticObject modelKey) {
    return new ScalarSequence.Builder().append(FEATURE_CONVERT_LIST_COUNT).append(modelKey).create();
  }

  /**
   * Extract model key value as column value
   * @return value for {@link #CONVERTOR}
   */
  public static ScalarSequence convertModelKeyValue(DBStaticObject modelKey) {
    return new ScalarSequence.Builder().append(FEATURE_CONVERT_MODEL_KEY).append(modelKey).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_CONVERT_LIST_COUNT, CONVERT_LIST_SIZE);
    registry.register(FEATURE_CONVERT_MODEL_KEY, CONVERT_MODEL_KEY);
    registry.register(FEATURE_CONVERTING_TOOLTIP_PROVIDER, new ConvertingTooltipProvider());
    SizePolicies.registerFeatures(registry);
    ColumnRenderer.registerFeatures(registry);
    ColumnComparator.registerFeatures(registry);
  }

  /** @return value for {@link #TOOLTIP_PROVIDER} */
  public static ScalarSequence convertingTooltipProvider(DBIdentity convertor) {
    return new ScalarSequence.Builder().append(FEATURE_CONVERTING_TOOLTIP_PROVIDER).appendSubsequence(ScalarSequence.create(convertor)).create();
  }

  /**
   * Using the specified convertor, transforms a row into a tooltip provider for that row.
   * */
  private static class ConvertingTooltipProvider implements SerializableFeature<ColumnTooltipProvider> {
    @Override
    public ColumnTooltipProvider restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      final Convertor<LoadedItem, ColumnTooltipProvider<LoadedItem>> conv = SerializedObjectAttribute.restore(reader,
        stream.nextSubstream(), Convertor.class, invalidate);
      if (!stream.isSuccessfullyAtEnd()) return null;
      return new MyTooltipProvider(conv);
    }

    @Override
    public Class<ColumnTooltipProvider> getValueClass() {
      return ColumnTooltipProvider.class;
    }

    private static class MyTooltipProvider implements ColumnTooltipProvider<LoadedItem> {
      private final Convertor<LoadedItem, ColumnTooltipProvider<LoadedItem>> myConvertor;

      public MyTooltipProvider(Convertor<LoadedItem, ColumnTooltipProvider<LoadedItem>> convertor) {
        myConvertor = convertor;
      }

      @Override
      public String getTooltip(CellState cellState, LoadedItem element, Point cellPoint, Rectangle cellRect) {
        return myConvertor.convert(element).getTooltip(cellState, element, cellPoint, cellRect);
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) return true;
        MyTooltipProvider other = Util.castNullable(MyTooltipProvider.class, obj);
        return other != null && Util.equals(myConvertor, other.myConvertor);
      }

      @Override
      public int hashCode() {
        return Util.hashCode(myConvertor) ^ MyTooltipProvider.class.hashCode();
      }
    }
  }

  private static abstract class BaseModelKeyConvertion implements SerializableFeature<Convertor> {
    @Override
    public Convertor restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      final long modelKeyItem = stream.nextLong();
      if (!stream.isSuccessfullyAtEnd()) return null;
      return new MyConvertor(modelKeyItem, this);
    }

    protected abstract Object extractValue(@NotNull LoadedItem item, @NotNull LoadedModelKey<?> modelKey);

    @Override
    public Class<Convertor> getValueClass() {
      return Convertor.class;
    }

    private static class MyConvertor extends Convertor<LoadedItem, Object> {
      private final BaseModelKeyConvertion myConversion;
      private final long myModelKeyItem;

      public MyConvertor(long modelKeyItem, BaseModelKeyConvertion conversion) {
        myModelKeyItem = modelKeyItem;
        myConversion = conversion;
      }

      @Override
      public Object convert(LoadedItem value) {
        if (value == null) return 0;
        GuiFeaturesManager manager = value.services().getActor(GuiFeaturesManager.ROLE);
        if (manager == null) return null;
        ModelKeyCollector keys = manager.getModelKeyCollector();
        if (keys == null) return null;
        final LoadedModelKey<?> modelKey = keys.getKey(myModelKeyItem);
        if (modelKey == null) return 0;
        return myConversion.extractValue(value, modelKey);
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) return true;
        MyConvertor other = Util.castNullable(MyConvertor.class, obj);
        return other != null && Util.equals(myConversion, other.myConversion) && myModelKeyItem == other.myModelKeyItem;
      }

      @Override
      public int hashCode() {
        return (int)myModelKeyItem;
      }
    }
  }

  private static final SerializableFeature<Convertor> CONVERT_LIST_SIZE = new BaseModelKeyConvertion() {
    protected Object extractValue(@NotNull LoadedItem item, @NotNull LoadedModelKey<?> modelKey) {
      LoadedModelKey<List<Object>> listModelKey = modelKey.castList(Object.class);
      if (listModelKey == null) return 0;
      List<?> list = listModelKey.getValue(item.getValues());
      return list != null ? list.size() : 0;
    }
  };

  private static final SerializableFeature<Convertor> CONVERT_MODEL_KEY = new BaseModelKeyConvertion() {
    @Override
    protected Object extractValue(@NotNull LoadedItem item, @NotNull LoadedModelKey<?> modelKey) {
      return modelKey.getValue(item.getValues());
    }
  };
}
