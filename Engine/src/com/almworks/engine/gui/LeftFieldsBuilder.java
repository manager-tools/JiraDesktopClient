package com.almworks.engine.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.api.engine.Connection;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Function2;
import com.almworks.util.components.renderer.FontStyle;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.RendererHostComponent;
import com.almworks.util.components.renderer.table.*;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;
import org.almworks.util.StringUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.almworks.util.collections.Functional.curry;

/**
 * @author dyoma
 */
public class LeftFieldsBuilder extends ItemTableBuilder {
  public static final TypedKey<ModelMap> MODEL_MAP = TypedKey.create("modelMap");
  public static final UIController<RendererHostComponent> CONTROLLER = new RendererHostConstroller();
  private final TableRenderer myRenderer = new TableRenderer();

  public TableRenderer getPresentation() {
    return myRenderer;
  }

  public LineBuilder createLineBuilder(String label) {
    LineBuilder builder = new LineBuilderImpl(myRenderer);
    builder.setLabel(label);
    return builder;
  }


  public void addLine(String s, final Function<ModelMap, String> valueGetter,
    final Function<ModelMap, Boolean> visibilityFunction)
  {
    TwoColumnLine line = addLine(s, new TextCell(FontStyle.BOLD, new ModelMapFunctionAdapter<String>(valueGetter)));
    line.setVisibility(new ModelMapFunctionAdapter<Boolean>(visibilityFunction));
  }


  public <T extends ItemKey> void addItem(String label, ModelKey<T> key, Function<T, Boolean> visibility) {
    TwoColumnLine line = myRenderer.addLine(label, new TextCell(FontStyle.BOLD, new ItemTextGetter(key)));
    if (visibility != null)
      line.setVisibility(new VisibleAdapter(key, visibility));
  }


  public void addString(String label, final ModelKey<String> key, boolean hideEmpty) {
    addSimple(label, key, hideEmpty, TEXT_TEXT_GETTER, TEXT_CELL, VISIBLE_IF_NOT_EMPTY);
  }


  public void addString(String label, final ModelKey<String> key, Function<String, Boolean> visibility) {
    TwoColumnLine line = addSimple(label, key, false, TEXT_TEXT_GETTER, TEXT_CELL, null);
    if (visibility != null) line.setVisibility(new VisibleAdapter(key, visibility));
  }

  @Override
  public void addStringList(String label, ModelKey<List<String>> key, boolean hideEmpty,
    @Nullable Function<Integer, List<CellAction>> cellActions, @Nullable List<CellAction> aggregateActions,
    @Nullable Function2<ModelMap, String, String> elementConvertor)
  {
    TableRendererCell cell = new StringValueListCell(key, myRenderer, cellActions, elementConvertor);
    if (aggregateActions != null) {
      cell = new ActionCellDecorator(cell, aggregateActions);
    }
    TwoColumnLine line = myRenderer.addLine(label, cell);
    if (hideEmpty) {
      line.setVisibility(new NotEmptyCollection(key));
    }
  }

  public void addDate(String label, final ModelKey<Date> key, boolean hideEmpty, boolean showTime,
    boolean showTimeOnlyIfExists)
  {
    final DateTimeTextGetter getter = new DateTimeTextGetter(key, showTime, showTimeOnlyIfExists);
    TwoColumnLine line = myRenderer.addLine(label, new TextCell(FontStyle.BOLD, getter));
    if (hideEmpty) {
      line.setVisibility(curry(VISIBLE_IF_NOT_EMPTY).invoke(getter));
    }
  }

  public void addSecondsDuration(String label, ModelKey<Integer> modelKey, boolean hideEmpty) {
    addSimple(label, modelKey, hideEmpty, SECONDS_DURATION_GETTER, TEXT_CELL, VISIBLE_IF_NOT_EMPTY);
  }

  public void addInteger(String label, final ModelKey<Integer> key) {
    addSimple(label, key, true, INTEGER_TEXT_GETTER, TEXT_CELL, VISIBLE_IF_NOT_ZERO);
  }

