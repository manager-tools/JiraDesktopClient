package com.almworks.screenshot.editor.tools.crop;

import com.almworks.screenshot.editor.image.HistoryItem;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.layers.BaseLayerOptions;
import com.almworks.screenshot.editor.layers.SingleShapeActionLayer;
import com.almworks.screenshot.editor.layers.StorageLayer;
import com.almworks.screenshot.editor.selection.BoxSelectionProcessor;
import com.almworks.screenshot.editor.shapes.AbstractShape;
import com.almworks.util.components.AActionButton;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

public class CropLayer extends SingleShapeActionLayer {
  private final Paint myShadowedPaint = createShadowedPaint();

  public CropLayer(WorkingImage imageControl) {
    super(imageControl);
    myOptions = new CropOptions();
    mySelectionProcessor = new BoxSelectionProcessor(myWorkingImage.getLayerControl()) {
      protected void paintSelection(Rectangle selection, Graphics2D g2, Area clip) {
        dropShadow(selection, g2, clip);
        super.paintSelection(selection, g2, clip);
      }
    };
  }

  public AbstractShape createShape(AbstractShape shape) {
    return null;
  }

  public AbstractShape createShape(Rectangle shape) {
    return null;
  }

  public StorageLayer createStorageLayer() {
    return null;
  }

  @Override
  public void processMouseEvent(MouseEvent e) {
    mySelectionProcessor.dispatchMouse(e);
  }

  protected boolean isMadeByLayer(AbstractShape shape) {
    return false;
  }

  public void dropShadow(Rectangle selection, Graphics2D g2, Area clip) {
    Rectangle bounds = clip.getBounds();
    if (!selection.contains(bounds)) {
      // draw shadowed
      Area area = new Area(bounds);
      Rectangle r = new Rectangle(selection);
      area.subtract(new Area(r));
      g2.setPaint(myShadowedPaint);
      g2.fill(area);
    }
  }

  public void processKeyEvent(KeyEvent e) {
    if (e.getID() == KeyEvent.KEY_PRESSED) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        if (mySelectionProcessor.getSelectionValue() != null) {
          apply();
          myWorkingImage.setCursor(Cursor.getDefaultCursor());
        }
      }
      if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        if (mySelectionProcessor.getSelectionValue() != null) {
          cancel();
          myWorkingImage.reportDirty(myWorkingImage.getBounds());
//        myWorkingImage.dropActionLayer(this);
          myWorkingImage.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
      }
    }
  }


  private void apply() {
    Rectangle selection = mySelectionProcessor.getSelectionValue();
    assert selection != null;
    Rectangle oldBounds = myWorkingImage.getBounds();
    Rectangle newBounds = selection;
    myWorkingImage.addHistoryItem(new CropHistoryItem(oldBounds, newBounds));
    myWorkingImage.cropImage(newBounds);
    cancel();
    myWorkingImage.reportDirty(myWorkingImage.getBounds());
  }

  private static Paint createShadowedPaint() {
    int size = 8;
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics g = image.getGraphics();
    Color c = new Color(0, 0, 0, 0.5F);
    ((Graphics2D) g).setPaint(c);
    g.fillRect(0, 0, size, size);
    g.dispose();
    return new TexturePaint(image, new Rectangle.Double(0, 0, size, size));
  }

  private class CropOptions extends BaseLayerOptions {
    private final AActionButton myApply = new AActionButton();

    public CropOptions() {
      super("Crop Image", "Select an area and press Enter, or click Apply Crop.");

      myApply.setAnAction(new SimpleAction("Apply Crop") {
        protected void customUpdate(UpdateContext context) throws CantPerformException {
          context.updateOnChange(mySelectionProcessor.getSelectionModel());
          if (mySelectionProcessor.getSelectionValue() == null)
            context.setEnabled(EnableState.DISABLED);
        }

        protected void doPerform(ActionContext context) throws CantPerformException {
          apply();
        }
      });
    }

    protected JComponent createContent() {
      return SingleChildLayout.envelop(myApply, 0F, 0.5F);
    }
  }


  private class CropHistoryItem implements HistoryItem {
    private final Rectangle myOldBounds;
    private final Rectangle myNewBounds;

    public CropHistoryItem(Rectangle oldBounds, Rectangle newBounds) {
      myOldBounds = oldBounds;
      myNewBounds = newBounds;
    }

    public void undo() {
      myWorkingImage.cropImage(myOldBounds);
    }

    public void redo() {
      myWorkingImage.cropImage(myNewBounds);
    }
  }
}
