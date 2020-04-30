package com.almworks.items.gui.meta.schema.renderers;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.ListCanvasRenderer;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.components.renderer.SecondsRenderer;
import org.jetbrains.annotations.Nullable;

public class ItemRenderers {
  private static final DBNamespace NS = MetaModule.NS.subNs("renderers");
  private static final DBNamespace NS_FEATURES = NS.subNs("features");

  /**
   * DefaultCanvasRenderer with support for null value<br>
   * Sequence [nullFontStyle(int),nullText(String)]
   */
  private static final DBIdentity FEATURE_DEFAULT_CANVAS_RENDERER = feature("renderer.defaultCanvasRenderer");
  /**
   * CanvasRenderer&lt;{@link Integer}&gt;. Renderer integer as seconds duration. Takes no parameters.
   * @see SecondsRenderer
   */
  private static final DBIdentity FEATURE_SECONDS_RENDERER = feature("renderer.secondsRenderer");
  public static final ScalarSequence SEQUENCE_SECONDS_RENDERER = ScalarSequence.create(FEATURE_SECONDS_RENDERER);
  /**
   * CanvasRenderer&lt;{@link java.util.Date}&gt;
   * {@link com.almworks.util.components.renderer.Renderers#shortestDate()}
   */
  private static final DBIdentity FEATURE_SHORTEST_DATE_RENDERER = feature("renderer.shortestDate");
  public static final ScalarSequence SEQUENCE_SHORTEST_DATE_RENDERER = ScalarSequence.create(FEATURE_SHORTEST_DATE_RENDERER);
  /**
   * CanvasRenderer. Sequence: [dayRange(int)]
   * @see Renderers.RelativeDaysRenderer
   */
  private static final DBIdentity FEATURE_RELATIVE_DAY_RENDERER = feature("renderer.relativeDays");
  /**
   * CanvasRenderer. Renderers {@link java.util.List} value passing each value to next canvas renderer.<br>
   * Sequence [separator(String), nextRenderer(CanvasRenderer)]
   */
  private static final DBIdentity FEATURE_LIST_RENDERER = feature("renderer.listRenderer");
  /**
   * CanvasRenderer&lt;{@link ItemKey}&gt;
   * @see ItemKey#NAME_ID_RENDERER
   */
  private static final DBIdentity FEATURE_NAME_ID_RENDERER = feature("renderer.nameId");
  public static final ScalarSequence SEQUENCE_NAME_ID_RENDERER = ScalarSequence.create(FEATURE_NAME_ID_RENDERER);

  public static ScalarSequence defaultCanvas(int fontStyle, String nullText) {
    return new ScalarSequence.Builder().append(FEATURE_DEFAULT_CANVAS_RENDERER).append(fontStyle).append(nullText).create();
  }

  public static ScalarSequence listRenderer(String separator, ScalarSequence elementRenderer) {
    return new ScalarSequence.Builder().append(FEATURE_LIST_RENDERER).append(separator).appendSubsequence(elementRenderer).create();
  }

  public static void secondsRenderer(ScalarSequence.Builder builder) {
    builder.append(FEATURE_SECONDS_RENDERER);
  }

  public static ScalarSequence relativeDays(int maxDays) {
    return new ScalarSequence.Builder().append(FEATURE_RELATIVE_DAY_RENDERER).append(maxDays).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_NAME_ID_RENDERER, SerializableFeature.NoParameters.create(ItemKey.NAME_ID_RENDERER, CanvasRenderer.class));
    registry.register(FEATURE_DEFAULT_CANVAS_RENDERER, DEFAULT_CANVAS);
    registry.register(FEATURE_LIST_RENDERER, LIST_RENDERER);
    registry.register(FEATURE_SECONDS_RENDERER, SerializableFeature.NoParameters.create(new SecondsRenderer(), CanvasRenderer.class));
    registry.register(FEATURE_SHORTEST_DATE_RENDERER, SerializableFeature.NoParameters.create(Renderers.shortestDate(), CanvasRenderer.class));
    registry.register(FEATURE_RELATIVE_DAY_RENDERER, RELATIVE_DAYS);
  }

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }

  private static final SerializableFeature<CanvasRenderer> DEFAULT_CANVAS = new SerializableFeature<CanvasRenderer>() {
    @Override
    public CanvasRenderer restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      int fontStyle = stream.nextInt();
      if (stream.isErrorOccurred()) return null;
      String text = stream.nextUTF8();
      if (!stream.isSuccessfullyAtEnd()) return null;
      if (text == null || text.isEmpty()) return Renderers.defaultCanvasRenderer();
      return new Renderers.DefaultCanvasRenderer(new CanvasRenderable.TextRenderable(fontStyle, text));
    }

    @Override
    public Class<CanvasRenderer> getValueClass() {
      return CanvasRenderer.class;
    }
  };

  private static final SerializableFeature<CanvasRenderer> RELATIVE_DAYS = new SerializableFeature<CanvasRenderer>() {
    @Override
    public CanvasRenderer restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      int maxDays = stream.nextInt();
      if (!stream.isSuccessfullyAtEnd()) return null;
      return new Renderers.RelativeDaysRenderer(maxDays);
    }

    @Override
    public Class<CanvasRenderer> getValueClass() {
      return CanvasRenderer.class;
    }
  };

  private static final SerializableFeature<CanvasRenderer> LIST_RENDERER = new SerializableFeature<CanvasRenderer>() {
    @Override
    public CanvasRenderer restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      String separator = stream.nextUTF8();
      CanvasRenderer renderer = SerializedObjectAttribute.restore(reader, stream.nextSubstream(), CanvasRenderer.class, invalidate);
      if (renderer == null || !stream.isSuccessfullyAtEnd()) return null;
      return new ListCanvasRenderer(separator, renderer);
    }

    @Override
    public Class<CanvasRenderer> getValueClass() {
      return CanvasRenderer.class;
    }
  };
}
