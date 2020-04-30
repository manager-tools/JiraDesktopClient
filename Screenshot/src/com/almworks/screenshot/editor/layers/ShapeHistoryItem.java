package com.almworks.screenshot.editor.layers;

import com.almworks.screenshot.editor.image.HistoryItem;
import com.almworks.screenshot.editor.shapes.AbstractShape;
import org.jetbrains.annotations.Nullable;

public class ShapeHistoryItem<S extends AbstractShape> implements HistoryItem {
  protected StorageLayer myStorageLayer;
  protected ActionLayer<S> myActionLayer;
  protected S myOldShape;
  protected S myNewShape;

  protected LayerImageControl myLayerImageControl;

  public ShapeHistoryItem(ActionLayer<S> myActionLayer, LayerImageControl myLayerImageControl, @Nullable S myNewShape, @Nullable S myOldShape) {
    this.myActionLayer = myActionLayer;
    this.myLayerImageControl = myLayerImageControl;
    this.myNewShape = myNewShape;
    this.myOldShape = myOldShape;
    this.myStorageLayer = myActionLayer.getStorageLayer();
  }

  public void undo() {
    if (myNewShape != null) {
      myStorageLayer.unstore(myNewShape);
      myActionLayer.setActiveShape(null);
    }

    if (myOldShape != null) {
      myStorageLayer.store(myOldShape);
    }
    myLayerImageControl.reportDirty(myLayerImageControl.getBounds());
  }

  public void redo() {
    if (myOldShape != null) {
      myStorageLayer.unstore(myOldShape);
      myActionLayer.setActiveShape(null);
    }

    if (myNewShape != null) {
      myStorageLayer.store(myNewShape);
    }
    myLayerImageControl.reportDirty(myLayerImageControl.getBounds());
  }
}
