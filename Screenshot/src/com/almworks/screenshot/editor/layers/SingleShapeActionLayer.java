package com.almworks.screenshot.editor.layers;

import com.almworks.screenshot.editor.gui.Palette;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.selection.BoxSelectionProcessor;
import com.almworks.screenshot.editor.selection.RectangleSelectionProcessor;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;

/**
 * @author Stalex
 */
public abstract class SingleShapeActionLayer<S extends AbstractShape, O extends LayerOptions> implements ActionLayer<S> {

  protected WorkingImage myWorkingImage;
  protected O myOptions;
  protected RectangleSelectionProcessor mySelectionProcessor;
  protected StorageLayer myStorageLayer = null;
  protected boolean myIsModified = false;

  protected S myActiveShape = null;


  protected SingleShapeActionLayer(WorkingImage wi) {
    myWorkingImage = wi;
    mySelectionProcessor = new SingleBoxSelectionProcessor(myWorkingImage.getLayerControl());
  }

  public S createShape(S shape) {
    return (S) shape.clone();
  }

  public S createShape(Rectangle shape) {
    return null;
  }

  public S createShape(Point point) {
    return null;
  }

  public boolean isModified() {
    return myIsModified;
  }

  public void setActiveShape(S as) {
    if (as != null) {      
      mySelectionProcessor.setSelectionValue(as.getRect());
    } else {
      mySelectionProcessor.setSelectionValue(null);
    }
    myActiveShape = as;
  }

  public void paint(Graphics2D g2, Area clip) {
    if (mySelectionProcessor != null) mySelectionProcessor.paint(g2, clip);
  }

  public void cancel() {
    mySelectionProcessor.clearSelection();
  }

  protected abstract boolean isMadeByLayer(AbstractShape shape);

  public void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_PRESSED) {
      StorageLayer storageLayer = getStorageLayer();
      if (storageLayer != null) {
        AbstractShape as = storageLayer.getShapeFromPoint(e.getPoint());
        if (isMadeByLayer(as)) {
          if (myActiveShape != as) {
            e.consume();
          }
          setActiveShape((S) as);
          myWorkingImage.reportDirty(myWorkingImage.getBounds());
        }
      }
    }
    if (!e.isConsumed()) {
      mySelectionProcessor.dispatchMouse(e);
    }
  }


  public void processKeyEvent(KeyEvent e) {
    mySelectionProcessor.dispatchKeyPressed(e);
    if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_DELETE) {
      removeSelection();
    }
  }

  protected void removeSelection() {
    if (mySelectionProcessor.getSelectionValue() != null) {
      assert myActiveShape != null;
      StorageLayer storageLayer = getStorageLayer();
      if (storageLayer != null)
        storageLayer.unstore(myActiveShape);
      myWorkingImage.addHistoryItem(new ShapeHistoryItem<S>(this, myWorkingImage, null, myActiveShape));
      myWorkingImage.reportDirty(getBounds());
      mySelectionProcessor.clearSelection();
      setActiveShape(null);
      myWorkingImage.setCursor(Cursor.getDefaultCursor());
    }
  }

  public Rectangle getBounds() {
    if (myActiveShape != null) {
      return myActiveShape.getBounds().union(mySelectionProcessor.getBounds());
    }
    return myWorkingImage.getBounds();
  }

  public LayerOptions getOptions() {
    return myOptions;
  }

  public void resize(Rectangle oldBounds, Rectangle newBounds) {
  }

  public Class getStorageLayerClass() {
    return null;
  }

  public StorageLayer getStorageLayer() {
    if (getStorageLayerClass() == null) {
      return null;
    } else if (myStorageLayer != null) {
      return myStorageLayer;
    } else {
      myStorageLayer = myWorkingImage.getStorageLayerForActionLayer(this);
      assert myStorageLayer != null : this;
      return myStorageLayer;
    }
  }

//  public StorageLayer createStorageLayer() {
//    return null;
//  }

  protected class SingleBoxSelectionProcessor extends BoxSelectionProcessor {
    public SingleBoxSelectionProcessor(LayerImageControl myImageControl) {
      super(myImageControl);
    }

    protected void setSelection(Rectangle selection) {
      super.setSelection(selection);

      assert selection != null;

      S oldShape = null;
      S newShape = null;

      if (myIsNewSelection) {
        if (selection.width > 5 && selection.height > 5) {
          newShape = createShape(selection);
        } else if (selection.width == 1 && selection.height == 1) {
          newShape = createShape(selection.getLocation());
        } else {
          newShape = null;
        }
      } else {
        if (selection.width > 5 && selection.height > 5) {
          newShape = createShape(myActiveShape);
          if (newShape != null) {
            newShape.setRect(selection);
          }
          oldShape = myActiveShape;
        }
      }
      if (newShape != null) {
        moveShape(newShape, oldShape);
      } else {
        setActiveShape(null);
      }

    }
  }

  protected void moveShape(S newShape, S oldShape) {
    Rectangle dirty = null;
    if (oldShape != null) {
      dirty = oldShape.getBounds();
      getStorageLayer().unstore(oldShape);
    }
    getStorageLayer().store(newShape);
    myWorkingImage.addHistoryItem(new ShapeHistoryItem<S>(SingleShapeActionLayer.this, myWorkingImage, newShape, oldShape));
    setActiveShape(newShape);

    myWorkingImage.reportDirty(dirty == null
      ? newShape.getBounds()
      : dirty.union(newShape.getBounds()));

    myIsModified = true;
  }

  public class LayerColorOptions extends BaseLayerOptions implements Palette.ColorChangeEvent {
    protected final Box myContaiter;

    protected Palette myPalette;

    public LayerColorOptions(String title, String description) {
      super(title, description);
      myContaiter = new Box(BoxLayout.Y_AXIS);
      myPalette = new Palette();
      myContaiter.add(myPalette.getComponent());
      myPalette.setListener(this);
    }

    protected JComponent createContent() {
      return myContaiter;
    }

    public void colorChanged(Color newColor) {
      if (myActiveShape != null) {
        myActiveShape.setColor(newColor);
        myWorkingImage.reportDirty(myActiveShape.getBounds());
      }
    }

    public void setColor(Color color) {
      myPalette.setColor(color);
    }

    public Color getColor() {
      return myPalette.getColor();
    }
  }
}


