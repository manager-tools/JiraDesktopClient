package com.almworks.engine.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ModelKey;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.renderer.FontStyle;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererCell;
import com.almworks.util.components.renderer.table.TextCell;
import com.almworks.util.components.renderer.table.TextWithIconCell;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ItemKeyValueListCell extends AbstractListCell {
  public static final Convertor<ItemKey, TableRendererCell> TO_LABEL_CELL =
    new Convertor<ItemKey, TableRendererCell>() {
      @Override
      public TableRendererCell convert(ItemKey value) {
        return TextCell.label(value.getDisplayName(), FontStyle.BOLD, true);
      }
    };
  public static final Convertor<ItemKey, TableRendererCell> TO_ICON_LABEL =
    new Convertor<ItemKey, TableRendererCell>() {
      @Override
      public TableRendererCell convert(ItemKey value) {
        return new TextWithIconCell(FontStyle.BOLD, value.getDisplayName(), value.getIcon());
      }
    };

  private final ModelKey<? extends Collection<? extends ItemKey>> myKey;
  private final Convertor<ItemKey,TableRendererCell> myCellConvertor;

  public ItemKeyValueListCell(@NotNull ModelKey<? extends Collection<? extends ItemKey>> key, @NotNull TableRenderer renderer, @NotNull Convertor<ItemKey,TableRendererCell> cellConvertor) {
    super(renderer, "cells:" + key.getName());
    myKey = key;
    myCellConvertor = cellConvertor;
  }

  @Override
  protected List<TableRendererCell> createCells(RendererContext context) {
    Collection<? extends ItemKey> values = LeftFieldsBuilder.getModelValueFromContext(context, myKey);
    if(values == null || values.isEmpty()) {
      return Collections.emptyList();
    }
    return myCellConvertor.collectList(values);
  }
}
