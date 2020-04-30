package com.almworks.util.ui.widgets.util;

import com.almworks.util.commons.Function2;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CanvasComponent;
import com.almworks.util.components.renderer.CanvasImpl;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.GraphContext;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class CanvasWidget<T> extends LeafRectCell<T> {
  private final CanvasImpl myCanvas = new CanvasImpl();
  private CanvasRenderer<T> myRenderer = Renderers.defaultCanvasRenderer();
  private Function2<CellContext, ? super T, CellState> myStateFactory;
  private T myProtoType;

  public CanvasWidget() {
  }

  public CanvasWidget(@Nullable CanvasRenderer<T> renderer, @Nullable Function2<CellContext, ? super T, CellState> stateFactory) {
    setRenderer(renderer);
    setStateFactory(stateFactory);
  }

  public void setRenderer(CanvasRenderer<T> renderer) {
    myRenderer = Util.NN(renderer, Renderers.<T>defaultCanvasRenderer());
  }

  public void setStateFactory(Function2<CellContext, ? super T, CellState> stateFactory) {
    myStateFactory = stateFactory;
  }

  public void setProtoType(T protoType) {
    myProtoType = protoType;
  }

  @NotNull
  @Override
  protected Dimension getPrefSize(CellContext context, T value) {
    if (myRenderer == null) return new Dimension(0, 0);
    if (value == null) value = myProtoType;
    Dimension result = new Dimension();
    CanvasImpl canvas = render(context, value);
    canvas.getSize(result, createCanvasComponent(context));
    canvas.clear();
    return result;
  }

  @Override
  public void paint(@NotNull GraphContext context, @Nullable T value) {
    CanvasImpl canvas = render(context, value);
    canvas.paint(context.getGraphics(), new Dimension(context.getWidth(), context.getHeight()), createCanvasComponent(context));
    canvas.clear();
  }

  private CanvasComponent.Simple createCanvasComponent(CellContext context) {
    return new CanvasComponent.Simple(context.getHost().getHostComponent());
  }

  private CanvasImpl render(CellContext context, T value) {
    CellState state = getCellState(context, value);
    myCanvas.clear();
    myCanvas.setupProperties(state);
    myRenderer.renderStateOn(state, myCanvas, value);
    return myCanvas;
  }

  private CellState getCellState(CellContext context, T value) {
    if (myStateFactory != null) {
      CellState state = myStateFactory.invoke(context, value);
      if (state != null) return state;
    }
    return CellState.LABEL;
  }
}
