package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class ItemRenderer implements CanvasRenderer<ItemKey> {
  private final CanvasRenderable myNull;
  private final CanvasRenderer<ItemKey> myDefaultRenderer;

  public ItemRenderer(@Nullable CanvasRenderable aNull, @Nullable CanvasRenderer<ItemKey> defaultRenderer) {
    myNull = aNull;
    myDefaultRenderer = Util.NN(defaultRenderer, ItemKey.ICON_NAME_RENDERER);
  }

  @Override
  public void renderStateOn(CellState state, Canvas canvas, ItemKey item) {
    if (item == null || BaseSingleEnumEditor.NULL_ITEM == item) {
      if (myNull != null) myNull.renderOn(canvas, state);
      return;
    }
    myDefaultRenderer.renderStateOn(state, canvas, item);
  }
}
