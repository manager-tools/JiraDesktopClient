package com.almworks.screenshot.editor.selection;

import com.almworks.screenshot.editor.image.ImageEditorUtils;
import com.almworks.screenshot.editor.layers.LayerImageControl;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;

public class SimpleBoxSelectionProcessor extends RectangleSelectionProcessor {
  protected static final int[] RESIZE_CURSORS = {Cursor.NW_RESIZE_CURSOR, Cursor.N_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR, -1, Cursor.E_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR};

  private final SelectOperation myBoxSelectResizingOperation = new BoxSelectResizing();

  public SimpleBoxSelectionProcessor(LayerImageControl imageControl) {
    super(imageControl);
  }

  protected void chooseOperation(MouseEvent e, Rectangle selection) {
    mySelectOperation = myBoxSelectResizingOperation;
  }

  protected Rectangle chooseSelection() {
    mySelection.setValue(null);
    return myRunningSelection;
  }

  protected void paintClear(Graphics2D g2, Area clip) {
  }

  protected void paintRunningSelection(Graphics2D g2, Area clip) {
    ImageEditorUtils.drawRect(g2, myRunningSelection, myRunningSelectionColor, myRunningSelectionStroke);
  }

  protected void paintSelection(Rectangle selection, Graphics2D g2, Area clip) {
  }


  protected void setSelection(Rectangle runningSelection) {
    if (runningSelection.width < MIN_SELECTION_LENGTH || runningSelection.height < MIN_SELECTION_LENGTH)
      mySelection.setValue(null);
    else mySelection.setValue(new Rectangle(runningSelection));
  }

  protected static Point getHandlePoint(Rectangle selection, int xpos, int ypos) {
    assert xpos >= -1 && xpos <= 1 : xpos;
    assert ypos >= -1 && ypos <= 1 : ypos;
    double halfWidth = (selection.getWidth() - 1) / 2D;
    double halfHeight = (selection.getHeight() - 1) / 2D;
    return new Point(selection.x + (int) (halfWidth * (xpos + 1)), selection.y + (int) (halfHeight * (ypos + 1)));
  }


  public class BoxSelectResizing implements SelectOperation {

    private int[] myDirections = null;
    private boolean myAllowed;
    private int xResize = 0;
    private int yResize = 0;
    private Point myRunningEnd = null;
    private Point myRunningOrigin = null;
    private int myRunningEndFixedX = NO_VALUE;
    private int myRunningEndFixedY = NO_VALUE;


    public void setWrongDirections(int... direction) {
      myAllowed = false;
      myDirections = direction;
    }

    public void setAllowedDirections(int... direction) {
      myAllowed = true;
      myDirections = direction;
    }


    public void setCursor(MouseEvent e) {
      xResize = 0;
      yResize = 0;
      Rectangle selection = mySelection.getValue();
      if (selection == null) selection = myRunningSelection;
      if (selection != null) {
        Point res = getCursorInPoint(selection, e.getPoint());
        xResize = res.x;
        yResize = res.y;
      }
      if (myDirections != null) {
        if (!myAllowed) {
          for (int disallowed : myDirections) {
            if (RESIZE_CURSORS[(xResize + 1) + (yResize + 1) * 3] == disallowed) {
              xResize = yResize = 0;
            }
          }
        } else {
          boolean hits = false;
          for (int allowed : myDirections) {
            if (RESIZE_CURSORS[(xResize + 1) + (yResize + 1) * 3] == allowed) {
              hits = true;
            }
          }
          if (!hits) {
            xResize = yResize = 0;
          }
        }
      }

      myImageControl.setCursor(getResizingCursor(xResize, yResize, Cursor.CROSSHAIR_CURSOR));
    }

    public Point getCursorInPoint(Rectangle selection, Point p) {
      int xRes = 0, yRes = 0;
      int dx = selection.x - p.x;
      int dy = selection.y - p.y;
      if (dy < ALLOWANCE && dy > -selection.height - ALLOWANCE) {
        if (Math.abs(dx) < ALLOWANCE) xRes = -1;
        else if (Math.abs(dx + selection.width) < ALLOWANCE) xRes = 1;
      }
      if (dx < ALLOWANCE && dx > -selection.width - ALLOWANCE) {
        if (Math.abs(dy) < ALLOWANCE) yRes = -1;
        else if (Math.abs(dy + selection.height) < ALLOWANCE) yRes = 1;
      }
      return new Point(xRes, yRes);
    }


