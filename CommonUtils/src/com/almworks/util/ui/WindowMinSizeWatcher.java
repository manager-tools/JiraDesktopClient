package com.almworks.util.ui;

import com.almworks.util.Env;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.Threads;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Watches descendant component tree and updates window minimum size. To install use {@link #install(java.awt.Window)}
 */
public class WindowMinSizeWatcher extends ContainerDescendantsWatcher
  implements Runnable, ComponentListener, PropertyChangeListener
{
  private final Bottleneck myBottleneck = new Bottleneck(30, ThreadGate.AWT_QUEUED, this);
  private final Window myWindow;

  private WindowMinSizeWatcher(Window window) {
    myWindow = window;
  }

  protected void onStartWatchDescendant(Container descendant) {
    requestUpdate();
    descendant.addComponentListener(this);
    watchSpecificProperties(descendant);
  }

  protected boolean shouldWatchDescendants(Container container) {
    return !(container instanceof JViewport);
  }

  protected void onStopWatchDescendant(Container descentant) {
    requestUpdate();
    stopWatchSpecificProperties(descentant);
  }

  private void stopWatchSpecificProperties(Container descentant) {
    if (descentant instanceof JLabel)
      descentant.removePropertyChangeListener("text", this);
  }

  private void watchSpecificProperties(Container descendant) {
    if (descendant instanceof JLabel)
      descendant.addPropertyChangeListener("text", this);
  }

  private void requestUpdate() {
    myBottleneck.requestDelayed();
  }

  public void run() {
    Threads.assertAWTThread();
    Dimension maxSize = myWindow.getGraphicsConfiguration().getBounds().getSize();
    Component root = getRootPane();
    Dimension minSize = root.getMinimumSize();
    Insets insets = myWindow.getInsets();
    minSize.width += insets.left + insets.right;
    minSize.height += insets.top + insets.bottom;
    minSize.width = Math.min(minSize.width, maxSize.width);
    minSize.height = Math.min(minSize.height, maxSize.height);
    myWindow.setMinimumSize(minSize);
  }

  private Component getRootPane() {
    assert myWindow.getComponentCount() == 1 : myWindow.getComponentCount() + " " + myWindow;
    assert myWindow.getComponent(0) instanceof JRootPane : myWindow.getComponent(0) + " " + myWindow;
    return myWindow.getComponent(0);
  }

  public static <W extends Window & RootPaneContainer> void install(W window) {
    if (Env.getJavaSpecificationVersion() < 6)
      return;
    new WindowMinSizeWatcher(window).watchSubTree(window.getRootPane());
  }

  public void propertyChange(PropertyChangeEvent evt) {
    requestUpdate();
  }

  public void componentResized(ComponentEvent e) {
    processComponentEvent(e);
  }

  public void componentMoved(ComponentEvent e) {
    processComponentEvent(e);
  }

  public void componentShown(ComponentEvent e) {
    processComponentEvent(e);
  }

  public void componentHidden(ComponentEvent e) {
    processComponentEvent(e);
  }

  private void processComponentEvent(ComponentEvent e) {
    requestUpdate();
  }
}
