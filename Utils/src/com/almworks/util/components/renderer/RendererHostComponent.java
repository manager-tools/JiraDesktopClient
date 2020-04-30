package com.almworks.util.components.renderer;

import com.almworks.util.components.Canvas;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.ui.MegaMouseAdapter;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public class RendererHostComponent extends JComponent implements WidthDrivenComponent, Highlightable {
  private final RendererContext myContext = new RendererContext(this);
  private final RendererActivityController myActivityController = new RendererActivityController(this);
  private final Lifecycle mySwingLife = new Lifecycle(false);
  private final Lifecycle myRendererDisplayable = new Lifecycle(false);

  private Renderer myRenderer;
  private GlassPainter myGlassPainter;

  public RendererHostComponent() {
    updateUI();
    setFocusable(false);
    MyMouseAdapter listener = new MyMouseAdapter();
    addMouseListener(listener);
    addMouseMotionListener(listener);
    new MyFocusTraversalPolicy().install(this);
  }

  public RendererActivityController getActivityController() {
    return myActivityController;
  }

  public void updateUI() {
    super.updateUI();
    LookAndFeel.installColorsAndFont(this, "TextArea.background", "TextArea.foreground", "Label.font");
  }

  public void addNotify() {
    super.addNotify();
    mySwingLife.cycleStart();
    UIUtil.addGlobalFocusOwnerListener(mySwingLife.lifespan(), new MyFocusListener());
    startGlassPainter();
  }

  private void startGlassPainter() {
    if (myRenderer != null) {
      myRendererDisplayable.cycleStart();
      assert myGlassPainter == null;
      Lifespan life = myRendererDisplayable.lifespan();
      myGlassPainter = GlassPainter.install(life, myRenderer, this);
      life.add(new Detach() {
        protected void doDetach() throws Exception {
          myGlassPainter = null;
        }
      });
    }
  }

  public void removeNotify() {
    mySwingLife.cycleEnd();
    myRendererDisplayable.cycleEnd();
    super.removeNotify();
  }

  @SuppressWarnings({"Deprecation"})
  @Deprecated
  public void layout() {
    myActivityController.layout();
  }

  public Dimension getPreferredSize() {
    if (myContext == null || myRenderer == null) {
      return super.getPreferredSize();
    }
    Dimension preferredSize = myRenderer.getPreferredSize(myContext);
    return AwtUtil.addInsets(preferredSize, getInsets());
  }

  public int getPreferredWidth() {
    if (myRenderer != null) {
      int width = myRenderer.getPreferedWidth(myContext);
      return width >= 0 ? width : getPreferredSize().width;
    }
    return 0;
  }

  public int getPreferredHeight(int width) {
    assert false : "not implemented";
    if (myRenderer != null) {
      int height = myRenderer.getPreferedHeight(width, myContext);
      return height >= 0 ? height : getPreferredSize().height;
    }
    return 0;
  }

  @NotNull
  public JComponent getComponent() {
    return this;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible();
  }

  public void requestFocus() {
    requestFocus(false);
  }

  public boolean requestFocus(boolean temporary) {
    return requestFocusImpl();
  }

  private boolean requestFocusImpl() {
    if (SwingTreeUtil.isAncestorOfFocusOwner(this))
      return false;
    getFocusTraversalPolicy().getDefaultComponent(this).requestFocus();
    return true;
  }

  public boolean requestFocusInWindow() {
    return requestFocusInWindow(false);
  }

  protected boolean requestFocusInWindow(boolean temporary) {
    return requestFocusImpl();
  }

  public void setRenderer(Renderer renderer) {
    myRendererDisplayable.cycleEnd();
    myRenderer = renderer;
    if (myContext != null)
      myContext.resetCaches();
//    setFocusable(myRenderer != null && myRenderer.isFocusable());
    invalidate();
    revalidate();
    repaint();
    if (isDisplayable() && renderer != null)
      startGlassPainter();
  }

  protected void paintComponent(Graphics g) {
    if (myContext != null && myRenderer != null) {
      AwtUtil.applyRenderingHints(g);
      // todo ask dyoma
      Shape savedClip = AwtUtil.clipClientArea(g, this);
      if (savedClip == null)
        return;
      myRenderer.paint(g, myContext);
      AwtUtil.restoreClip(g, this, savedClip);
    }
  }

  public void setFont(Font font) {
    super.setFont(font);
    if (myContext != null)
      myContext.resetCaches();
  }

  @SuppressWarnings({"MethodOverloadsMethodOfSuperclass"})
  public FontMetrics getFontMetrics(int fontStyle) {
    return getFontMetrics(getFont().deriveFont(fontStyle));
  }

  private void updateMouseActivity(MouseEvent e) {
    if (myRenderer == null || myContext == null)
      return;
    Insets insets = getInsets();
    int id = e.getID();
    if (myRenderer.updateMouseActivity(id, e.getX() - insets.left, e.getY() - insets.top, myContext, myActivityController))
      e.consume();
  }

  private void dispatchMouseEvent(MouseEvent e) {
    if (e.isConsumed())
      return;
    if (myRenderer == null || myContext == null)
      return;
    Insets insets = getInsets();
    int id = e.getID();
    myRenderer.processMouseEvent(id, e.getX() - insets.left, e.getY() - insets.top, myContext, myActivityController);
  }

  public void invalidateRenderer() {
    myContext.resetCaches();
    if (myGlassPainter != null)
      myGlassPainter.rendererInvalidated();
    invalidate();
    revalidate();
    repaint();
  }

  @Deprecated
  public void reshape(int x, int y, int w, int h) {
    if (x != getX() || y != getY() || w != getWidth() || h != getHeight())
      myContext.resetCaches();
    super.reshape(x, y, w, h);
  }

  public <T> void putValue(Lifespan lifespan, final TypedKey<? extends T> key, final T value) {
    myContext.putValue(key, value);
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        T currentValue = myContext.getValue(key);
        if (value == currentValue)
          myContext.putValue(key, value);
      }
    });
  }

  public void setHighlightPattern(Pattern pattern) {
    myContext.putValue(Canvas.PATTERN_PROPERTY, pattern);
    repaint();
  }

  private class MyMouseAdapter extends MegaMouseAdapter {
    public void mouseClicked(MouseEvent e) {
      updateMouseActivity(e);
      dispatchMouseEvent(e);
    }

    public void mouseExited(MouseEvent e) {
      myActivityController.removeMouseActivity();
      dispatchMouseEvent(e);
    }

    public void mouseMoved(MouseEvent e) {
      if (!myActivityController.hasActivity(e))
        updateMouseActivity(e);
      dispatchMouseEvent(e);
    }
  }

  private static class MyFocusListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
