package com.almworks.engine.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ModelKey;
import com.almworks.util.commons.Function;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRendererCell;
import com.almworks.util.ui.actions.AnActionListener;

import javax.swing.*;

/**
 *
 */
public interface LineBuilder {
  LineBuilder setLabel(String label);

  LineBuilder setStringValue(ModelKey<String> key, boolean hideEmpty);

  LineBuilder setValueCell(TableRendererCell cell);

  LineBuilder addAction(String tooltip, Icon icon, AnActionListener action);

  void addLine();

  LineBuilder setVisibility(Function<RendererContext, Boolean> visibility);

  LineBuilder setItemValue(ModelKey<ItemKey> key);
  
  LineBuilder setIntegerValue(ModelKey<Integer> key);
}
