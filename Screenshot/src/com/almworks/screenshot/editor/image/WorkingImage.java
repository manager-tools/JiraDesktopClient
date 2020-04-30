package com.almworks.screenshot.editor.image;

import com.almworks.screenshot.editor.layers.*;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.SortedSet;

/**
 * Layered image:
 * Background Layer
 * BlurStorageLayer
 * MultiShapeStorageLayer
 * ActionLayer
 */
public class WorkingImage extends SimpleModifiable implements LayerImageControl {
  private static final Color BACKGROUND_COLOR = Color.WHITE;
  public static final DataRole<History> HISTORY_ROLE = DataRole.createRole(History.class);

  private final DirtyRegionsCollectorImpl myDirtyCollector = new DirtyRegionsCollectorImpl();

  private final BackgroundLayer myBackgroundLayer;

  private final SortedSet<StorageLayer> myStorageLayers = Collections15.treeSet(new LayerComparator());

  private final BasicScalarModel<ActionLayer> mySelectedLayer = BasicScalarModel.createWithValue(null, true);

  private AffineTransform myTransform;

  private BasicScalarModel<Double> myScale;

  /**
   * Cache of layers in Z-order, starting from the background image.
   */
  private Layer[] myLayersCacheZ = null;

  private RepaintSink[] myRepaintSinks = null;

  private ActionLayer myActionLayer = null;

  private Rectangle myBounds;

  private Rectangle myScaledBounds;

  private BufferedImage myImage;

  private Cursor myCursor;

  private ImageComponentControl myComponentControl;

  private final History myHistory = new History();

  public WorkingImage(BufferedImage sourceImage) {
    myBounds = new Rectangle(sourceImage.getWidth(), sourceImage.getHeight());
    myBackgroundLayer = new BackgroundLayer(sourceImage, this);
    myScale = BasicScalarModel.createWithValue(1.0d, true);
  }

  private void updateImage() {
    Rectangle oldBounds = (myScaledBounds != null) ? new Rectangle(myScaledBounds) : null;
    double scale = myScale.getValue();
    myScaledBounds = new Rectangle((int) Math.round(myBounds.x * scale), (int) Math.round(myBounds.y * scale),
      (int) Math.round(myBounds.width * scale), (int) Math.round(myBounds.height * scale));
    myDirtyCollector.clear();
    paintDirty(null, null);
    onResize(oldBounds, myBounds); //todo fix oldbounds wrong
  }

  private void onResize(Rectangle oldBounds, Rectangle newBounds) {
    myComponentControl.imageResized(oldBounds, newBounds);
    for (StorageLayer myStorageLayer : myStorageLayers) {
      myStorageLayer.resize(oldBounds, newBounds);
    }
    if (myActionLayer != null) {
      myActionLayer.resize(oldBounds, newBounds);
    }
  }

  public Dimension getSize() {
    return new Dimension(myBounds.getSize());
  }

  public void setComponentControl(ImageComponentControl componentControl) {
    assert componentControl != null;
    assert myComponentControl == null;
    myComponentControl = componentControl;
  }

  public int getHeight() {
    return myScaledBounds.height;
  }

  public int getWidth() {
    return myScaledBounds.width;
  }

  public void paintDirty(Rectangle imageClip, Rectangle absClip) {
    Area paintArea;

    if (myImage == null || imageClip == null) {
      myImage = new BufferedImage(myScaledBounds.width, myScaledBounds.height, BufferedImage.TYPE_INT_RGB);
      myDirtyCollector.dirty(myBounds);
      paintArea = myDirtyCollector.beginPaint(myBounds);
    } else {
      //restoring of original abotete cliping coordinates
      if (absClip == null) {
        paintArea = myDirtyCollector.beginPaint(translateToAbs(imageClip));
      } else {
        paintArea = myDirtyCollector.beginPaint(absClip);
      }
    }
    if (!paintArea.isEmpty()) {
      Graphics2D g2 = (Graphics2D) myImage.getGraphics();
      g2.transform(myTransform);

      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, (myScale.getValue() < 1.0f) ?
        RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      g2.setClip(paintArea);
      g2.setColor(BACKGROUND_COLOR);

      for (Layer layer : getLayersZ()) {
        paintLayer(layer, g2, paintArea);
      }

      myDirtyCollector.endPaint(paintArea);
    }
  }

  private void paintLayer(Layer layer, Graphics2D g2, Area paintArea) {
    layer.paint(g2, paintArea);
  }

