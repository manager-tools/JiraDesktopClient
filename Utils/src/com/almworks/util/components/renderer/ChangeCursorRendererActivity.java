package com.almworks.util.components.renderer;

import com.almworks.util.components.RendererActivityController;
import com.almworks.util.ui.ComponentProperty;

import java.awt.*;

public class ChangeCursorRendererActivity implements RendererActivity {
  private final Cursor myCursor;

  public ChangeCursorRendererActivity(Cursor cursor) {
    myCursor = cursor;
  }

  public void apply(RendererActivityController controller, Rectangle rectangle) {
    controller.setCursor(myCursor, rectangle);
  }

  public static RendererActivity create(int cursor) {
    return new ChangeCursorRendererActivity(Cursor.getPredefinedCursor(cursor));
  }

  public <T> void storeValue(ComponentProperty<T> key, T value) {
  }
}
