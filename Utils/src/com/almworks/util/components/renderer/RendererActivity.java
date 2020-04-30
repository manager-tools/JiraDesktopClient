package com.almworks.util.components.renderer;

import com.almworks.util.components.RendererActivityController;
import com.almworks.util.ui.ComponentProperty;

import java.awt.*;

public interface RendererActivity {
  void apply(RendererActivityController controller, Rectangle rectangle);

  <T> void storeValue(ComponentProperty<T> key, T value); 
}