  public BufferedImage getImage() {
    if (myImage == null)
      paintDirty(null, null);
    return myImage;
  }

  public BufferedImage getInitialSubImage(Rectangle rect) {
    return myBackgroundLayer.getSubimage(rect);
  }

  public Modifiable getModifiableActionLayer() {
    return this;
  }

  public ActionLayer getActionLayer() {
    return myActionLayer;
  }

  public void dropActionLayer() {
    setActionLayer(null);
  }

  public History getHistory() {
    return myHistory;
  }

  private void requestRepaint(Rectangle bounds) {
    RepaintSink[] sinks = myRepaintSinks;
    if (sinks == null)
      return;
    for (RepaintSink sink : sinks) {
      if (sink != null)
        sink.requestRepaint(bounds);
    }
  }

  public synchronized Detach addRepaintSink(final RepaintSink sink) {
    assert sink != null;
    if (sink == null)
      return Detach.NOTHING;
    final int index;
    if (myRepaintSinks == null) {
      myRepaintSinks = new RepaintSink[] {sink};
      index = 0;
    } else {
      RepaintSink[] sinks = myRepaintSinks;
      RepaintSink[] newSinks = new RepaintSink[sinks.length + 1];
      System.arraycopy(sinks, 0, newSinks, 0, sinks.length);
      index = newSinks.length - 1;
      newSinks[index] = sink;
      myRepaintSinks = newSinks;
    }
    return new Detach() {
      protected void doDetach() {
        synchronized (WorkingImage.this) {
          if (index >= 0 && index < myRepaintSinks.length) {
            if (myRepaintSinks[index] == sink) {
              myRepaintSinks[index] = null;
            }
          }
        }
      }
    };
  }

  public void changeActionLayer(ActionLayer newLayer) {
    ActionLayer actionLayer = getActionLayer();

    if (newLayer != actionLayer && actionLayer != null && actionLayer.isModified()) {
      actionLayer.cancel();
      dropActionLayer();
    }
    setActionLayer(newLayer);
  }

  public void setActionLayer(ActionLayer layer) {
    ActionLayer oldLayer = myActionLayer;
    myActionLayer = layer;
    invalidateLayersCache();
    if (!Util.equals(oldLayer, layer)) {
      Rectangle oldBounds = oldLayer != null ? oldLayer.getBounds() : null;
      Rectangle newBounds = layer != null ? layer.getBounds() : null;
      Rectangle bounds = newBounds == null ? oldBounds : (oldBounds == null ? newBounds : newBounds.union(oldBounds));
      if (bounds != null) {
        myDirtyCollector.dirty(bounds);
        requestRepaint(bounds);
      }
      fireChanged();
      mySelectedLayer.setValue(myActionLayer);
    }
  }

  private void invalidateLayersCache() {
    myLayersCacheZ = null;
  }

  public void invalidateImage() {
    myImage = null;
  }

  public Rectangle getBounds() {
    return new Rectangle(myBounds);
  }


  /**
   * Translates a Rectangle to absolute coords.
   */
  public Rectangle translateToAbs(Rectangle rect) {
    try {
      return myTransform.createInverse().createTransformedShape(rect).getBounds();
    } catch (NoninvertibleTransformException e) {
      Log.warn(e);
      return null;
    }
  }

  /**
   * Translates a Rectangle to relative coords. The result is an intersection of
   * this translation and current WorkingImage. If there is no intersection, it returns an empty Rectangle.
   */
  public Rectangle translateToRel(Rectangle rect) {
    return myTransform.createTransformedShape(rect).getBounds();
  }

  public void dispatch(MouseEvent e) {

    double inverseScale = (1 / myScale.getValue() - 1);

    e.translatePoint((myScaledBounds.x), (myScaledBounds.y));
    e.translatePoint((int) (e.getPoint().x * inverseScale), (int) (e.getPoint().y * inverseScale));

    myCursor = null;

    if (e.getID() == MouseEvent.MOUSE_PRESSED) {
      requestFocus();
    }

    dispatchToLayers(e);
    if (myComponentControl != null)
      myComponentControl.setComponentCursor(myCursor);
  }

  private void dispatchToLayers(MouseEvent e) {
    if (e.isConsumed())
      return;
    if (myActionLayer != null) {
      myActionLayer.processMouseEvent(e);
      if (e.isConsumed())
        return;
    }
  }

