package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.ConstraintDescriptorProxy;
import com.almworks.api.application.qb.ConstraintFilterNode;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public interface DatePreset {
  ComboBoxConstraintKey<DatePreset> KEY = new DatePresetKey();
  CanvasRenderer<DatePreset> RENDERER = new Renderer();

  void write(@NotNull String conditionId, @NotNull FormulaWriter writer);

  @NotNull
  String getDisplayString();

  PropertyMap getValues();

  class RelativeInteval implements DatePreset {
    public static final List<RelativeInteval> KNOWN_PRESETS;

    static final RelativeInteval TODAY = new RelativeInteval("Today", 0, 0, DateUnit.DAY);
    static final RelativeInteval YESTERDAY = new RelativeInteval("Yesterday", 1, 1, DateUnit.DAY);
    static final RelativeInteval LAST_7_DAYS = new RelativeInteval("Last 7 days", 6, 0, DateUnit.DAY);
    static final RelativeInteval THIS_WEEK = new RelativeInteval("This week", 0, 0, DateUnit.WEEK);
    static final RelativeInteval LAST_WEEK = new RelativeInteval("Last week", 1, 1, DateUnit.WEEK);
    static final RelativeInteval THIS_MONTH = new RelativeInteval("This month", 0, 0, DateUnit.MONTH);
    static final RelativeInteval LAST_MONTH = new RelativeInteval("Last month", 1, 1, DateUnit.MONTH);

    private final String myDisplayString;
    private final int myAfter;
    private final int myBefore;
    private final DateUnit myUnit;
    private static final String DURING_TOKEN = " during ";
    public static final AListModel<RelativeInteval> ALL_INTERVALS;

    public RelativeInteval(@NotNull String displayString, int after, int before, @NotNull DateUnit unit) {
      assert after >= 0 : after;
      assert before >= 0 : before;
      assert after >= before : after + " " + before;
      myDisplayString = displayString;
      myAfter = after;
      myBefore = before;
      myUnit = unit;
    }

    public void write(String conditionId, FormulaWriter writer) {
      writer = writer.createChild();
      writer.addToken(conditionId);
      writer.addRaw(DURING_TOKEN);
      writer.addToken(String.valueOf(DateUnit.RelativeDate.getUnitChar(myUnit)));
      writer.addRaw(" ");
      writer.addToken(String.valueOf(myAfter));
      writer.addRaw(" ");
      writer.addToken(String.valueOf(myBefore));
    }

    @NotNull
    public String getDisplayString() {
      return myDisplayString;
    }

    public PropertyMap getValues() {
      PropertyMap values = new PropertyMap();
      setValues(values, DateBoundParams.AFTER);
      setValues(values, DateBoundParams.BEFORE);
      return values;
    }

    private void setValues(PropertyMap values, DateBoundParams params) {
      boolean before = params.isLaterBound();
      int value = before ? myBefore : myAfter;
      params.setInitialValue(new DateUnit.RelativeDate(value, myUnit, before), values);
    }

    public static void register(TokenRegistry<FilterNode> registry) {
      registry.registerInfixConstraint(DURING_TOKEN.trim(), new InfixParser<FilterNode>() {
        public FilterNode parse(ParserContext<FilterNode> left, ParserContext<FilterNode> right) throws ParseException {
          List<String> tokens = right.getAllTokens();
          if (tokens.size() != 3)
            throw right.createException("Expected: <unit> <after> <before>", -1, -1);
          String unitString = tokens.get(0);
          if (unitString.length() != 1)
            throw right.createException("Wrong unit: " + unitString, 0, 1);
          DateUnit unit = DateUnit.parseUnit(unitString.charAt(0));
          if (unit == null)
            throw right.createException("Wrong unit: " + unitString, 0, 1);
          DateUnit.RelativeDate after;
          DateUnit.RelativeDate before;
          try {
            after = DateUnit.RelativeDate.parseRelative(unit, tokens.get(1), false);
            before = DateUnit.RelativeDate.parseRelative(unit, tokens.get(2), true);
          } catch (java.text.ParseException e) {
            throw new ParseException("Integer expected");
          }
          RelativeInteval knownInterval = findKnownInterval(after.getRelative(), before.getRelative(), unit);
          return new ConstraintFilterNode(ConstraintDescriptorProxy.stub(left.getSingle(), DateAttribute.CONSTRAINT_TYPE),
            DateAttribute.createValues(before, after, knownInterval));
        }
      });
    }

    @Nullable
    private static RelativeInteval findKnownInterval(int after, int before, DateUnit unit) {
      for (RelativeInteval inteval : KNOWN_PRESETS)
        if (inteval.myAfter == after && inteval.myBefore == before && inteval.myUnit == unit)
          return inteval;
      return null;
    }


    static {
      List<RelativeInteval> presets = Collections15.arrayList();
      presets.add(TODAY);
      presets.add(YESTERDAY);
      presets.add(LAST_7_DAYS);
      presets.add(THIS_WEEK);
      presets.add(LAST_WEEK);
      presets.add(THIS_MONTH);
      presets.add(LAST_MONTH);
      KNOWN_PRESETS = Collections.unmodifiableList(presets);
      ALL_INTERVALS = FixedListModel.create(RelativeInteval.KNOWN_PRESETS);
    }
  }


  public static class DatePresetKey extends ComboBoxConstraintKey<DatePreset> {
    public DatePresetKey() {
      super("datePreset");
    }

    @NotNull
    protected AListModel<RelativeInteval> getVariantsModel() {
      return RelativeInteval.ALL_INTERVALS;
    }

    protected DatePreset getDefaultSelection() {
      return null;
    }
  }


  public static class Renderer implements CanvasRenderer<DatePreset> {
    public void renderStateOn(CellState state, Canvas canvas, DatePreset preset) {
      canvas.appendText(preset != null ? preset.getDisplayString() : "");
    }
  }
}
