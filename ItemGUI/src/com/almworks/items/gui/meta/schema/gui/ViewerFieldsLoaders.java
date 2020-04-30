package com.almworks.items.gui.meta.schema.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.ItemUrlService;
import com.almworks.engine.gui.*;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.ModelKeyCollector;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Function2;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ATextArea;
import com.almworks.util.components.renderer.ChangeCursorRendererActivity;
import com.almworks.util.components.renderer.FontStyle;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.*;
import com.almworks.util.config.Configuration;
import com.almworks.util.datetime.DateUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.almworks.engine.gui.LeftFieldsBuilder.*;
import static com.almworks.util.collections.Functional.apply2;
import static com.almworks.util.collections.Functional.curry;

class ViewerFieldsLoaders {
  public static void registerFeatures(FeatureRegistry features) {
    features.register(ViewerField.FEATURE_BEHAVIOUR_SEPARATOR,
      SerializableFeature.NoParameters.create(FIELD_SEPARATOR, ViewerField.class));
    features.register(ViewerField.FEATURE_BEHAVIOUR_SINGLE_ENUM, LOAD_SINGLE_ENUM);
    features.register(ViewerField.FEATURE_BEHAVIOUR_MULTI_ENUM, LOAD_MULTI_ENUM);
    features.register(ViewerField.FEATURE_CELL_CONVERTOR_LABEL, SerializableFeature.NoParameters.create(ItemKeyValueListCell.TO_LABEL_CELL, Convertor.class));
    features.register(ViewerField.FEATURE_CELL_CONVERTOR_ICON_TEXT, SerializableFeature.NoParameters.create(ItemKeyValueListCell.TO_ICON_LABEL, Convertor.class));
    features.register(ViewerField.FEATURE_BEHAVIOUR_INLINE_MULTI_ENUM, LOAD_INLINE_MULTI_ENUM);
    features.register(ViewerField.FEATURE_BEHAVIOUR_TEXT, LOAD_TEXT);
    features.register(ViewerField.FEATURE_BEHAVIOUR_DECIMAL_NOT_ZERO, LOAD_DECIMAL_NOT_ZERO);
    features.register(ViewerField.FEATURE_BEHAVIOUR_DECIMAL, LOAD_DECIMAL);
    features.register(ViewerField.FEATURE_BEHAVIOUR_INTEGER, LOAD_INTEGER);
    features.register(ViewerField.FEATURE_BEHAVIOUR_SECONDS_DURATION, LOAD_SECONDS_DURATION);
    features.register(ViewerField.FEATURE_BEHAVIOUR_DAY, TimeLoader.day());
    features.register(ViewerField.FEATURE_BEHAVIOUR_DATE_TIME, TimeLoader.dateTime());
    features.register(ViewerField.FEATURE_BEHAVIOUR_URL, LOAD_URL);
  }

  private static final ViewerField FIELD_SEPARATOR = new ViewerField(TypedKey.EMPTY_ARRAY) {
    @Override
    @NotNull
    public List<? extends TableRendererLine> createLeftFieldLines(TableRenderer renderer, RendererContext context) {
      return Collections.singletonList(new SeparatorLine(5));
    }
    @Override
    public RightField createRightField(Configuration settings) {
      return null;
    }

    @Override
    protected boolean canUpdateFrom(ViewerField field) {
      LogHelper.error("Cannot update separator");
      return false;
    }
  };

  private static abstract class GenericLoader implements SerializableFeature<ViewerField> {
    protected static final TypedKey<Long> MODEL_KEY = TypedKey.create("modelKey");
    protected static final TypedKey<String> DISPLAY_NAME = TypedKey.create("displayName");
    protected static final TypedKey<Boolean> HIDE_EMPTY = TypedKey.create("hideEmpty");
    protected static final TypedKey<Boolean> IS_MULTILINE = TypedKey.create("isMultiline");
    protected static final TypedKey<ModelKeyCollector> COLLECTOR = TypedKey.create("modelKeyCollector");
    protected static final TypedKey<?>[] GENERIC_KEYS = {MODEL_KEY, DISPLAY_NAME, HIDE_EMPTY, IS_MULTILINE, COLLECTOR};
    private final TypedKey<?>[] myKeys;

