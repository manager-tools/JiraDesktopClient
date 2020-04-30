package com.almworks.screenshot.editor.tools.transform;

import com.almworks.screenshot.editor.image.ImageEditorUtils;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.*;
import com.almworks.screenshot.editor.selection.BoxSelectionProcessor;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;

/**
 * @author Stalex
 */
public class SelectionActionLayer
  extends SingleShapeActionLayer<SelectionShape, SelectionActionLayer.SelectionOptions>
{
  public SelectionActionLayer(WorkingImage myImageControl) {
    super(myImageControl);
    mySelectionProcessor = new SuperSelection(myImageControl);
    myOptions = new SelectionOptions();
  }

  protected void removeSelection() {
    if (mySelectionProcessor.getSelectionValue() != null) {
      assert myActiveShape != null;
      getStorageLayer().unstore(myActiveShape.getShape());
      myWorkingImage.addHistoryItem(new ShapeHistoryItem(this, myWorkingImage, null, myActiveShape.getShape()));
      myWorkingImage.reportDirty(getBounds());
      mySelectionProcessor.clearSelection();
      setActiveShape(null);
      myWorkingImage.setCursor(Cursor.getDefaultCursor());
    }
  }

  public SelectionShape createShape(SelectionShape shape) {
    return null;
  }

  public void setActiveShape(SelectionShape as) {
    super.setActiveShape(as);
  }

  protected boolean isMadeByLayer(AbstractShape shape) {
    return false;
  }

  public Class getStorageLayerClass() {
    return MultipleShapeStorageLayer.class;
  }

  public StorageLayer createStorageLayer() {
    return new MultipleShapeStorageLayer(myWorkingImage);
  }


  protected class SuperSelection extends BoxSelectionProcessor {
    public SuperSelection(LayerImageControl imageControl) {
      super(imageControl);
    }

    protected void paintSelection(Rectangle selection, Graphics2D g2, Area clip) {
      ImageEditorUtils.drawRect(g2, selection, mySelectionFrameColor, SELECTION_STROKE);
    }

    @Override
    protected Rectangle chooseSelection() {
      return mySelection.getValue();
    }

    protected void chooseOperation(MouseEvent e, Rectangle selection) {
      if (selection != null && selection.contains(e.getPoint())) {
        mySelectOperation = myMoveOperation;
      } else {
        mySelectOperation = myResizeOperation;
      }
    }

    @Override
    protected void paintRunningSelection(Graphics2D g2, Area clip) {
      ImageEditorUtils.drawRect(g2, myRunningSelection, myRunningSelectionColor, SELECTION_STROKE);
    }

    protected void setSelection(Rectangle runningSelection) {
      super.setSelection(runningSelection);
      StorageLayer storageLayer = getStorageLayer();
      if (storageLayer == null)
        return;
      if (myIsNewSelection) {
        AbstractShape shape = storageLayer.getShapeFromRect(runningSelection);
        if (shape != null) {
          myActiveShape = new SelectionShape(myImageControl, shape);
        } else {
          clearSelection();
        }
        setActiveShape(myActiveShape);
      } else {

        Point initLocation = myActiveShape.getRect().getLocation();

        AbstractShape oldS = myActiveShape.getShape();
        storageLayer.unstore(oldS);

        AbstractShape newS = myActiveShape.getClone();
        myActiveShape.setShape(newS);
        myActiveShape.translate(runningSelection.x - initLocation.x, runningSelection.y - initLocation.y);
        storageLayer.store(newS);

        myWorkingImage.addHistoryItem(new ShapeHistoryItem(SelectionActionLayer.this, myImageControl, newS, oldS));

        myWorkingImage.reportDirty(myWorkingImage.getBounds());
      }
    }
  }


  public class SelectionOptions extends BaseLayerOptions {

    protected SelectionOptions() {
      super("Select", "Select a previously placed object, then move or delete it.");
    }

    protected JComponent createContent() {
      return null;
    }
  }
}