    public void startSelection(MouseEvent e) {
      assert myRunningSelection == null : myRunningSelection;
      Rectangle selection = mySelection.getValue();
      if (selection != null) {

        mySelection.setValue(null);
      }
      if (selection != null && ((xResize | yResize) != 0)) {
        myRunningSelection = new Rectangle(selection);
        handleSelectionChange(selection);
        if (xResize * yResize != 0) {
          // corner drag: origin is the opposite corner
          myRunningOrigin = getHandlePoint(selection, -xResize, -yResize);
        } else if (xResize != 0 && yResize == 0) {
          // vertical side drag
          myRunningOrigin = getHandlePoint(selection, -xResize, -1);
          myRunningEndFixedY = selection.y + selection.height - 1;
        } else if (yResize != 0 && xResize == 0) {
          // horizontal side drag
          myRunningOrigin = getHandlePoint(selection, -1, -yResize);
          myRunningEndFixedX = selection.x + selection.width - 1;
        }
      } else {
        handleSelectionChange(null);
        Point p = truncatePoint(e.getPoint());
        myRunningOrigin = p;
        myRunningSelection = new Rectangle(p);
        myRunningEndFixedX = NO_VALUE;
        myRunningEndFixedY = NO_VALUE;
      }
      myRunningEnd = null;
      myImageControl.reportDirty(myImageControl.getBounds());
      e.consume();
    }

    public void continueSelection(MouseEvent e) {
      setRunningEnd(e);
      updateRunningSelection();
      e.consume();
    }


    public void endSelection(MouseEvent e) {
      setRunningEnd(e);
      updateRunningSelection();
      setSelection(myRunningSelection);
      clearRunningSelection();
      myImageControl.reportDirty(myImageControl.getBounds());
      e.consume();
    }

    private Cursor getResizingCursor(int movex, int movey, int defaultCursor) {
      assert movex >= -1 && movex <= 1;
      assert movey >= -1 && movey <= 1;
      int index = (movex + 1) + (movey + 1) * 3;
      int cursor = RESIZE_CURSORS[index];
      if (cursor < 0) cursor = defaultCursor;
      return Cursor.getPredefinedCursor(cursor);
    }

    private void setRunningEnd(MouseEvent e) {
      myRunningEnd = truncatePoint(e.getPoint());
      if (myRunningEndFixedX != NO_VALUE) myRunningEnd.x = myRunningEndFixedX;
      if (myRunningEndFixedY != NO_VALUE) myRunningEnd.y = myRunningEndFixedY;
    }

    private void clearRunningSelection() {
      myRunningSelection = null;
      myRunningOrigin = null;
      myRunningEnd = null;
      myRunningEndFixedX = NO_VALUE;
      myRunningEndFixedY = NO_VALUE;
    }

    private void updateRunningSelection() {
      if (myRunningEnd == null) return;

      Rectangle oldSeletion = getBounds();
      if (Math.abs(myRunningEnd.x - myRunningOrigin.x) < MIN_SELECTION_LENGTH)
        myRunningEnd.x = myRunningOrigin.x + MIN_SELECTION_LENGTH * (int) Math.signum(myRunningEnd.x - myRunningOrigin.x);
      if (Math.abs(myRunningEnd.y - myRunningOrigin.y) < MIN_SELECTION_LENGTH)
        myRunningEnd.y = myRunningOrigin.y + MIN_SELECTION_LENGTH * (int) Math.signum(myRunningEnd.y - myRunningOrigin.y);
      int x1 = Math.min(myRunningOrigin.x, myRunningEnd.x);
      int x2 = Math.max(myRunningOrigin.x, myRunningEnd.x) + 1;
      int y1 = Math.min(myRunningOrigin.y, myRunningEnd.y);
      int y2 = Math.max(myRunningOrigin.y, myRunningEnd.y) + 1;
      myRunningSelection.x = x1;
      myRunningSelection.y = y1;
      myRunningSelection.width = x2 - x1;
      myRunningSelection.height = y2 - y1;
      truncateRect(myRunningSelection);

      myImageControl.reportDirty(getBounds().union(oldSeletion));

    }
  }

}