  public void addDecimal(String label, final ModelKey<BigDecimal> key, boolean hideZeroOrEmpty) {
    addSimple(label, key, hideZeroOrEmpty, DECIMAL_TEXT_GETTER, TEXT_CELL, VISIBLE_IF_NOT_ZERO);
  }

  private <T> TwoColumnLine addSimple(String label, ModelKey<T> key, boolean hideEmpty,
    Function2<ModelKey<T>, RendererContext, String> getterF,
    Function<Function<RendererContext, String>, TableRendererCell> cellF,
    Function2<Function<RendererContext, String>, RendererContext, Boolean> visibilityF)
  {
    Function<RendererContext, String> getter = curry(getterF).invoke(key);
    TwoColumnLine line = myRenderer.addLine(label, cellF.invoke(getter));
    if (hideEmpty) line.setVisibility(curry(visibilityF).invoke(getter));
    return line;
  }

  @Override
  public TableRendererLine addLine(TableRendererLine line) {
    return myRenderer.addLine(line);
  }

  @Override
  public int getVerticalGap() {
    return myRenderer.getVerticalGap();
  }

  public TwoColumnLine addLine(String label, TableRendererCell cell) {
    return myRenderer.addLine(label, cell);
  }

  public void addSeparator() {
    myRenderer.addLine(new SeparatorLine(5));
  }

  protected TableRenderer getRenderer() {
    return myRenderer;
  }

  private static class RendererHostConstroller implements UIController<RendererHostComponent> {
    public void connectUI(@NotNull Lifespan lifespan, @NotNull ModelMap model,
      @NotNull final RendererHostComponent component)
    {
      component.putValue(lifespan, MODEL_MAP, model);
      ChangeListener listener = new ChangeListener() {
        public void onChange() {
          component.invalidateRenderer();
        }
      };
      model.addAWTChangeListener(lifespan, listener);
      listener.onChange();
    }
  }


  public static class LineBuilderImpl implements LineBuilder {
    private final TableRenderer myRenderer;
    private final java.util.List<CellAction> myActions = Collections15.arrayList();
    private TableRendererCell myValueCell;
    private Function<RendererContext, Boolean> myVisibility;
    private TableRendererCell myLabelCell;

    public LineBuilderImpl(TableRenderer renderer) {
      myRenderer = renderer;
    }

    public LineBuilder setLabel(String label) {
      assert label != null;
      return setLabelCell(TextCell.label(label));
    }

    private LineBuilder setLabelCell(TableRendererCell cell) {
      assert cell != null;
      myLabelCell = cell;
      return this;
    }

    public LineBuilder setStringValue(ModelKey<String> key, boolean hideEmpty) {
      Function<RendererContext, String> getter = curry(TEXT_TEXT_GETTER).invoke(key);
      myValueCell = new TextCell(FontStyle.BOLD, getter);
      if (hideEmpty)
        myVisibility = curry(VISIBLE_IF_NOT_EMPTY).invoke(getter);
      return this;
    }

    public LineBuilder setValueCell(TableRendererCell cell) {
      myValueCell = cell;
      return this;
    }

    public LineBuilder addAction(String tooltip, Icon icon, AnActionListener action) {
      myActions.add(new CellAction(icon, tooltip, action));
      return this;
    }

    public void addLine() {
      assert myLabelCell != null;
      assert myValueCell != null;
      if (!myActions.isEmpty()) {
        myValueCell = new ActionCellDecorator(myValueCell, myActions);
      }
      TwoColumnLine line = new TwoColumnLine(myLabelCell, myValueCell, myRenderer);
      myRenderer.addLine(line);
      if (myVisibility != null)
        line.setVisibility(myVisibility);
    }

    public LineBuilder setVisibility(Function<RendererContext, Boolean> visibility) {
      myVisibility = visibility;
      return this;
    }

    public LineBuilder setItemValue(ModelKey<ItemKey> key) {
      myValueCell = new TextCell(FontStyle.BOLD, new ItemTextGetter(key));
      return this;
    }