    private GenericLoader(TypedKey<?> ... additionalKeys) {
      myKeys = new TypedKey<?>[GENERIC_KEYS.length + additionalKeys.length];
      System.arraycopy(GENERIC_KEYS, 0, myKeys, 0, GENERIC_KEYS.length);
      System.arraycopy(additionalKeys, 0, myKeys, GENERIC_KEYS.length, additionalKeys.length);
    }

    @Override
    public ViewerField restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      DataHolder data = readStream(reader, stream, invalidate);
      return data != null ? new LoadedViewerField(data) : null;
    }

    private DataHolder readStream(DBReader reader, ByteArray.Stream stream, Procedure<LongList> invalidate) {
      DataHolder data = new DataHolder(myKeys);

      long modelKey = stream.nextLong();
      if (modelKey <= 0) {
        LogHelper.error("Failed to load field", stream);
        return null;
      }
      data.setValue(MODEL_KEY, modelKey);
      data.setValue(DISPLAY_NAME, stream.nextUTF8());
      data.setValue(HIDE_EMPTY, stream.nextBoolean());
      data.setValue(IS_MULTILINE, stream.nextBoolean());
      if (myKeys.length > GENERIC_KEYS.length) loadAdditional(reader, stream, invalidate, data);

      if (!stream.isSuccessfullyAtEnd()) {
        LogHelper.error("Failed to load field", stream);
        return null;
      }

      ModelKeyCollector keys = ModelKeyCollector.getInstance(reader);
      if (keys == null) {
        return null;
      }
      data.setValue(COLLECTOR, keys);
      return data;
    }

    protected void loadAdditional(DBReader reader, ByteArray.Stream stream, Procedure<LongList> invalidate, DataHolder data) {
      LogHelper.error("Should be overridden", this, getClass());
    }

    @Nullable
    protected abstract TableRendererLine createLine(LoadedModelKey<?> key, boolean hideEmpty, String label, TableRenderer renderer, DataHolder data);

    @Override
    public Class<ViewerField> getValueClass() {
      return ViewerField.class;
    }

    private class LoadedViewerField extends ViewerField {
      public LoadedViewerField(DataHolder data) {
        super(data);
      }

      @Override
      @NotNull
      public java.util.List<? extends TableRendererLine> createLeftFieldLines(TableRenderer renderer, RendererContext context) {
        if (Boolean.TRUE.equals(myData.getValue(IS_MULTILINE))) return Collections.emptyList();
        LoadedModelKey<?> key = getModelKey();
        if (key == null) return Collections.emptyList();
        Boolean hide = myData.getValue(HIDE_EMPTY);
        String label = myData.getValue(DISPLAY_NAME) + ":";
        TableRendererLine line = GenericLoader.this.createLine(key, hide, label, renderer, myData);
        return line != null ? Collections.singletonList(line) : Collections.<TableRendererLine>emptyList();
      }

      @Override
      public RightField createRightField(Configuration settings) {
        if (!Boolean.TRUE.equals(myData.getValue(IS_MULTILINE))) return null;
        ModelKey<?> key = getModelKey();
        return key == null ? null : new MultilineTextArea(key, settings, myData.getValue(DISPLAY_NAME));
      }

      @Override
      protected boolean canUpdateFrom(ViewerField field) {
        boolean canUpdate = field instanceof LoadedViewerField;
        LogHelper.assertError(canUpdate, field);
        return canUpdate;
      }

