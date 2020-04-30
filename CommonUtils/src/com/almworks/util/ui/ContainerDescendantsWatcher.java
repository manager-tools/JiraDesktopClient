package com.almworks.util.ui;

import com.almworks.util.commons.AsyncSingleObjectPool;
import com.almworks.util.commons.Procedure;

import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

public class ContainerDescendantsWatcher implements ContainerListener {
  private static final AsyncSingleObjectPool<StartWatch> ourStartWatchPool =
    AsyncSingleObjectPool.awtNewInstance(StartWatch.class);
  private static final AsyncSingleObjectPool<StopWatch> ourStopWatchPool =
    AsyncSingleObjectPool.awtNewInstance(StopWatch.class);

  public final void componentAdded(ContainerEvent e) {
    Component component = e.getChild();
    if (!(component instanceof Container))
      return;
    watchSubTree((Container) component);
  }

  public final void componentRemoved(ContainerEvent e) {
    Component component = e.getChild();
    if (!(component instanceof Container))
      return;
    stopWatchSubTree((Container) component);
  }

  public final void watchSubTree(Container container) {
    if (shouldWatchDescendants(container)) {
      watchStrictDescendants(container);
      watchContainer(container);
    } else
      onStartWatchDescendant(container);
  }

  protected final void watchStrictDescendants(Container container) {
    applyToDescendantsImpl(container, true, ourStartWatchPool);
  }

  private <P extends ContainerProcedure> void applyToDescendantsImpl(Container ancestor, boolean strict, AsyncSingleObjectPool<P> pool) {
    P procedure = pool.getInstance();
    procedure.setWatcher(this);
    applyToStrictDescendants(ancestor, procedure);
    if (!strict)
      procedure.invoke(ancestor);
    procedure.setWatcher(null);
    pool.releaseInstance(procedure);
  }

  protected void applyToStrictDescendants(Container ancestor, Procedure<Container> procedure) {
    if (!shouldWatchDescendants(ancestor))
      return;
    for (int i = 0; i < ancestor.getComponentCount(); i++) {
      Component component = ancestor.getComponent(i);
      if (component instanceof Container) {
        Container container = (Container) component;
        procedure.invoke(container);
        applyToStrictDescendants(container, procedure);
      }
    }
  }

  protected final void watchContainer(Container component) {
    onStartWatchDescendant(component);
    if (shouldWatchDescendants(component))
      ((Container) component).addContainerListener(this);
  }

  protected final void stopWatchSubTree(Container container) {
    applyToDescendantsImpl(container, false, ourStopWatchPool);
  }

  protected final void stopWatchContainer(Container descentant) {
    descentant.removeContainerListener(this);
    onStopWatchDescendant(descentant);
  }

  /**
   * A container appears in watched scope
   */
  protected void onStartWatchDescendant(Container descendant) {}

  /**
   * A previousdly watched container disappears from watched scope
   * @param descentant
   */
  protected void onStopWatchDescendant(Container descentant) {}

  /**
   * Override to reduce watched scope
   * @param container
   * @return true if watcher should watch descendants of given container.
   * The container itself is already in watched scope.
   */
  protected boolean shouldWatchDescendants(Container container) {
    return true;
  }

  private static abstract class ContainerProcedure implements Procedure<Container> {
    protected ContainerDescendantsWatcher myWatcher;

    public void setWatcher(ContainerDescendantsWatcher watcher) {
      myWatcher = watcher;
    }
  }

  public static final class StartWatch extends ContainerProcedure {
    public void invoke(Container arg) {
      myWatcher.watchContainer(arg);
    }
  }

  public static final class StopWatch extends ContainerProcedure {
    public void invoke(Container arg) {
      myWatcher.stopWatchContainer(arg);
    }
  }
}