    public LineBuilder setIntegerValue(ModelKey<Integer> key) {
      myValueCell = new TextCell(FontStyle.BOLD, curry(INTEGER_TEXT_GETTER).invoke(key));
      return this;
    }
  }


  public static class NotEmptyCollection implements Function<RendererContext, Boolean> {
    private final ModelKey<? extends Collection<?>> myKey;

    public NotEmptyCollection(@NotNull ModelKey<? extends Collection<?>> key) {
      myKey = key;
    }

    public Boolean invoke(RendererContext context) {
      Collection<?> value = getModelValueFromContext(context, myKey);
      return value != null && value.size() > 0;
    }
  }


  public static abstract class Action<T> implements AnActionListener {
    private final ModelKey<T> myKey;

    public Action(ModelKey<T> key) {
      myKey = key;
    }

    public void perform(ActionContext context) throws CantPerformException {
      RendererContext rendererContext = context.getSourceObject(RendererContext.RENDERER_CONTEXT);
      T value = getModelValueFromContext(rendererContext, myKey);
      if (value == null)
        return;
      LoadedItemServices lis = getModelValueFromContext(rendererContext, LoadedItemServices.VALUE_KEY);
      Connection connection = lis.getConnection();
      if (connection == null) {
        return;
      }
      act(context, rendererContext, connection, value);
    }

    protected abstract void act(ActionContext context, RendererContext rendererContext, @NotNull Connection connection,
      @NotNull T value) throws CantPerformException;
  }

  @Nullable
  public static ModelMap getModelMapFromContext(RendererContext context) {
    return context.getValue(MODEL_MAP);
  }

  public static <T> T getModelValueFromContext(RendererContext context, ModelKey<T> modelKey) {
    ModelMap modelMap = getModelMapFromContext(context);
    return modelMap != null ? modelKey.getValue(modelMap) : null;
  }

  public static boolean hasModelValueFromContext(RendererContext context, ModelKey<?> key) {
    ModelMap modelMap = getModelMapFromContext(context);
    return modelMap != null && key.hasValue(modelMap);
  }

  public static Function2<ModelKey<String>, RendererContext, String> TEXT_TEXT_GETTER = new Function2<ModelKey<String>, RendererContext, String>() {
    public String invoke(ModelKey<String> key, RendererContext argument) {
      return getModelValueFromContext(argument, key);
    }
  };

  public static class DateTimeTextGetter implements Function<RendererContext, String> {
    private final ModelKey<Date> myKey;
    private final DateUtil.ShowTime myShowTime;

    public static DateTimeTextGetter timeIfExists(ModelKey<Date> key) {
      return new DateTimeTextGetter(key, DateUtil.ShowTime.IF_EXISTS);
    }

    @Deprecated
    public DateTimeTextGetter(ModelKey<Date> key, boolean showTime, boolean showTimeOnlyIfExists) {
      this(key, showTime ? (showTimeOnlyIfExists ? DateUtil.ShowTime.IF_EXISTS : DateUtil.ShowTime.ALWAYS) : DateUtil.ShowTime.NEVER);
    }

    public DateTimeTextGetter(ModelKey<Date> key, DateUtil.ShowTime showTime) {
      myKey = key;
      myShowTime = showTime;
    }

    public String invoke(RendererContext context) {
      return DateUtil.toLocalString(getModelValueFromContext(context, myKey), myShowTime);
    }
  }
  
  public static class DateTextGetter implements Function<RendererContext, String> {
    private final ModelKey<Integer> myKey;

    public DateTextGetter(ModelKey<Integer> key) {
      myKey = key;
    }

    @Override
    public String invoke(RendererContext context) {
      Integer nDaysFromEpoch = getModelValueFromContext(context, myKey);
      if (nDaysFromEpoch == null) return "";
      Date date = DateUtil.toInstantOnDay(nDaysFromEpoch);
      return DateUtil.toLocalString(date, DateUtil.ShowTime.NEVER);
    }
  };