      @Nullable
      private LoadedModelKey<?> getModelKey() {
        ModelKeyCollector collector = myData.getValue(COLLECTOR);
        Long keyItem = myData.getValue(MODEL_KEY);
        LoadedModelKey<?> key = collector.getKey((long) keyItem);
        if(key == null) LogHelper.error("Missing key", keyItem);
        return key;
      }
    }
  }

  private static final SerializableFeature<ViewerField> LOAD_SINGLE_ENUM = new GenericLoader() {
    @Override
    @Nullable
    protected TableRendererLine createLine(LoadedModelKey<?> k, boolean hideEmpty, String label, TableRenderer renderer, DataHolder data) {
      ModelKey<? extends ItemKey> key = k.castScalar(ItemKey.class);
      if (key == null) {
        LogHelper.warning("Not a single enum key", k, label);
        return null;
      }
      TwoColumnLine line = renderer.addLine(label, new TextCell(FontStyle.BOLD, new LeftFieldsBuilder.ItemTextGetter(key)));
      if (hideEmpty) line.setVisibility(new NoEnumValue(key));
      return line;
    }
  };

  private static final TypedKey<Convertor<ItemKey, TableRendererCell>> CELL_CONVERTOR = TypedKey.create("cellConvertor");
  private static final SerializableFeature<ViewerField> LOAD_MULTI_ENUM = new GenericLoader(CELL_CONVERTOR) {
    @Override
    protected TableRendererLine createLine(LoadedModelKey<?> k, boolean hideEmpty, String label, TableRenderer renderer, DataHolder data) {
      ModelKey<? extends Collection<ItemKey>> key = k.castCollection(ItemKey.class);
      Convertor<ItemKey, TableRendererCell> convertor = data.getValue(CELL_CONVERTOR);
      if (convertor == null) convertor = ItemKeyValueListCell.TO_LABEL_CELL;
      if (key == null) {
        LogHelper.warning("Not multi-enum key", k, label);
        return null;
      }
      ItemKeyValueListCell valueCell = new ItemKeyValueListCell(key, renderer, convertor);
      TwoColumnLine line = renderer.addLine(label, valueCell);
      if (hideEmpty) line.setVisibility(new NotEmptyCollection(key));
      return line;
    }

    @Override
    protected void loadAdditional(DBReader reader, ByteArray.Stream stream, Procedure<LongList> invalidate, DataHolder data) {
      //noinspection unchecked
      data.setValue(CELL_CONVERTOR, (Convertor<ItemKey, TableRendererCell>) SerializedObjectAttribute.restore(reader, stream.nextSubstream(), Convertor.class, invalidate));
    }
  };

  private static final SerializableFeature<ViewerField> LOAD_INLINE_MULTI_ENUM = new GenericLoader() {
    @Override
    @Nullable
    protected TableRendererLine createLine(LoadedModelKey<?> k, boolean hideEmpty, String label, TableRenderer renderer, DataHolder data) {
      ModelKey<? extends Collection<ItemKey>> key = k.castCollection(ItemKey.class);
      if (key == null) {
        LogHelper.warning("Not inline multi enum key", k, label);
        return null;
      }
      TwoColumnLine line = renderer.addLine(label, new TextCell(FontStyle.BOLD, new LeftFieldsBuilder.MultiItemTextGetter(key)));
      if (hideEmpty) line.setVisibility(new LeftFieldsBuilder.NotEmptyCollection(key));
      return line;
    }
  };

  private static final SerializableFeature<ViewerField> LOAD_URL = new SimpleField<>(TEXT_TEXT_GETTER, UrlCell.creator, VISIBLE_IF_NOT_EMPTY);
  private static final SerializableFeature<ViewerField> LOAD_TEXT = new SimpleField<>(TEXT_TEXT_GETTER, TEXT_CELL, VISIBLE_IF_NOT_EMPTY);
  private static final SerializableFeature<ViewerField> LOAD_DECIMAL_NOT_ZERO = new SimpleField<>(DECIMAL_TEXT_GETTER, TEXT_CELL, VISIBLE_IF_NOT_ZERO);
  private static final SerializableFeature<ViewerField> LOAD_DECIMAL = new SimpleField<>(DECIMAL_TEXT_GETTER, TEXT_CELL, VISIBLE_IF_NOT_EMPTY);
  private static final SerializableFeature<ViewerField> LOAD_INTEGER = new SimpleField<>(INTEGER_TEXT_GETTER, TEXT_CELL, VISIBLE_IF_NOT_ZERO);
  private static final SerializableFeature<ViewerField> LOAD_SECONDS_DURATION = new SimpleField<>(SECONDS_DURATION_GETTER, TEXT_CELL, VISIBLE_IF_NOT_EMPTY);


  private static class SimpleField<T> extends GenericLoader {
    private final Function<ModelKey<T>, Function<RendererContext, String>> myGetter;
    private final Function<Function<RendererContext, String>, TableRendererCell> myCell;
    private final Function<Function<RendererContext, String>, Function<RendererContext, Boolean>> myVisibility;

    private SimpleField(Function2<ModelKey<T>, RendererContext, String> getter, Function<Function<RendererContext, String>, TableRendererCell> cell,
      Function2<Function<RendererContext, String>, RendererContext, Boolean> visibility)
    {
      myGetter = curry(getter);
      myCell = cell;
      myVisibility = curry(visibility);
    }

    @Override
    protected TableRendererLine createLine(LoadedModelKey<?> key, boolean hideEmpty, String label, TableRenderer renderer, DataHolder data) {
      Function<RendererContext, String> getter = myGetter.invoke((ModelKey<T>)key);
      TwoColumnLine line = renderer.addLine(label, myCell.invoke(getter));
      if (hideEmpty) line.setVisibility(myVisibility.invoke(getter));
      return line;
    }
  }

  private static class NoEnumValue implements Function<RendererContext, Boolean> {
    private final ModelKey<? extends ItemKey> myKey;

    public NoEnumValue(ModelKey<? extends ItemKey> key) {
      myKey = key;
    }

    @Override
    public Boolean invoke(RendererContext context) {
      ItemKey value = getModelValueFromContext(context, myKey);
      return value != null && value.getItem() > 0;
    }
  }

  private static class UrlCell extends TextCell {
    private static final FontStyle URL_STYLE = FontStyle.Decorator.coloredByName(FontStyle.Decorator.underline(FontStyle.PLAIN), "Link.foreground");

    private UrlCell(Function<RendererContext, String> textGetter) {
      super(URL_STYLE, textGetter);
    }

    @Nullable
    public RendererActivity getActivityAt(int id, int x, int y, RendererContext context, Rectangle rectangle) {
      if (id == MouseEvent.MOUSE_CLICKED) {
        return openUrl(context);
      }
      if (id == MouseEvent.MOUSE_MOVED) {
        return ChangeCursorRendererActivity.create(Cursor.HAND_CURSOR);
      }
      return super.getActivityAt(id, x, y, context, rectangle);
    }

    private RendererActivity openUrl(RendererContext context) {
      String url = getText(context);
      ModelMap modelMap = getModelMapFromContext(context);
      LoadedItemServices lis = LoadedItemServices.VALUE_KEY.getValue(modelMap);
      Connection connection = lis == null ? null : lis.getConnection();
      return new ShowUrlMaybeItem(url, lis == null ? null : lis.getActor(ItemUrlService.ROLE), connection == null ? null : connection.getProvider());
    }

    @Nullable
    protected JTextField createLifeComponent(RendererContext context, Rectangle rectangle) {
      return null;
    }

    public static Function<Function<RendererContext, String>, TableRendererCell> creator = new Function<Function<RendererContext, String>, TableRendererCell>() {
      @Override
      public TableRendererCell invoke(Function<RendererContext, String> getter) {
        return new UrlCell(getter);
      }
    };
  }

  private static class MultilineTextArea implements ViewerField.RightField {
    private final LargeTextFormlet myFormlet;
    private final TextController<Object> myController;
    private final ATextArea myArea = new ATextArea();
    private final String myDisplayName;

    MultilineTextArea(ModelKey<?> key, Configuration settings, String displayName) {
      myDisplayName = displayName;
      myController = TextController.anyTextViewer(key, false);
      String id = key.getName();
      myFormlet = new LargeTextFormlet(myArea, myController, settings.getOrCreateSubset(id));
    }

    @Override
    public Formlet getFormlet() {
      return myFormlet;
    }

    @Override
    public void connectUI(Lifespan life, ModelMap model) {
      myController.connectUI(life, model, myArea);
    }

    @Override
    public String getDisplayName() {
      return myDisplayName;
    }
  }


  private static class TimeLoader extends GenericLoader {
    private final boolean myDateOnly;

    private TimeLoader(boolean dateOnly) {
      myDateOnly = dateOnly;
    }

    public static TimeLoader day() {
      return new TimeLoader(true);
    }
    
    public static TimeLoader dateTime() {
      return new TimeLoader(false);
    }
    
    @Override
    protected TableRendererLine createLine(LoadedModelKey<?> key, boolean hideEmpty, String label, TableRenderer renderer, DataHolder data) {
      Function<RendererContext, String> getter = myDateOnly 
        ? new DateTextGetter((ModelKey<Integer>) key)  
        : new DateTimeTextGetter((ModelKey<Date>)key, DateUtil.ShowTime.ALWAYS);
      TwoColumnLine line = renderer.addLine(label, new TextCell(FontStyle.BOLD, getter));
      if (hideEmpty) {
        line.setVisibility(apply2(getter, curry(VISIBLE_IF_NOT_EMPTY)));
      }
      return line;
    }
  }
}