  public void processKeyEvent(KeyEvent e) {
    if (e.isConsumed())
      return;
    myCursor = null;
    /*Layer selectedLayer = mySelectedLayer.getValue();
    if (selectedLayer != null)
      selectedLayer.processKeyEvent(e);*/
    if (myActionLayer != null) {
      myActionLayer.processKeyEvent(e);
    }
    if (myCursor != null)
      myComponentControl.setComponentCursor(myCursor);
  }

  private Layer[] getLayersZ() {
    if (myLayersCacheZ == null) {
      java.util.List<Layer> list = Collections15.arrayList();
      list.add(myBackgroundLayer);
      list.addAll(myStorageLayers);
      if (myActionLayer != null)
        list.add(myActionLayer);
      myLayersCacheZ = list.toArray(new Layer[list.size()]);
    }
    return myLayersCacheZ;
  }

  public LayerImageControl getLayerControl() {
    return this;
  }

  public void reportDirty(Shape shape) {
    myDirtyCollector.dirty(shape);
    requestRepaint(shape.getBounds());
  }

  public Cursor getCursor() {
    return myCursor;
  }

  public void setCursor(Cursor cursor) {
    myCursor = cursor;
  }

  /**
   * @param rect must be in "absolute" coordinates
   */
  public void cropImage(Rectangle rect) {
    assert rect != null;
    if (myBounds.equals(rect))
      return;
    myBounds = new Rectangle(rect);
    setScale(myScale.getValue());
    updateImage();
  }

  public void dropActionLayer(ActionLayer layer) {
    if (myActionLayer == layer)
      setActionLayer(null);
  }

  public ScalarModel<ActionLayer> getSelectedLayer() {
    return mySelectedLayer;
  }

  public void requestFocus() {
    myComponentControl.requestFocus();
  }

  public void addHistoryItem(HistoryItem item) {
    myHistory.addItem(item);
  }

  public StorageLayer getStorageLayerForActionLayer() {
    return getStorageLayerForActionLayer(myActionLayer);
  }

  @Nullable
  public StorageLayer getStorageLayerForActionLayer(ActionLayer actionLayer) {
    Class toolLayerClass = actionLayer.getStorageLayerClass();
    StorageLayer layer = findStorageLayer(toolLayerClass);
    if (layer == null) {
      layer = actionLayer.createStorageLayer();
      if (layer != null) {
        myStorageLayers.add(layer);
        invalidateLayersCache();
      }
    }
    return layer;
  }

  public void undo() {
    myHistory.undo();
  }

  public void redo() {
    myHistory.redo();
  }


  /**
   * Finding
   */
  private StorageLayer findStorageLayer(Class layerClass) {
    for (StorageLayer layer : myStorageLayers) {
      if (layerClass == layer.getClass())
        return layer;
    }
    return null;
  }

  public Layer getLayer(Class layerClass) {
    if (myActionLayer != null && layerClass.isInstance(myActionLayer) &&
      (myActionLayer.isModified() || findStorageLayer(layerClass) == null))
      return myActionLayer;
    else
      return findStorageLayer(layerClass);
  }

  public Dimension getScaledSize() {
    return myScaledBounds.getSize();
  }

  public void setScale(double scale) {
    myScale.setValue(scale);
    myTransform = AffineTransform.getScaleInstance(scale, scale);
    myTransform.translate(-myBounds.x, -myBounds.y);
    updateImage();
  }

  public double getScale() {
    return myScale.getValue();
  }

  public ScalarModel<Double> getScaleModel() {
    return myScale;
  }

  public BufferedImage getResultImage() {
    dropActionLayer();
    setScale(1.0d);
    invalidateImage();
    return getImage();
  }

  public void toScreenCoordinates(Point p) {
    myTransform.transform(p, p);
    myComponentControl.toScreenCoordinates(p);
  }

  public class History extends SimpleModifiable {

    private java.util.List<HistoryItem> items = new ArrayList<HistoryItem>();

    private int current = -1;

    public void addItem(HistoryItem item) {
      while (current < items.size() - 1) {
        items.remove(items.size() - 1);
      }
      items.add(item);
      current = items.size() - 1;
      fireChanged();
    }

    public void undo() {
      if (current >= 0)
        items.get(current--).undo();
      fireChanged();
    }

    public void redo() {
      if (current < items.size() - 1)
        items.get(++current).redo();
      fireChanged();
    }

    public boolean canPerform(boolean undo) {
      if (undo) return current >= 0;
      else return current < items.size() - 1;
    }

    public void perform(boolean undo) {
      if (undo) undo();
      else redo();
    }
  }
}


