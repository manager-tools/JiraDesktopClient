package com.almworks.sumtable;

import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.detach.Lifespan;

import java.util.Comparator;

abstract class AxisDefinition implements CanvasRenderable {
  public static final CanvasRenderer<AxisDefinition> RENDERER = new CanvasRenderer<AxisDefinition>() {
    public void renderStateOn(CellState state, Canvas canvas, AxisDefinition item) {
      canvas.appendText(item != null ? item.getName() : "");
    }
  };
  public static final Comparator<AxisDefinition> COMPARATOR = new MyComparator();

  public abstract AListModel<? extends STFilter> createOptionsModel(Lifespan lifespan, ItemHypercube hypercube);

  public abstract boolean equals(Object obj);

  public abstract int hashCode();

  public abstract String getName();

  private static class MyComparator implements Comparator<AxisDefinition> {
    public int compare(AxisDefinition o1, AxisDefinition o2) {
      if (o1 == null)
        return o2 == null ? 0 : -1;
      else
        return o2 == null ? 1 : o1.getName().compareToIgnoreCase(o2.getName());
    }
  }
}
