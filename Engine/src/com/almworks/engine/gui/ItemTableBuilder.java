package com.almworks.engine.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Function2;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererCell;
import com.almworks.util.components.renderer.table.TableRendererLine;
import com.almworks.util.components.renderer.table.TwoColumnLine;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


public abstract class ItemTableBuilder {
  public <T extends ItemKey> void addItem(String label, final ModelKey<T> key) {
    addItem(label, key, null);
  }

  public abstract <T extends ItemKey> void addItem(String label, ModelKey<T> key,
    Function<T, Boolean> visibility);

  public abstract void addString(String label, ModelKey<String> key, boolean hideEmpty);

  public abstract void addString(String label, ModelKey<String> key, Function<String, Boolean> visibility);

  public abstract void addDate(String label, ModelKey<Date> key, boolean hideEmpty, boolean showTime,
    boolean showTimeOnlyIfExists);

  public abstract void addInteger(String label, ModelKey<Integer> key);

  public abstract void addDecimal(String label, ModelKey<BigDecimal> key, boolean hideZeroOrEmpty);

  public abstract void addSeparator();

  public abstract LineBuilder createLineBuilder(String s);

  public abstract void addLine(String label, Function<ModelMap, String> valueGetter, Function<ModelMap, Boolean> visibilityFunction);

  public abstract void addSecondsDuration(String label, ModelKey<Integer> modelKey, boolean hideEmpty);

  public abstract void addStringList(String label, ModelKey<List<String>> key, boolean hideEmpty,
    @Nullable Function<Integer, List<CellAction>> cellActions, @Nullable List<CellAction> aggregateActions,
    @Nullable Function2<ModelMap, String, String> elementConvertor);

  public abstract TwoColumnLine addLine(String label, TableRendererCell cell);

  public abstract TableRenderer getPresentation();

  public abstract TableRendererLine addLine(TableRendererLine line);

  public abstract int getVerticalGap();
}
