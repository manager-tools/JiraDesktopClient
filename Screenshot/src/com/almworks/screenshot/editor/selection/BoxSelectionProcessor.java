package com.almworks.screenshot.editor.selection;

import com.almworks.screenshot.editor.layers.LayerImageControl;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;

public class BoxSelectionProcessor extends SimpleBoxSelectionProcessor {
  private static final int HANDLE_SIZE = 5;
  protected final static Stroke SELECTION_STROKE = createSelectionStroke();
  protected final static Stroke NORMAL_STROKE = new BasicStroke(1f);

  protected Color mySelectionFrameColor = new Color(0.4F, 0.4F, 0.4F);
  protected Color mySelectionHandleColor = new Color(0.2F, 0.2F, 0.2F);

  protected final BoxSelectResizing myResizeOperation = new BoxSelectResizing();
  protected final BoxSelectMoving myMoveOperation = new BoxSelectMoving();

  protected boolean myIsNewSelection;

  public BoxSelectionProcessor(LayerImageControl imageControl) {
    super(imageControl);
  }



  protected void handleSelectionChange(Rectangle selection) {
    myIsNewSelection = (selection == null);
  }

  protected void paintSelection(Rectangle selection, Graphics2D g2, Area clip) {
    g2.setColor(mySelectionHandleColor);
    for (int i = -1; i <= 1; i++) {
      for (int j = -1; j <= 1; j++) {
        if (i != 0 || j != 0) {
          Point p = getHandlePoint(selection, i, j);
          paintHandle(g2, p.x, p.y);
        }
      }
    }
  }

  public static void paintHandle(Graphics2D g2, int x, int y) {
    g2.setStroke(NORMAL_STROKE);
    g2.setColor(Color.GRAY);
    g2.fillRect(x - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
    g2.setColor(Color.BLACK);
    g2.drawRect(x - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
  }

  protected void chooseOperation(MouseEvent e, Rectangle selection) {
    Rectangle moveBox = null;
    if (selection != null) {
      moveBox = new Rectangle(selection);
      moveBox.grow(-HANDLE_SIZE, -HANDLE_SIZE);
    }
    if (selection != null && moveBox.contains(e.getPoint())) {
      mySelectOperation = myMoveOperation;
    } else {
      mySelectOperation = myResizeOperation;
    }
  }

  protected Rectangle chooseSelection() {
    Rectangle selection = mySelection.getValue();
    if (selection == null) selection = myRunningSelection;
    return selection;
  }


  private static Stroke createSelectionStroke() {
    BasicStroke basicStroke = /*new BasicStroke(1F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 2f);*/
        new BasicStroke(0.8F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10F, new float[]{5f, 3f}, 0f);

    return basicStroke;
  }


  class BoxSelectMoving implements SelectOperation {
    private Point myHandlePoint;

    public void setCursor(MouseEvent e) {
      myImageControl.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    public void startSelection(MouseEvent e) {
      assert myRunningSelection == null : myRunningSelection;
      Rectangle selection = mySelection.getValue();
      assert selection != null : selection;
      handleSelectionChange(selection);
      myHandlePoint = e.getPoint();
      myHandlePoint.translate(-selection.x, -selection.y);
      mySelection.setValue(null);
      myRunningSelection = new Rectangle(selection);
      myImageControl.reportDirty(myImageControl.getBounds());
      e.consume();
    }

    public void continueSelection(MouseEvent e) {
      updateRunningSelection(e);
      e.consume();
    }

    public void endSelection(MouseEvent e) {
      updateRunningSelection(e);
      setSelection(myRunningSelection);
      myRunningSelection = null;
      myImageControl.reportDirty(myImageControl.getBounds());
      e.consume();
    }

    private void updateRunningSelection(MouseEvent e) {
      Rectangle r = getBounds();

      Point point = e.getPoint();

      myRunningSelection.setLocation(point.x - myHandlePoint.x, point.y - myHandlePoint.y);
      truncateRect(myRunningSelection);

      r = r.union(getBounds());

      myImageControl.reportDirty(r);
    }
  }
}
