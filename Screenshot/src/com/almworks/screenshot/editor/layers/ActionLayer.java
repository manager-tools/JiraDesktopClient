package com.almworks.screenshot.editor.layers;

import com.almworks.screenshot.editor.shapes.AbstractShape;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public interface ActionLayer<S extends AbstractShape> extends Layer {

  void cancel();

  LayerOptions getOptions();

  boolean isModified();

  void setActiveShape(S as);

  void processKeyEvent(KeyEvent e);

  void processMouseEvent(MouseEvent e);
  
  Class getStorageLayerClass();

  StorageLayer getStorageLayer();

  @Nullable
  StorageLayer createStorageLayer();
}
