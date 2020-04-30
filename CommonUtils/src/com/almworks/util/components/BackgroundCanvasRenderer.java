package com.almworks.util.components;

import com.almworks.util.commons.AsyncSingleObjectPool;
import com.almworks.util.components.renderer.CanvasImpl;
import com.almworks.util.components.renderer.DefaultCanvasComponent;
import com.almworks.util.components.renderer.ListCellState;
import com.almworks.util.components.renderer.RootAttributes;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class BackgroundCanvasRenderer<T> extends BaseRendererComponent {
  private final DefaultCanvasComponent myCanvas = new DefaultCanvasComponent();
  @Nullable
  private Component myBackground = null;
  @Nullable
  private CanvasRenderer<? super T> myRenderer = null;
  private boolean myNeedsLayout = true;
  protected static final AsyncSingleObjectPool<Rectangle> TEMP_RECTANGLE =
    AsyncSingleObjectPool.awtNewInstance(Rectangle.class);

  public BackgroundCanvasRenderer() {
    add(myCanvas);
    setOpaque(false);
    myCanvas.setOpaque(false);
  }

  public void validate() {
    doLayout();
  }

  @Deprecated
  public void reshape(int x, int y, int w, int h) {
    myNeedsLayout |= w != getWidth() || h != getHeight();
    super.reshape(x, y, w, h);
  }

  public void paint(Graphics g) {
    if (myNeedsLayout)
      validate();
    AwtUtil.applyRenderingHints(g);
    Rectangle rect = TEMP_RECTANGLE.getInstance();
    try {
      paintChild(g, myBackground, rect);
      paintChild(g, myCanvas, rect);
    } finally {
      TEMP_RECTANGLE.releaseInstance(rect);
    }
  }

  protected void paintChild(Graphics g, Component component, Rectangle tmp) {
    if (component == null)
      return;
    tmp = component.getBounds(tmp);
    if (!g.hitClip(tmp.x, tmp.y, tmp.width, tmp.height))
      return;
    Graphics graphics = g.create(tmp.x, tmp.y, tmp.width, tmp.height);
    try {
      component.paint(graphics);
    } finally {
      graphics.dispose();
    }
  }

  public void prepareCanvas(ListCellState cellState, T item) {
    CanvasImpl canvas = myCanvas.prepareCanvas(cellState);
    if (myBackground != null) {
      canvas.setForeground(myBackground.getForeground());
      canvas.setCanvasBorder(null);
      canvas.setCanvasBackground(null);
      canvas.setBackground(null);
      canvas.getCurrentSection().setBackground(null);
      myCanvas.setOpaque(false);
    }
    if (myRenderer != null) {
      myRenderer.renderStateOn(cellState, canvas, item);
    } else if (item instanceof CanvasRenderable) {
      ((CanvasRenderable) item).renderOn(canvas, cellState);
    } else {
      canvas.appendText(item != null ? item.toString() : "");
    }
    setToolTipText(canvas.getTooltip());
  }

  public void setBackgroundComponent(Component bg) {
    if (myBackground != bg) {
      if (myBackground != null) remove(myBackground);
      myBackground = bg;
      if (myBackground != null) add(myBackground);
    }
    if (myBackground != null) {
      RootAttributes attributes = myCanvas.getCanvasAttributes();
      attributes.myBackground = null;
      myCanvas.setOpaque(false);
    } else {
      myCanvas.setOpaque(true);
    }
  }

  public boolean setRenderer(CanvasRenderer<? super T> renderer) {
    if (renderer == myRenderer)
      return false;
    myRenderer = renderer;
    return true;
  }

  @Nullable
  public CanvasRenderer<? super T> getRenderer() {
    return myRenderer;
  }

  public Dimension getPreferredSize() {
    Dimension background = myBackground != null ? myBackground.getPreferredSize() : new Dimension();
    int bgInsetsHeight;
    int bgInsetsWidth;
    if (myBackground instanceof JComponent) {
      JComponent bgComponent = (JComponent) myBackground;
      bgInsetsHeight = AwtUtil.getInsetHeight(bgComponent);
      bgInsetsWidth = AwtUtil.getInsetWidth(bgComponent);
    } else {
      bgInsetsHeight = 0;
      bgInsetsWidth = 0;
    }
    Dimension size = myCanvas.getPreferredSize();
    size.width += bgInsetsWidth;
    size.height = Math.max(background.height, size.height + bgInsetsHeight);
    return size;
  }

  @Deprecated
  public final void layout() {
    layoutRenderer();
    myNeedsLayout = false;
  }

  protected void layoutRenderer() {
    Insets bgInsets = getBackgroundInsets();
    int width = getWidth();
    int height = getHeight();
    int clientWidth = width - AwtUtil.getInsetWidth(bgInsets);
    int clintHieght = height - AwtUtil.getInsetHeight(bgInsets);
    setBoundsBackground(0, 0, width, height);
    setBoundsCanvas(bgInsets.left, bgInsets.top, clientWidth, clintHieght);
  }

  protected Insets getBackgroundInsets() {
    Component bg = myBackground;
    return (bg instanceof JComponent) ? ((JComponent) bg).getInsets(new Insets(0, 0, 0, 0)) : AwtUtil.EMPTY_INSETS;
  }

  protected void setBoundsCanvas(int x, int y, int width, int height) {
    AwtUtil.setBounds(myCanvas, x, y, width, height);
  }

  protected void setBoundsBackground(int x, int y, int width, int height) {
    if (myBackground != null)
      AwtUtil.setBounds(myBackground, x, y, width, height);
  }

  public void transferCanvasDecorationsToBackground() {
    if(myBackground != null) {
      final CanvasImpl canvas = myCanvas.getCanvas();
      transferBackgroundColor(canvas);
      transferBorder(canvas);
    }
  }

  private void transferBackgroundColor(CanvasImpl canvas) {
    final Color bg = canvas.getCanvasBackground();
    if(bg != null) {
      canvas.setCanvasBackground(null);
      //noinspection ConstantConditions
      myBackground.setBackground(bg);
    }
  }

  private void transferBorder(CanvasImpl canvas) {
    final Border border = canvas.getCanvasBorder();
    if(border == null) {
      return;
    }
    canvas.setCanvasBorder(null);
    if((myBackground instanceof JComponent) && !needsWrapping(myBackground)) {
      final JComponent jc = (JComponent) myBackground;
      //noinspection ConstantConditions
      jc.setBorder(BorderFactory.createCompoundBorder(border, jc.getBorder()));
    } else {
      myBackground = new BorderWrapper(myBackground, border);
    }
  }

  private boolean needsWrapping(Component background) {
    return "com.apple.laf.AquaComboBoxRenderer".equals(background.getClass().getCanonicalName());
  }

  private static class BorderWrapper extends JComponent {
    final Component myContent;
    final Border myBorder;

    public BorderWrapper(Component content, Border border) {
      myContent = content;
      myBorder = border;
      setLayout(new BorderLayout());
      add(content, BorderLayout.CENTER);
      setBorder(border);
    }

    @Override
    public Insets getInsets(Insets insets) {
      if(myContent instanceof JComponent) {
        insets = ((JComponent)myContent).getInsets(insets);
      } else {
        insets.top = insets.left = insets.bottom = insets.right = 0;
      }
      final Insets border = myBorder.getBorderInsets(myContent);
      insets.top += border.top;
      insets.left += border.left;
      insets.bottom += border.bottom;
      insets.right += border.right;
      return insets;
    }
  }
}
