package com.almworks.items.gui.meta.schema.columns;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemDownloadStageKey;
import com.almworks.api.application.LoadedItem;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.renderers.ItemRenderers;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.components.renderer.ShadingComponent;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.ColorUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ColumnRenderer {
  public static final DataLoader<ColumnRenderer> LOADER = SerializedObjectAttribute.create(ColumnRenderer.class,
    Columns.RENDERER_SETUP);
  /**
   * Uses CollectionRenderer as LoadItem value renderer (value takes from column value convertor {@link Columns#CONVERTOR}).<br>
   * Shades the cell if item download stage is less than minDownloadStage. Supports
   * <ul>
   * <li>DUMMY - never shaded</li>
   * <li>QUICK - shades DUMMY</li>
   * <li>FULL - shades if details are not ever loaded</li>
   * </ul>
   * Sequence [minDownloadStage(dbValue), valueCollectionRenderer]
   */
  private static final DBIdentity FEATURE_VALUE_RENDERER = Columns.feature("renderers.valueRenderer");
  /**
   * Converts CanvasRenderer to CollectionRenderer
   * Sequence [canvasRenderer]
   */
  private static final DBIdentity FEATURE_CANVAS_RENDERER = Columns.feature("renderers.canvasRenderer");

  public abstract <T> boolean setupRenderer(TableColumnBuilder<LoadedItem, T> builder, @Nullable Convertor<LoadedItem, T> convertor);

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_VALUE_RENDERER, ValueRenderer.FEATURE);
    registry.register(FEATURE_CANVAS_RENDERER, CANVAS_CONVERTOR);
  }

  public static ScalarSequence valueCanvasDefault(ItemDownloadStage stage, int fontStyle, String nullText) {
    return canvasRenderer(stage, ItemRenderers.defaultCanvas(fontStyle, nullText));
  }

  public static ScalarSequence valueListCanvasDefault(ItemDownloadStage minStage, String separator) {
    return canvasRenderer(minStage, ItemRenderers.listRenderer(separator, ItemRenderers.defaultCanvas(0, "")));
  }

  public static ScalarSequence valueSeconds(ItemDownloadStage minStage) {
    return canvasRenderer(minStage, ItemRenderers.SEQUENCE_SECONDS_RENDERER);
  }

  public static ScalarSequence shortestDate(ItemDownloadStage minStage) {
    return canvasRenderer(minStage, ItemRenderers.SEQUENCE_SHORTEST_DATE_RENDERER);
  }

  public static ScalarSequence relativeDays(ItemDownloadStage minStage, int maxDays) {
    return canvasRenderer(minStage, ItemRenderers.relativeDays(maxDays));
  }

  public static ScalarSequence canvasRenderer(ItemDownloadStage minStage, ScalarSequence renderer) {
    return new ScalarSequence.Builder()
      .append(FEATURE_VALUE_RENDERER).append(minStage.getDbValue())
      .append(FEATURE_CANVAS_RENDERER).appendSubsequence(renderer)
      .create();
  }

  private static class ValueRenderer extends ColumnRenderer {
    public static final SerializableFeature<ColumnRenderer> FEATURE = new SerializableFeature<ColumnRenderer>() {
      @Override
      public ColumnRenderer restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        int intStage = stream.nextInt();
        ItemDownloadStage stage = ItemDownloadStage.fromDbValue(intStage);
        CollectionRenderer renderer = SerializedObjectAttribute.restore(reader, stream, CollectionRenderer.class,
          invalidate);
        return renderer != null ? new ValueRenderer(renderer, stage) : null;
      }

      @Override
      public Class<ColumnRenderer> getValueClass() {
        return ColumnRenderer.class;
      }
    };

    private final CollectionRenderer myValueRenderer;
    private final ItemDownloadStage myMinStage;

    private ValueRenderer(CollectionRenderer valueRenderer, ItemDownloadStage minStage) {
      myValueRenderer = valueRenderer;
      myMinStage = minStage;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> boolean setupRenderer(TableColumnBuilder<LoadedItem, T> builder, @Nullable Convertor<LoadedItem, T> convertor) {
      if (convertor == null) {
        LogHelper.error("Convertor required", builder.getHeaderTextNN(), builder.getNameNN());
        return false;
      }
      builder.setRenderer(new ShadingRenderer(myMinStage, Renderers.convertingRenderer(myValueRenderer, convertor)));
      return true;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      ValueRenderer other = Util.castNullable(ValueRenderer.class, obj);
      return other != null && Util.equals(myValueRenderer, other.myValueRenderer) && Util.equals(myMinStage, other.myMinStage);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myValueRenderer, myMinStage) ^ ValueRenderer.class.hashCode();
    }
  }

  private static final SerializableFeature<CollectionRenderer> CANVAS_CONVERTOR =
    new SerializableFeature<CollectionRenderer>() {
      @SuppressWarnings({"unchecked"})
      @Override
      public CollectionRenderer restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        CanvasRenderer renderer = SerializedObjectAttribute.restore(reader, stream.nextSubstream(), CanvasRenderer.class, invalidate);
        return renderer != null ? Renderers.createRenderer(renderer) : null;
      }

      @Override
      public Class<CollectionRenderer> getValueClass() {
        return CollectionRenderer.class;
      }
    };

  private static class ShadingRenderer implements CollectionRenderer<LoadedItem> {
    private final ItemDownloadStage myStage;
    private final CollectionRenderer<LoadedItem> myRenderer;
    private final JComponent myNoDummyValue = new ShadingComponent();

    private ShadingRenderer(ItemDownloadStage stage, CollectionRenderer<LoadedItem> renderer) {
      myStage = ItemDownloadStage.fixForUI(stage);
      myRenderer = renderer;
    }

    @Override
    public JComponent getRendererComponent(CellState state, LoadedItem item) {
      boolean shade;
      if (item == null) shade = true;
      else {
        ItemDownloadStage stage = ItemDownloadStageKey.retrieveValue(item);
        shade = !ItemDownloadStage.hasValueForUI(myStage, stage);
      }
      if (!shade) return myRenderer.getRendererComponent(state, item);
      else {
        myNoDummyValue.setForeground(ColorUtil.between(state.getForeground(), state.getOpaqueBackground(), 0.75f));
        state.setBackgroundTo(myNoDummyValue, true);
        myNoDummyValue.setBorder(state.getBorder());
        return myNoDummyValue;
      }
    }
  }
}
