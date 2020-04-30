package com.almworks.screenshot.editor.layers;

import com.almworks.screenshot.editor.tools.blur.BlurStorageLayer;

import java.util.Comparator;

public class LayerComparator implements Comparator<Layer> {
  //This comparator's ordering is inconsistent with equals()
  public int compare(Layer layer1, Layer layer2) {
    int i1 = sortNumber(layer1);
    int i2 = sortNumber(layer2);
    return (i1 < i2 ? -1 : (i1 == i2 ? 0 : 1));
  }

  private int sortNumber(Layer layer) {

    if (layer instanceof BackgroundLayer) return 1;
//    if (layer instanceof PastedToolLayer) return 2;
    if (layer instanceof BlurStorageLayer) return 3;

    if (layer instanceof MultipleShapeStorageLayer) return 4;
//    if (layer instanceof AnnotationToolLayer) return 1;
//    if (layer instanceof CropToolLayer) return 5;
    return 256;
  }
}
