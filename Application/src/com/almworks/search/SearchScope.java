package com.almworks.search;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;

import java.util.Collection;

public abstract class SearchScope implements CanvasRenderable {
  public abstract String getName();

  public abstract Collection<GenericNode> getCurrentScope(UpdateContext context) throws CantPerformException;

  public void renderOn(Canvas canvas, CellState state) {
    canvas.appendText(getName());
  }
}
