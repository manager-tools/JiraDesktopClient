package com.almworks.screenshot.editor.layers;

import com.almworks.screenshot.editor.image.HistoryItem;

import java.awt.*;

public interface LayerImageControl {
  Rectangle getBounds();

  void reportDirty(Shape shape);

  void setCursor(Cursor cursor);

  void cropImage(Rectangle rect);

  void dropActionLayer(ActionLayer layer);

  void setActionLayer(ActionLayer layer);

  void changeActionLayer(ActionLayer layer);

  void addHistoryItem(HistoryItem item);

  StorageLayer getStorageLayerForActionLayer(ActionLayer ActionLayer);

  StorageLayer getStorageLayerForActionLayer();

  Layer getLayer(Class layerClass);

  Rectangle translateToAbs(Rectangle rect);

  Rectangle translateToRel(Rectangle rect);
}
