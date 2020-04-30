package com.almworks.items.gui.meta.schema.export;

import com.almworks.api.application.util.ExportContext;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.renderers.ItemRenderers;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.PlainTextCanvas;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class RenderValueExport<T> implements ModelKeyValueExport.ValueExport<T> {
  private static final DBNamespace NS_FEATURES = Exports.featuresNS("renderValue");
  public static final SerializableFeature<ModelKeyValueExport.ValueExport> FEATURE = new SerializableFeature<ModelKeyValueExport.ValueExport>() {
    @Override
    public ModelKeyValueExport.ValueExport restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      CanvasRenderer renderer = SerializedObjectAttribute.restore(reader, stream.nextSubstream(), CanvasRenderer.class, invalidate);
      //noinspection unchecked
      return new RenderValueExport(renderer);
    }

    @Override
    public Class<ModelKeyValueExport.ValueExport> getValueClass() {
      return ModelKeyValueExport.ValueExport.class;
    }
  };

  private final CanvasRenderer<T> myRenderer;

  public RenderValueExport(CanvasRenderer<T> renderer) {
    myRenderer = renderer;
  }

  @Override
  public String exportValue(ExportContext context, T value) {
    return PlainTextCanvas.renderText(value, myRenderer);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    RenderValueExport other = Util.castNullable(RenderValueExport.class, obj);
    return other != null && Util.equals(myRenderer, other.myRenderer);
  }

  @Override
  public int hashCode() {
    return Util.hashCode(myRenderer) ^ RenderValueExport.class.hashCode();
  }

  private static final DBIdentity FEATURE_RENDER_VALUE = feature("renderValue");
  public static final ScalarSequence SEQUENCE_DEFAULT_RENDERER = create(ItemRenderers.defaultCanvas(0, ""));
  public static final ScalarSequence SEQUENCE_RENDERER_BOOLEAN = create(ItemRenderers.defaultCanvas(Font.PLAIN, "false"));

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_RENDER_VALUE, FEATURE);
  }

  public static ScalarSequence create(ScalarSequence renderer) {
    return new ScalarSequence.Builder().append(FEATURE_RENDER_VALUE).appendSubsequence(renderer).create();
  }

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }
}
