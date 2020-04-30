package com.almworks.screenshot.editor.selection;

import com.almworks.screenshot.editor.image.ImageEditorUtils;
import com.almworks.screenshot.editor.layers.LayerImageControl;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;

public abstract class RectangleSelectionProcessor {
  protected static final int MIN_SELECTION_LENGTH = 15;
  protected static final int ALLOWANCE = (MIN_SELECTION_LENGTH - 1) / 2;
  protected static final int NO_VALUE = Integer.MIN_VALUE;

  protected final Stroke myRunningSelectionStroke = createRunningSelectionStroke();
  protected Color myRunningSelectionColor = Color.BLACK;
  protected Rectangle myRunningSelection = null;
  protected final BasicScalarModel<Rectangle> mySelection = BasicScalarModel.createWithValue(null, true);
  protected final LayerImageControl myImageControl;

  protected SelectOperation mySelectOperation;
  protected boolean mySelectOperationActive = false;

  public RectangleSelectionProcessor(LayerImageControl imageControl) {
    myImageControl = imageControl;
  }

  public void clearSelection() {
    mySelection.setValue(null);
    myRunningSelection = null;
  }

  public Rectangle getRunningSelection() {
    return myRunningSelection;
  }

  public Rectangle getSelectionValue() {
    return mySelection.getValue();
  }

  public void setSelectionValue(Rectangle selection) {
    mySelection.setValue(selection);

  }

  public void paint(Graphics2D g2, Area clip) {
    if (myRunningSelection != null) {
      paintRunningSelection(g2, clip);
    } else {
      Rectangle selection = mySelection.getValue();
      if (selection != null) {
        paintSelection(selection, g2, clip);
      } else {
        paintClear(g2, clip);
      }
    }
  }

  private static BasicStroke createRunningSelectionStroke() {
    return new BasicStroke(1F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10F, new float[]{1F, 1F}, 0F);
  }

  public ScalarModel<Rectangle> getSelectionModel() {
    return mySelection;
  }

  public Rectangle getBounds() {
    if (myRunningSelection != null) {
      Rectangle rect = new Rectangle(myRunningSelection);
      rect.grow(10, 10);
      return rect;
    } else {
      Rectangle selection = mySelection.getValue();

      if (selection != null) {
        selection.grow(10, 10);
        return selection;
      } else return ImageEditorUtils.EMPTY_BOUNDS;
    }
  }

  public final void setRunningSelectionColor(Color color) {
    myRunningSelectionColor = color;
  }

  protected void truncateRect(Rectangle rect) {
    Rectangle bounds = myImageControl.getBounds();
    int x = (rect.x < bounds.x) ? bounds.x : rect.x;
    int y = (rect.y < bounds.y) ? bounds.y : rect.y;

    int width = (rect.width > bounds.width) ? bounds.width : rect.width;
    int height = (rect.height > bounds.height) ? bounds.height : rect.height;

    x = (x + width >= bounds.getMaxX()) ? (int) (bounds.getMaxX() - width) : x;
    y = (y + height >= bounds.getMaxY()) ? (int) (bounds.getMaxY() - height) : y;

    rect.setLocation(x, y);

    rect.setSize(width, height);
  }

  protected Point truncatePoint(Point p) {
    Rectangle bounds = myImageControl.getBounds();

    if (p.x < bounds.x) p.x = bounds.x;
    if (p.x >= bounds.width + bounds.x) p.x = bounds.width + bounds.x - 1;
    if (p.y < bounds.y) p.y = bounds.y;
    if (p.y >= bounds.height + bounds.y) p.y = bounds.height + bounds.y - 1;
    return p;
  }

  protected abstract void chooseOperation(MouseEvent e, Rectangle selection);

  protected abstract Rectangle chooseSelection();

  public void dispatchKeyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_ESCAPE:
        if (myRunningSelection != null) {
          myImageControl.reportDirty(myRunningSelection);
          myRunningSelection = null;
        }
        break;
    }
  }

  public void dispatchMouse(MouseEvent e) {

    Rectangle selection = chooseSelection();
    if (!mySelectOperationActive) {
      chooseOperation(e, selection);
    }

    if (myImageControl.getBounds().contains(e.getPoint())) mySelectOperation.setCursor(e);

    int id = e.getID();

    if (id == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON1 && myRunningSelection == null) {
      mySelectOperation.startSelection(e);
      mySelectOperationActive = true;
    } else if (id == MouseEvent.MOUSE_DRAGGED && myRunningSelection != null) {
      mySelectOperation.continueSelection(e);
    } else if (id == MouseEvent.MOUSE_RELEASED && e.getButton() == MouseEvent.BUTTON1 && myRunningSelection != null) {
      mySelectOperation.endSelection(e);
      mySelectOperationActive = false;
    }
  }

  protected abstract void paintClear(Graphics2D g2, Area clip);

  protected abstract void paintRunningSelection(Graphics2D g2, Area clip);

  protected abstract void paintSelection(Rectangle selection, Graphics2D g2, Area clip);

  protected abstract void setSelection(Rectangle runningSelection);


  /**
   * When selection is tranformed this method called to give the ability
   * devide new selection from the existing selection
   *
   * @param selection null if new selection. Otherwise, exiting rectangle
   */
  protected void handleSelectionChange(@Nullable Rectangle selection) {

  }


  interface SelectOperation {

    void setCursor(MouseEvent e);

    void startSelection(MouseEvent e);

    void continueSelection(MouseEvent e);

    void endSelection(MouseEvent e);
  }

}
