package com.almworks.screenshot.editor.image;

import com.almworks.util.components.ScrollableHelper;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.ui.MegaMouseAdapter;
import com.almworks.util.ui.UIComponentWrapper2;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ConstProvider;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

public class ImageDesk implements UIComponentWrapper2 {
  public final static float PREDEFFINED_SCALES[] = {0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f};

  private final WorkingImage myImage;
  private final ImageCanvas myCanvas;
  private final JScrollPane myScrollPane;

  private int myZoomPosition = 2;

  public ImageDesk(WorkingImage image) {
    myImage = image;
    myCanvas = new ImageCanvas();
    myScrollPane = new JScrollPane(myCanvas);
    setupScrollpane();
  }

  private void setupScrollpane() {
    myScrollPane.setColumnHeaderView(getHorizontalRuler());
    myScrollPane.setRowHeaderView(getVerticalRuler());
    Aqua.cleanScrollPaneBorder(myScrollPane);
    Aqua.cleanScrollPaneResizeCorner(myScrollPane);
  }

  public JComponent getComponent() {
    return myScrollPane;
  }

  public void dispose() {
  }

  public Detach getDetach() {
    return null;
  }

  public WorkingImage getWorkingImage() {
    return myImage;
  }

  public Component getHorizontalRuler() {
    return null;
  }

  public Component getVerticalRuler() {
    return null;
  }

  public void start() {
    myImage.addRepaintSink(myCanvas);
    myImage.setComponentControl(myCanvas);

    myImage.setScale(PREDEFFINED_SCALES[myZoomPosition]);
  }

  public void addData(JPanel panel) {
    ConstProvider.addRoleValue(panel, WorkingImage.HISTORY_ROLE, myImage.getHistory());
  }


  public class ImageCanvas extends JComponent implements Scrollable, RepaintSink, ImageComponentControl {
    private final TexturePaint myChequeredPaint = UIUtil.createChequeredPaint();
    private MegaMouseAdapter myMouseAdapter;
    private KeyEventAdapter myKeyAdapter;
    private Cursor myLastCursor = null;
    private Rectangle myLastRepaintAbsBound = null;

    public ImageCanvas() {
      setAutoscrolls(true);
      myMouseAdapter = new MegaMouseAdapter() {
        protected void onMouseEvent(MouseEvent e) {
          Point p = getDifference();

          e.translatePoint(-p.x, -p.y);
          myImage.dispatch(e);
        }


        public void mouseWheelMoved(MouseWheelEvent e) {
          if (e.isControlDown()) {
            int tmp = myZoomPosition + e.getWheelRotation();
            if (tmp >= 0 && tmp < PREDEFFINED_SCALES.length) {
              myZoomPosition = tmp;
              myImage.setScale(PREDEFFINED_SCALES[myZoomPosition]);
            }
          }
        }
      };
      addMouseMotionListener(myMouseAdapter);
      addMouseListener(myMouseAdapter);
      addMouseWheelListener(myMouseAdapter);

      myKeyAdapter = new KeyEventAdapter() {
        protected void onKeyEvent(KeyEvent e) {
          myImage.processKeyEvent(e);
        }
      };
      addKeyListener(myKeyAdapter);
    }

    public void requestFocus() {
      requestFocusInWindow();
    }

    public void toScreenCoordinates(Point p) {
      Point diff = getDifference();
      p.translate(diff.x, diff.y);
      SwingUtilities.convertPointToScreen(p, myCanvas);
    }

    public void paint(Graphics g) {

      Rectangle clipBounds = g.getClipBounds();
      int thisWidth = getWidth();
      int thisHeight = getHeight();

      if (clipBounds == null) {
        clipBounds = new Rectangle(0, 0, thisWidth, thisHeight);
      }

      int imageWidth = myImage.getWidth();
      int imageHeight = myImage.getHeight();

      int dx = (thisWidth > imageWidth) ? (thisWidth - imageWidth) / 2 : 0;
      int dy = (thisHeight > imageHeight) ? (thisHeight - imageHeight) / 2 : 0;
      Rectangle imageBounds = new Rectangle(dx, dy, imageWidth, imageHeight);

      if (!imageBounds.contains(clipBounds))
        paintChequered(g, clipBounds);

      Rectangle imageClip = clipBounds.intersection(imageBounds);
      if (!imageClip.isEmpty()) {
        imageClip.translate(-dx, -dy);

        myImage.paintDirty(imageClip, myLastRepaintAbsBound);
        BufferedImage image = myImage.getImage();

        assert image.getWidth() == myImage.getWidth();
        assert image.getHeight() == myImage.getHeight();

        image = image.getSubimage(imageClip.x, imageClip.y, imageClip.width, imageClip.height);
        g.drawImage(image, dx + imageClip.x, dy + imageClip.y, null);
      }
    }

    public void requestRepaint(Rectangle bounds) {
      Point p = getDifference();

      myLastRepaintAbsBound = bounds;
      bounds = myImage.translateToRel(bounds);

      repaint(p.x + bounds.x, p.y + bounds.y, bounds.width, bounds.height);
    }

    public Point getDifference() {
      int thisWidth = getWidth();
      int thisHeight = getHeight();
      int imageWidth = myImage.getWidth();
      int imageHeight = myImage.getHeight();
      int dx = thisWidth > imageWidth ? (thisWidth - imageWidth) / 2 : 0;
      int dy = thisHeight > imageHeight ? (thisHeight - imageHeight) / 2 : 0;
      Point p = new Point(dx, dy);
      return p;
    }

    private void paintChequered(Graphics g, Rectangle clipBounds) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setPaint(myChequeredPaint);
      g2.fill(clipBounds);
    }

    public Dimension getPreferredSize() {
      return myImage.getScaledSize();
    }

    public Dimension getPreferredScrollableViewportSize() {
      return myImage.getScaledSize();
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return ScrollableHelper.getScrollableBlockIncrementStandard(orientation, visibleRect);
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return ScrollableHelper.getScrollableUnitIncrementStandard(orientation, visibleRect);
    }

    public boolean getScrollableTracksViewportHeight() {
      return ScrollableHelper.getScrollableTracksViewportHeightStandard(this);
    }

    public boolean getScrollableTracksViewportWidth() {
      return ScrollableHelper.getScrollableTracksViewportWidthStandard(this);
    }

    public void setComponentCursor(Cursor cursor) {
      if (myLastCursor != cursor) {
        myLastCursor = cursor;
        Cursor c = cursor == null ? Cursor.getDefaultCursor() : cursor;
        setCursor(c);
      }
    }

    public void imageResized(Rectangle oldBounds, Rectangle newBounds) {
      Container parent = getParent();
      if (!(parent instanceof JViewport))
        return;
      JViewport viewport = ((JViewport) parent);
      Rectangle viewRect = viewport.getViewRect();

      Dimension prefSize = getPreferredSize();
      setSize((int) Math.max(prefSize.getWidth(), viewRect.getWidth()),
        (int) Math.max(viewRect.getHeight(), prefSize.getHeight()));

      repaint();
    }
  }
}