//      evt.
    }
  }

  private class MyFocusTraversalPolicy extends FocusTraversalPolicy {
    private final FocusTargetComponent myFirstFocus = new FocusTargetComponent(true, "first");
    private final FocusTargetComponent myLastFocus = new FocusTargetComponent(false, "last");
    private final FocusTargetComponent myNextFocus = new FocusTargetComponent(true, "next");
    private final FocusTargetComponent myPrevFocus = new FocusTargetComponent(false, "prev");

    @Nullable
    public Component getComponentAfter(Container aContainer, Component aComponent) {
      return aComponent != myNextFocus ? myNextFocus : null;
    }

    @Nullable
    public Component getComponentBefore(Container aContainer, Component aComponent) {
      return aComponent != myPrevFocus ? myPrevFocus : null;
    }

    public Component getFirstComponent(Container aContainer) {
      return myFirstFocus;
    }

    public Component getLastComponent(Container aContainer) {
      return myLastFocus;
    }

    public void install(JComponent host) {
      host.setFocusTraversalPolicy(this);
//      host.setFocusCycleRoot(true);
      host.setFocusTraversalPolicyProvider(true);
      myPrevFocus.addToHost(host);
      myNextFocus.addToHost(host);
    }

    public Component getDefaultComponent(Container aContainer) {
      return myNextFocus;
    }

    private class FocusTargetComponent extends JComponent {
      private final boolean myForward;
      private final String myDebugName;

      public FocusTargetComponent(boolean forward, String debugName) {
        myForward = forward;
        myDebugName = debugName;
        setFocusable(true);
      }

      public boolean requestFocusInWindow() {
        return requestFocusImpl();
      }

      private boolean requestFocusImpl() {
        if (myContext == null || myRenderer == null)
          return false;
        if (myActivityController.focusNextComponent(myContext, myRenderer, myForward))
          return true;
        Container provider = SwingTreeUtil.findFocusPolicyProvider(RendererHostComponent.this.getParent(),
          RendererHostComponent.this.getFocusCycleRootAncestor());
        Container rootAncestor = RendererHostComponent.this.getFocusCycleRootAncestor();
        FocusTraversalPolicy policy = rootAncestor.getFocusTraversalPolicy();
        if (provider == null)
          provider = rootAncestor;
        Component nextComponent = myForward ? policy.getComponentAfter(provider, this) :
          policy.getComponentBefore(provider, RendererHostComponent.this);
        if (nextComponent == null)
          nextComponent = myForward ? policy.getFirstComponent(rootAncestor) : policy.getLastComponent(rootAncestor);
        if (nextComponent == null)
          nextComponent = policy.getDefaultComponent(rootAncestor);
        if (nextComponent != null)
          nextComponent.requestFocus();

        return true;
      }

      public void requestFocus() {
        requestFocusImpl();
      }

      public boolean requestFocus(boolean temporary) {
        return requestFocusImpl();
      }

      public void addToHost(JComponent host) {
        host.add(this);
        if (myForward)
          setBounds(Short.MAX_VALUE - 2, Short.MAX_VALUE - 2, 1, 1);
        else
          setBounds(0, -2, 1, 1);
      }

      public void paint(Graphics g) {
      }

      public void invalidate() {
      }

      public void repaint(long tm, int x, int y, int width, int height) {
      }

      @Deprecated
      public void layout() {
      }
    }
  }

  private static class GlassPainter extends Detach implements Renderer.Listener {
    private final Renderer myRenderer;
    private ImageComponent myImageComponent;
    private Rectangle myLastRect;
    private final JViewport myViewport;
    private final RendererHostComponent myHost;

    public GlassPainter(RendererHostComponent host, Renderer renderer, JViewport viewport) {
      myRenderer = renderer;
      myViewport = viewport;
      myHost = host;
    }

    @Nullable
    public static GlassPainter install(Lifespan lifespan, Renderer renderer, RendererHostComponent host) {
      if (renderer == null || lifespan.isEnded())
        return null;
      Container parent = host.getParent();
      if (parent instanceof ScrollablePanel)
        parent = parent.getParent();
      if (!(parent instanceof JViewport))
        return null;
      GlassPainter glassPainter = new GlassPainter(host, renderer, (JViewport) parent);
      renderer.addAWTListener(lifespan, glassPainter);
      lifespan.add(glassPainter);
      return glassPainter;
    }

    protected void doDetach() throws Exception {
      removeImageComponent();
    }

    public void onMouseOverRectangle(Rectangle rect) {
      if (myLastRect != null) {
        if (rect == null) {
          removeImageComponent();
          return;
        }
        if (myLastRect.equals(rect))
          return;
      }
      if (rect == null)
        return;
      Rectangle viewRect = myViewport.getViewRect();
      Insets insets = myHost.getInsets();
      int rightVisible = viewRect.x + viewRect.width - insets.left;
      if (rightVisible >= rect.width) {
        removeImageComponent();
        return;
      }
      Rectangle lifeRect = getActivityController().getLiveRectanle();
      if (lifeRect != null && lifeRect.intersects(rect)) {
        removeImageComponent();
        return;
      }
      JRootPane rootPane = SwingTreeUtil.findAncestorOfType(myViewport, JRootPane.class);
      if (rootPane == null)
        return;
      if (myImageComponent == null)
        myImageComponent = new ImageComponent();
      if (myImageComponent.getParent() != rootPane.getLayeredPane())
        rootPane.getLayeredPane().add(myImageComponent, JLayeredPane.DEFAULT_LAYER);
      myLastRect = new Rectangle(rect);
      int width = rect.width - rightVisible;
      assert width > 0;
      int x = rect.x + rightVisible;
      BufferedImage img = new BufferedImage(width, rect.height, BufferedImage.TYPE_INT_RGB);
      Graphics g = img.getGraphics();
      AwtUtil.applyRenderingHints(g);
      g.setColor(myHost.getBackground());
      g.fillRect(0, 0, img.getWidth(), img.getHeight());
      g.setColor(myHost.getForeground());
      g.translate(-x, -rect.y);
      myRenderer.paint(g, getContext());
      Point point = SwingUtilities.convertPoint(myHost, new Point(x, rect.y), rootPane);
      myImageComponent.setImage(point.x + insets.left, point.y + insets.top, img);
    }

    private RendererContext getContext() {
      return myHost.myContext;
    }

    private RendererActivityController getActivityController() {
      return myHost.getActivityController();
    }

    private void removeImageComponent() {
      if (myImageComponent != null) {
        Container parent = myImageComponent.getParent();
        if (parent != null) {
          parent.remove(myImageComponent);
          myLastRect = null;
          parent.repaint();
        }
      }
    }

    public void rendererInvalidated() {
      removeImageComponent();
    }
  }

  private static class ImageComponent extends JComponent {
    private BufferedImage myImage;
    private int myX;
    private int myY;

    public void setImage(int x, int y, BufferedImage image) {
      myImage = image;
      myX = x;
      myY = y;
      invalidate();
      revalidate();
      repaint();
    }

    @Deprecated
    public void layout() {
      if (myImage != null)
        setBounds(myX, myY, myImage.getWidth(), myImage.getHeight());
      else
        setBounds(-1, -1, 0, 0);
    }

    public void paint(Graphics g) {
      if (myImage != null)
        g.drawImage(myImage, 0, 0, null);
    }
  }
}
