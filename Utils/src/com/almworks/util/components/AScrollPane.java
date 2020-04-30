package com.almworks.util.components;

import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.dnd.DndUtil;
import org.almworks.util.Failure;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AScrollPane extends JScrollPane implements SizeDelegating {
  private static final ComponentProperty<MouseWheelDispatcher> WHEEL_FORWARD = ComponentProperty.createProperty("wheelForward");
  private SizeDelegate mySizeDelegate = null;
  private boolean myDndActive;
  private boolean myAdaptiveVerticalScroll = false;

  public AScrollPane() {
    this(null);
  }

  public AScrollPane(Component view) {
    super(view);
    new ScrollBarListener().install(this);
  }

  public AScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
    super(view, vsbPolicy, hsbPolicy);
  }

  public Dimension getMaximumSize() {
    return SizeDelegate.maximum(this, mySizeDelegate, super.getMaximumSize());
  }

  public Dimension getMinimumSize() {
    return SizeDelegate.minimum(this, mySizeDelegate, super.getMinimumSize());
  }

  public Dimension getPreferredSize() {
    return SizeDelegate.preferred(this, mySizeDelegate, super.getPreferredSize());
  }

  public void setSizeDelegate(SizeDelegate delegate) {
    mySizeDelegate = delegate;
    revalidate();
  }

  public Dimension getSuperSize(SizeKey sizeKey) {
    if (sizeKey == SizeKey.PREFERRED)
      return super.getPreferredSize();
    else if (sizeKey == SizeKey.MAXIMUM)
      return super.getMaximumSize();
    else if (sizeKey == SizeKey.MINIMUM)
      return super.getMinimumSize();
    else
      throw new Failure(sizeKey.toString());
  }

  public Dimension getSize(SizeKey sizeKey) {
    return sizeKey.getFrom(this);
  }

  /**
   * When adaptive vertical scroll is on, the scrollpane doesn't intercept mouse wheel events when vertical scroll isn't
   * possible (vertical scrollbar is hidden or disabled).<br>
   * Note: if vertical scrollbar is never shown and adaptive scrolling is on scrolling with mouse wont be allowed even
   * when view is larger then viewable area.
   * @param adaptiveVerticalScroll new value of adaptive vertical scroll property
   */
  public void setAdaptiveVerticalScroll(boolean adaptiveVerticalScroll) {
    myAdaptiveVerticalScroll = adaptiveVerticalScroll;
    updateWheelForward();
  }

  /**
   * @see #setAdaptiveVerticalScroll(boolean)
   */
  public boolean isAdaptiveVerticalScroll() {
    return myAdaptiveVerticalScroll;
  }

  private void updateWheelForward() {
    if (forwardWheel()) getWheelForward().ensureAddded();
    else getWheelForward().ensureRemoved();
  }

  @Override
  public void addMouseWheelListener(MouseWheelListener l) {
    getWheelForward().addListener(l);
    if (forwardWheel()) getWheelForward().ensureAddded();
  }

  @Override
  public void removeMouseWheelListener(MouseWheelListener l) {
    getWheelForward().removeListener(l);
  }

  private void superAddMouseWheelListener(MouseWheelListener listener) {
    super.addMouseWheelListener(listener);
  }

  private void superRemoveMouseWheelListener(MouseWheelListener listener) {
    super.removeMouseWheelListener(listener);
  }

  private boolean forwardWheel() {
    if (!myAdaptiveVerticalScroll) return true;
    JScrollBar bar = getVerticalScrollBar();
    return bar != null && bar.isVisible() && bar.isEnabled() && hasSomethingToScroll(bar);
  }

  private boolean hasSomethingToScroll(JScrollBar bar) {
    if(Aqua.isAqua()) {
      // this is how AquaScrollBarUI decides
      return bar.getMaximum() - bar.getMinimum() - bar.getModel().getExtent() > 0;
    }
    return true;
  }

  public void setDndActive(boolean dndActive) {
    if (myDndActive != dndActive) {
      myDndActive = dndActive;
      repaint();
    }
  }


  public void paint(Graphics g) {
    super.paint(g);
    Graphics2D sg = (Graphics2D) g.create();
    try {
      paintDropReady(sg);
    } finally {
      sg.dispose();
    }
  }

  private void paintDropReady(Graphics2D g2) {
    if (myDndActive) {
      DndUtil.paintDropBorder(g2, this);
    }
  }

  private MouseWheelDispatcher getWheelForward() {
    AScrollPane.MouseWheelDispatcher dispatcher = WHEEL_FORWARD.getClientValue(this);
    if (dispatcher == null) {
      dispatcher = new MouseWheelDispatcher();
      WHEEL_FORWARD.putClientValue(this, dispatcher);
    }
    return dispatcher;
  }

  private class MouseWheelDispatcher implements MouseWheelListener {
    private boolean myAdded = false;
    private MouseWheelListener myMouseWheelListeners = null;

    public void mouseWheelMoved(MouseWheelEvent e) {
      if (myMouseWheelListeners != null) myMouseWheelListeners.mouseWheelMoved(e);
      else assert false;
    }

    public void ensureAddded() {
      if (!myAdded && myMouseWheelListeners != null) {
        myAdded = true;
        superAddMouseWheelListener(this);
      }
    }

    public void ensureRemoved() {
      if (myAdded) {
        myAdded = false;
        superRemoveMouseWheelListener(this);
      }
    }

    public void addListener(MouseWheelListener l) {
      myMouseWheelListeners = AWTEventMulticaster.add(myMouseWheelListeners, l);
    }

    public void removeListener(MouseWheelListener l) {
      myMouseWheelListeners = AWTEventMulticaster.remove(myMouseWheelListeners, l);
      if (myMouseWheelListeners == null)
        superRemoveMouseWheelListener(this);
    }
  }


  private class ScrollBarListener extends ComponentAdapter implements PropertyChangeListener, ChangeListener {
    private JScrollBar myCurrentBar = null;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      Object source = evt.getSource();
      String name = evt.getPropertyName();
      if (source == AScrollPane.this && "verticalScrollBar".equals(name)) {
        subscribeToScrollBar((JScrollBar)evt.getNewValue());
      } else if (source == myCurrentBar) {
        if("enabled".equals(name)) {
          updateWheelForward();
        } else if("model".equals(name)) {
          subscribeToScrollBar(myCurrentBar);
        }
      }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      updateWheelForward();
    }

    @Override
    public void componentShown(ComponentEvent e) {
      componentVisibilityChanged(e);
    }

    @Override
    public void componentHidden(ComponentEvent e) {
      componentVisibilityChanged(e);
    }

    private void componentVisibilityChanged(ComponentEvent e) {
      if (e.getSource() == myCurrentBar) updateWheelForward();
    }

    private void subscribeToScrollBar(JScrollBar bar) {
      if (myCurrentBar != null) {
        myCurrentBar.removePropertyChangeListener(this);
        myCurrentBar.removeComponentListener(this);
        if(Aqua.isAqua()) {
          myCurrentBar.getModel().removeChangeListener(this);
        }
      }
      myCurrentBar = bar;
      if (myCurrentBar != null) {
        myCurrentBar.addPropertyChangeListener(this);
        myCurrentBar.addComponentListener(this);
        if(Aqua.isAqua()) {
          myCurrentBar.getModel().addChangeListener(this);
        }
      }
      updateWheelForward();
    }

    public void install(AScrollPane component) {
      component.addPropertyChangeListener(this);
      subscribeToScrollBar(getVerticalScrollBar());
    }
  }
}
