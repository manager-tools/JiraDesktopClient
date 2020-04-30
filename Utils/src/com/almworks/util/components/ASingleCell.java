package com.almworks.util.components;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.renderer.*;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

public class ASingleCell<T> extends JComponent {
  private CanvasRenderer<? super T> myRenderer = Renderers.defaultCanvasRenderer();
  private ValueModel<? extends T> myModel = null;
  private final CanvasComponent.Simple myCanvasComponent = new CanvasComponent.Simple(ASingleCell.this);
  private final ChangeListener myListener = new ChangeListener() {
    @Override
    public void onChange() {
      invalidate();
      revalidate();
      repaint();
    }
  };
  private boolean myCanShrink = true;

  public ASingleCell() {
    updateUI();
  }

  public static <T> ASingleCell<T> create() {
    return new ASingleCell<T>();
  }

  private CanvasComponent.Simple getCanvasComponent() {
    return myCanvasComponent;
  }

  public void setRenderer(CanvasRenderer<? super T> renderer) {
    myRenderer = renderer;
  }

  public CanvasRenderer<? super T> getRenderer() {
    return myRenderer;
  }

  public void setCanShrink(boolean canShrink) {
    if (myCanShrink == canShrink) return;
    myCanShrink = canShrink;
    invalidate();
    revalidate();
    repaint();
  }

  public void setModel(ValueModel<? extends T> model) {
    if (myModel == model) return;
    ValueModel<? extends T> oldModel = myModel;
    if (oldModel != null) oldModel.removeChangeListener(myListener);
    myModel = model;
    if (myModel != null) myModel.addAWTChangeListener(myListener);
    firePropertyChange("model", oldModel, model);
    invalidate();
    revalidate();
    repaint();
  }

  public T getValue() {
    return myModel != null ? myModel.getValue() : null;
  }

  private void renderValueOn(Canvas canvas, final CellState state) {
    CanvasRenderer<? super T> renderer = myRenderer;
    if (renderer == null) return;
    T value = getValue();

    renderer.renderStateOn(state, canvas, value);
  }

  private CellState getCurrentState() {
    return new SimpleCellState(getBackground(), getForeground(), getFont());
  }

  @Override
  public void updateUI() {
    setUI(SingleCellUI.INSTANCE);
  }

  private static class SingleCellUI extends ComponentUI {
    private static final SingleCellUI INSTANCE = new SingleCellUI();

    @Override
    public void installUI(JComponent c) {
      LookAndFeel.installColorsAndFont(c, "Label.background", "Label.foreground", "Label.font");
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
      Dimension size = getSelfPrefSize(c);
      if (size == null) size = new Dimension(0, 0);
      Insets insets = c.getInsets();
      size.width += insets.left + insets.right;
      size.height += insets.top + insets.bottom;
      return size;
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
      ASingleCell cell = Util.castNullable(ASingleCell.class, c);
      if (cell == null) return super.getMinimumSize(c);
      Dimension pref = getPreferredSize(cell);
      if (!cell.myCanShrink) return pref;
      pref.width = 1;
      return pref;
    }

    private Dimension getSelfPrefSize(JComponent c) {
      ASingleCell cell = Util.castNullable(ASingleCell.class, c);
      if (cell == null) return new Dimension(0, 0);
      CellState state = cell.getCurrentState();
      CanvasComponent.Simple canvasComponent = cell.getCanvasComponent();
      CanvasImpl canvas = canvasComponent.useCanvas(state);
      cell.renderValueOn(canvas, state);
      Dimension size = new Dimension(0, 0);
      canvas.getSize(size, canvasComponent);
      canvasComponent.releaseCanvas(canvas);
      return size;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
      Insets insets = c.getInsets();
      Dimension size = c.getSize();
      size.height -= insets.top + insets.bottom;
      size.width -= insets.left + insets.right;
//      g.translate(-insets.left, -insets.top);
      g.setClip(insets.left, insets.top, size.width, size.height);
//      g.setClip(0, 0, size.width, size.height);
      AwtUtil.applyRenderingHints(g);
      ASingleCell cell = Util.cast(ASingleCell.class, c);
      CellState state = cell.getCurrentState();
      CanvasComponent.Simple canvasComponent = cell.getCanvasComponent();
      CanvasImpl canvas = canvasComponent.useCanvas(state);
      cell.renderValueOn(canvas, state);
      canvas.paint(g, size, canvasComponent);
      canvasComponent.releaseCanvas(canvas);
    }
  }
}
