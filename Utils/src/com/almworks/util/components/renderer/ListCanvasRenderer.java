package com.almworks.util.components.renderer;

import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import org.almworks.util.Util;

public class ListCanvasRenderer implements CanvasRenderer {
  private final String mySeparator;
  private final CanvasRenderer myElementRenderer;

  public ListCanvasRenderer(String separator, CanvasRenderer elementRenderer) {
    mySeparator = separator;
    myElementRenderer = elementRenderer;
  }

  @Override
  public void renderStateOn(CellState state, Canvas canvas, Object item) {
    java.util.List list = Util.castNullable(java.util.List.class, item);
    if (list == null || list.isEmpty()) return;
    String sep = "";
    for (Object element : list) {
      canvas.emptySection();
      canvas.appendText(sep);
      sep = mySeparator;
      //noinspection unchecked
      myElementRenderer.renderStateOn(state, canvas, element);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    ListCanvasRenderer other = Util.castNullable(ListCanvasRenderer.class, obj);
    return other != null && Util.equals(mySeparator, other.mySeparator) && Util.equals(myElementRenderer, other.myElementRenderer);
  }

  @Override
  public int hashCode() {
    return Util.hashCode(mySeparator, myElementRenderer) ^ ListCanvasRenderer.class.hashCode();
  }
}