  public static final Function2<ModelKey<Integer>, RendererContext, String> INTEGER_TEXT_GETTER = new Function2<ModelKey<Integer>, RendererContext, String>() {
    public String invoke(ModelKey<Integer> key, RendererContext context) {
      Integer integer = getModelValueFromContext(context, key);
      return integer != null ? integer.toString() : "";
    }
  };
  public static final Function2<ModelKey<Integer>, RendererContext, String> SECONDS_DURATION_GETTER = new Function2<ModelKey<Integer>, RendererContext, String>() {
    public String invoke(ModelKey<Integer> key, RendererContext context) {
      Integer seconds = getModelValueFromContext(context, key);
      if (seconds == null)
        return "";
      return DateUtil.getFriendlyDuration(seconds, true);
    }
  };
  public static final Function2<ModelKey<BigDecimal>, RendererContext, String> DECIMAL_TEXT_GETTER = new Function2<ModelKey<BigDecimal>, RendererContext, String>() {
    public String invoke(ModelKey<BigDecimal> key, RendererContext context) {
      BigDecimal value = getModelValueFromContext(context, key);
      return value != null ? TextUtil.bigDecimalToString(value) : "";
    }
  };
  
  public static final Function<Function<RendererContext, String>, TableRendererCell> TEXT_CELL = new Function<Function<RendererContext, String>, TableRendererCell>() {
    @Override
    public TableRendererCell invoke(Function<RendererContext, String> getter) {
      return new TextCell(FontStyle.BOLD, getter);
    }
  };

  public static final Function2<Function<RendererContext, String>, RendererContext, Boolean> VISIBLE_IF_NOT_EMPTY = new Function2<Function<RendererContext, String>, RendererContext, Boolean>() {
    public Boolean invoke(Function<RendererContext, String> getter, RendererContext argument) {
      String value = getter.invoke(argument);
      return value != null && value.length() > 0;
    }
  };
  public static final Function2<Function<RendererContext, String>, RendererContext, Boolean> VISIBLE_IF_NOT_ZERO = new Function2<Function<RendererContext, String>, RendererContext, Boolean>() {
    public Boolean invoke(Function<RendererContext, String> getter, RendererContext argument) {
      String value = getter.invoke(argument);
      return value != null && value.length() > 0 && !value.equals("0");
    }
  };


  private static class VisibleAdapter<T> implements Function<RendererContext, Boolean> {
    private final Function<T, Boolean> myVisibility;
    private final ModelKey<T> myKey;

    public VisibleAdapter(ModelKey<T> key, Function<T, Boolean> visibility) {
      myVisibility = visibility;
      myKey = key;
    }

    public Boolean invoke(RendererContext argument) {
      T value = getModelValueFromContext(argument, myKey);
      return myVisibility.invoke(value);
    }
  }


  public static class ItemTextGetter implements Function<RendererContext, String> {
    private final ModelKey<? extends ItemKey> myKey;

    public ItemTextGetter(@NotNull ModelKey<? extends ItemKey> key) {
      myKey = key;
    }

    public String invoke(RendererContext context) {
      ItemKey value = getModelValueFromContext(context, myKey);
      return value != null ? value.getDisplayName() : null;
    }
  }

  
  public static class MultiItemTextGetter implements Function<RendererContext, String> {
    private final ModelKey<? extends Collection<ItemKey>> myKey;

    public MultiItemTextGetter(@NotNull ModelKey<? extends Collection<ItemKey>> key) {
      myKey = key;
    }

    @Override
    public String invoke(RendererContext context) {
      final Collection<ItemKey> keys = getModelValueFromContext(context, myKey);
      if(keys == null || keys.isEmpty()) {
        return null;
      }
      final List<String> names = ItemKey.DISPLAY_NAME.collectList(keys);
      return StringUtil.implode(names, " ");
    }
  }


  private class ModelMapFunctionAdapter<M> implements Function<RendererContext, M> {
    private final Function<ModelMap, M> myDelegate;

    private ModelMapFunctionAdapter(Function<ModelMap, M> delegate) {
      myDelegate = delegate;
    }

    public M invoke(RendererContext argument) {
      ModelMap modelMap = getModelMapFromContext(argument);
      return modelMap != null ? myDelegate.invoke(modelMap) : null;
    }
  }
}