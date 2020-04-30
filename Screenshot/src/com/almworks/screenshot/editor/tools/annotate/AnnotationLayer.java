package com.almworks.screenshot.editor.tools.annotate;

import com.almworks.screenshot.editor.gui.FontChooserPanel;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.MultipleShapeStorageLayer;
import com.almworks.screenshot.editor.layers.ShapeHistoryItem;
import com.almworks.screenshot.editor.layers.SingleShapeActionLayer;
import com.almworks.screenshot.editor.layers.StorageLayer;
import com.almworks.screenshot.editor.selection.BoxSelectionProcessor;
import com.almworks.screenshot.editor.shapes.AbstractShape;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;

class AnnotationLayer extends SingleShapeActionLayer<AnnotationShape, AnnotationLayer.AnnotationOptions> {

  public static final float STROKE_WIDTH = 3F;

  private static final Color NOTE_AREA_COLOR = Color.BLACK;

  private BoxSelectionProcessor myAnnotationSelection;

  public void setCurrentFont(Font myCurrentFont, Color myCurrentFontColor) {
    AnnotationShape activeShape = myActiveShape;
    if (activeShape != null) {
      AnnotationShape newShape = createShape(activeShape);
      if (newShape != null) {
        newShape.setFontAndColor(myCurrentFont, myCurrentFontColor);
        moveShape(newShape, activeShape);
      }
    }
  }

  public Font getCurrentFont() {
    return myOptions.getFont();
  }

  public AnnotationLayer(WorkingImage wi) {
    super(wi);

    myOptions = new AnnotationOptions();
    setCurrentFont(myOptions.getFont(), myOptions.getFontColor());

    myAnnotationSelection = new AnnotationSelectionProcessor();
    myAnnotationSelection.setRunningSelectionColor(NOTE_AREA_COLOR);
  }


  protected boolean isMadeByLayer(AbstractShape shape) {
    return shape instanceof AnnotationShape;
  }

  @Override
  public void processMouseEvent(MouseEvent e) {
    myAnnotationSelection.dispatchMouse(e);
    if (!e.isConsumed()) {
      super.processMouseEvent(e);
    }
  }

  @Override
  public Rectangle getBounds() {
    return super.getBounds().union(myAnnotationSelection.getBounds());
  }

  @Override
  public void setActiveShape(AnnotationShape as) {
    super.setActiveShape(as);
    if (as != null) {
      if (!as.isSingleText()) {
        mySelectionProcessor.setSelectionValue(as.getRect());
      }
      myAnnotationSelection.setSelectionValue(as.getLinkedRect());
    } else {
      mySelectionProcessor.clearSelection();
      myAnnotationSelection.clearSelection();
    }
  }

  protected void removeSelection() {
    AnnotationShape activeShape = myActiveShape;
    if (activeShape != null && activeShape.isSingleText()) {
      if (myAnnotationSelection.getSelectionValue() != null) {
        getStorageLayer().unstore(activeShape);
        myWorkingImage.addHistoryItem(new ShapeHistoryItem<AnnotationShape>(this, myWorkingImage, null, activeShape));
        myWorkingImage.reportDirty(getBounds());
        myAnnotationSelection.clearSelection();
        setActiveShape(null);
        myWorkingImage.setCursor(Cursor.getDefaultCursor());
      }
    } else {
      super.removeSelection();
    }
  }


  @Override
  public void paint(Graphics2D g2, Area clip) {
    AwtUtil.applyRenderingHints(g2);
    super.paint(g2, clip);
    myAnnotationSelection.paint(g2, clip);
  }

  public Class getStorageLayerClass() {
    return MultipleShapeStorageLayer.class;
  }


  public StorageLayer createStorageLayer() {
    return new MultipleShapeStorageLayer(myWorkingImage);
  }

