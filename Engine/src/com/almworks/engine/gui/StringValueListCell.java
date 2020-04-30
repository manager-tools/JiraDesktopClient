package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Function2;
import com.almworks.util.components.renderer.FontStyle;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererCell;
import com.almworks.util.components.renderer.table.TextCell;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StringValueListCell extends AbstractListCell {
  private final ModelKey<List<String>> myKey;
  private final Function<Integer, List<CellAction>> myCellActions;
  @Nullable
  private final Function2<ModelMap, String, String> myElementConvertor;

  /**
   * @param cellActions function for giving actions for i-ths row
   */
  public StringValueListCell(ModelKey<List<String>> key, TableRenderer renderer,
    Function<Integer, List<CellAction>> cellActions, @Nullable Function2<ModelMap, String, String> elementConvertor)
  {
    super(renderer, "cells:" + key.getName());
    myKey = key;
    myCellActions = cellActions;
    myElementConvertor = elementConvertor;
  }

  protected List<TableRendererCell> createCells(RendererContext context) {
    ModelMap modelMap = LeftFieldsBuilder.getModelMapFromContext(context);
    List<String> values = LeftFieldsBuilder.getModelValueFromContext(context, myKey);
    List<TableRendererCell> result = Collections15.arrayList();
    if (values != null) {
      for (int i = 0, valuesSize = values.size(); i < valuesSize; i++) {
        String value = values.get(i);
        if (myElementConvertor != null) value = myElementConvertor.invoke(modelMap, value);
        TableRendererCell cell = TextCell.label(value, FontStyle.BOLD, true);
        if (myCellActions != null) {
          List<CellAction> actions = myCellActions.invoke(i);
          if (actions != null && !actions.isEmpty()) {
            cell = new ActionCellDecorator(cell, actions);
          }
        }
        result.add(cell);
      }
    }
    return result;
  }
} 