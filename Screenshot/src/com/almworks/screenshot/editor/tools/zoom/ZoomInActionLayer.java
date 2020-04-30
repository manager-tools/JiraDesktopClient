package com.almworks.screenshot.editor.tools.zoom;

import com.almworks.screenshot.editor.image.ImageEditorUtils;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.BaseLayerOptions;
import com.almworks.screenshot.editor.layers.MultipleShapeStorageLayer;
import com.almworks.screenshot.editor.layers.SingleShapeActionLayer;
import com.almworks.screenshot.editor.layers.StorageLayer;
import com.almworks.screenshot.editor.selection.BoxSelectionProcessor;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;

/**
 * @author Stalex
 */
public class ZoomInActionLayer extends SingleShapeActionLayer<ZoomInShape, ZoomInActionLayer.ZoomInOptions> {

  private BoxSelectionProcessor myZoomerSelection;
  private static final int INIT_SCALE = 2;

  @Override
  public void setActiveShape(ZoomInShape as) {
    myActiveShape = as;

    if (as != null) {
      mySelectionProcessor.setSelectionValue(myActiveShape.getRect());
      myZoomerSelection.setSelectionValue(myActiveShape.getLinkedRect());
      myOptions.setValue(as.getTimes());
    } else {
      mySelectionProcessor.clearSelection();
      myZoomerSelection.clearSelection();
    }
  }

  public ZoomInActionLayer(WorkingImage image) {
    super(image);
    myOptions = new ZoomInOptions();
    myZoomerSelection = new ZoomInSelectionProcessor();

  }

  public ZoomInShape createShape(Rectangle shape) {
    return new ZoomInShape(myWorkingImage, shape, myOptions.getValue(), Color.BLACK);
  }


  public void paint(Graphics2D g2, Area clip) {
    myZoomerSelection.paint(g2, clip);
    super.paint(g2, clip);
  }

  protected boolean isMadeByLayer(AbstractShape shape) {
    return shape instanceof ZoomInShape;
  }

  public void processMouseEvent(MouseEvent e) {
    myZoomerSelection.dispatchMouse(e);
    if (!e.isConsumed()) super.processMouseEvent(e);
  }


  public Class getStorageLayerClass() {
    return MultipleShapeStorageLayer.class;
  }

  public StorageLayer createStorageLayer() {
    return new MultipleShapeStorageLayer(myWorkingImage);
  }

  class ZoomInOptions extends BaseLayerOptions.SliderOptions implements ChangeListener {
    public ZoomInOptions() {
      super("Magnify", "Click and drag to select an area for magnification.");
      setSliderParams(2, 20, INIT_SCALE);
    }

    public void stateChanged(ChangeEvent e) {
      super.stateChanged(e);
      if (myActiveShape != null) {
        Rectangle dirty = new Rectangle(myActiveShape.getBounds());
        myActiveShape.setScale(getValue());
        myZoomerSelection.setSelectionValue(myActiveShape.getLinkedRect());
        dirty = dirty.union(myActiveShape.getBounds());
        myWorkingImage.reportDirty(dirty);
      }
    }
  }

  private class ZoomInSelectionProcessor extends BoxSelectionProcessor {
    public ZoomInSelectionProcessor() {
      super(ZoomInActionLayer.this.myWorkingImage);
    }

    @Override
    public void dispatchMouse(MouseEvent e) {
      if (getSelectionValue() != null && getSelectionValue().contains(e.getPoint()) || getRunningSelection() != null) {
        super.dispatchMouse(e);
        e.consume();
      }
    }

    @Override
    protected void setSelection(Rectangle selection) {
      assert selection != null;
      setSelectionValue(selection);      
      ZoomInShape newShape = createShape(myActiveShape);
      if (newShape != null) {
        newShape.setLinkedRect(selection);
        moveShape(newShape, myActiveShape);
      }
    }

    @Override
    protected void chooseOperation(MouseEvent e, Rectangle selection) {
      if (selection != null && selection.contains(e.getPoint())) {
        mySelectOperation = myMoveOperation;
      }
    }

    @Override
    protected void paintSelection(Rectangle selection, Graphics2D g2, Area clip) {
      ImageEditorUtils.drawRect(g2, selection, mySelectionFrameColor, SELECTION_STROKE);
      g2.setColor(mySelectionHandleColor);

    }
  }
}