  /*public AnnotationShape createShape(AnnotationShape annotationShape) {
    return new AnnotationShape(annotationShape);
  }
*/
  public AnnotationShape createShape(Rectangle rectangle) {
    AnnotationShape shape = new AnnotationShape(myWorkingImage, rectangle, myOptions.getFont(), myOptions.getColor(),
      myOptions.getFontColor());

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        myOptions.focusTextEdit();
      }
    });
    return shape;
  }

  @Override
  public AnnotationShape createShape(Point point) {
    AnnotationShape shape =
      new AnnotationShape(myWorkingImage, point, myOptions.getFont(), myOptions.getColor(), myOptions.getFontColor());
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        myOptions.focusTextEdit();
      }
    });
    return shape;
  }

  private class AnnotationSelectionProcessor extends BoxSelectionProcessor {

    public AnnotationSelectionProcessor() {
      super(AnnotationLayer.this.myWorkingImage);
    }

    @Override
    public void dispatchMouse(MouseEvent e) {
      if (getSelectionValue() != null && getSelectionValue().contains(e.getPoint()) || getRunningSelection() != null) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
          if (e.getClickCount() == 2) {
            myOptions.focusTextEdit();

            return;
          }
        }
        super.dispatchMouse(e);
        e.consume();
      }
    }

    @Override
    protected void setSelection(Rectangle selection) {
      assert selection != null;
      setSelectionValue(selection);
      AnnotationShape activeShape = myActiveShape;
      if (activeShape != null) {
        AnnotationShape newShape = createShape(activeShape);
        if (newShape != null) {
          newShape.setLinkedRect(selection);
          moveShape(newShape, activeShape);
        }
      }
    }

    @Override
    protected void chooseOperation(MouseEvent e, Rectangle selection) {
      Point res = myResizeOperation.getCursorInPoint(selection, e.getPoint());
      if (res.x == 0) {
        mySelectOperation = myMoveOperation;
      } else {
        mySelectOperation = myResizeOperation;/*myMoveOperation*/
        myResizeOperation.setAllowedDirections(Cursor.E_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR);
      }
    }

    @Override
    protected void paintSelection(Rectangle selection, Graphics2D g2, Area clip) {
      //ImageEditorUtils.drawRect(g2, selection, mySelectionFrameColor, SELECTION_STROKE);
      //super.paintSelection(selection, g2, clip);
      Point p = getHandlePoint(selection, -1, 0);
      paintHandle(g2, p.x, p.y);
      p = getHandlePoint(selection, 1, 0);
      paintHandle(g2, p.x, p.y);
    }
  }


  public class AnnotationOptions extends SingleShapeActionLayer.LayerColorOptions
    implements FontChooserPanel.FontChangeListener
  {

    private TextEditorDialog myDialog = null;

    private FontChooserPanel myFontPanel;

    public AnnotationOptions() {
      super("Comment", "Click where you'd like to place a comment, or click and drag to attach a comment to an area.");
      myFontPanel = FontChooserPanel.getInstance();
      myFontPanel.setListener(this);
      setCurrentFont(myFontPanel.getCurrentFont(), myFontPanel.getCurrentFontColor());
      myContaiter.add(myFontPanel.getComponent());
    }

    public void focusTextEdit() {
      if (myDialog == null) {
        myDialog = new TextEditorDialog(FocusManager.getCurrentManager().getFocusedWindow());
      }

      AnnotationShape activeShape = myActiveShape;
      if (activeShape != null) {
        myDialog.setText(activeShape.getText(), activeShape.getTextBounds().getSize());
        Rectangle bounds = activeShape.getBounds();
        Point p = new Point(bounds.x, bounds.y + bounds.height);
        myWorkingImage.toScreenCoordinates(p);
        myDialog.show(p);

        if (myDialog.getNewText() != null) {
          activeShape.setText(myDialog.getNewText());
          myAnnotationSelection.setSelectionValue(activeShape.getLinkedRect());
        } else {
          if (activeShape.isFirstEdit()) {
            removeSelection();
          }
        }
        myWorkingImage.reportDirty(myWorkingImage.getBounds());
      }
    }

    public void onFontChanged(Font newFont, Color newColor) {
      setCurrentFont(newFont, newColor);
    }

    public Font getFont() {
      return myFontPanel.getCurrentFont();
    }

    public Color getFontColor() {
      return myFontPanel.getCurrentFontColor();
    }
  }
}
