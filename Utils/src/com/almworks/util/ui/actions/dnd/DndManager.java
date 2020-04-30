package com.almworks.util.ui.actions.dnd;

import com.almworks.appinit.AWTEventPreprocessor;
import com.almworks.appinit.EventQueueReplacement;
import com.almworks.util.commons.Factory;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.InstanceProvider;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.ref.SoftReference;
import java.util.List;

public class DndManager implements AWTEventPreprocessor {
  private final List<DndTarget> myTargets = Collections15.arrayList();

  private DragContext myCurrentDrag;
  private DragImageComponent myDragContentImage = null;
  private DragImageComponent myDragActionImage = null;
  private SoftReference<MouseEvent> myLastEvent;

  public DndManager() {
  }

  public void registerTarget(Lifespan lifespan, final DndTarget target) {
    assert !myTargets.contains(target);
    myTargets.add(target);
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        myTargets.remove(target);
      }
    });
  }

  public void registerSource(Lifespan lifespan, JComponent source) {
    UIUtil.addMouseMotionListener(lifespan, source, new MouseMotionListener() {
      public void mouseDragged(MouseEvent e) {
        updateDrag(e);
      }

      public void mouseMoved(MouseEvent e) {
      }
    });
  }

  private void updateDrag(MouseEvent e) {
    DragContext context = myCurrentDrag;
    if (context == null) {
      return;
    }
//    if (e.getComponent() != context.getComponent()) {
//      assert false : e.getComponent() + " " + context.getComponent();
//      return;
//    }
    notifyTargets(new DndEvent(true, context, e));
    updateDragImage(e);
  }

  private void notifyTargets(DndEvent event) {
    for (DndTarget target : myTargets) {
      target.dragNotify(event);
    }
  }

  public void dragStarted(InputEvent event, DragContext context) {
    assert myCurrentDrag == null : myCurrentDrag + " " + context;
    myCurrentDrag = context;
    notifyTargets(new DndEvent(true, context, event));
    updateDragImage(event);
  }

  private void updateDragImage(InputEvent event) {
    boolean clearDrags = true;
    if (event instanceof MouseEvent) {
      Component c = event.getComponent();
      DragContext currentDrag = myCurrentDrag;
      Factory<Image> contentFactory = currentDrag == null ? null : currentDrag.getValue(DndUtil.CONTENT_IMAGE_FACTORY);
      Factory<Image> actionFactory = currentDrag == null ? null : currentDrag.getValue(DndUtil.ACTION_IMAGE_FACTORY);
      if (c != null && (contentFactory != null || actionFactory != null)) {
        Window window = c instanceof Window ? ((Window) c) : SwingUtilities.getWindowAncestor(c);
        JLayeredPane layeredPane = null;
        if (window instanceof JFrame) {
          layeredPane = ((JFrame) window).getLayeredPane();
        } else if (window instanceof JDialog) {
          layeredPane = ((JDialog) window).getLayeredPane();
        }
        if (layeredPane != null) {
          MouseEvent me = (MouseEvent) event;
          myDragContentImage =
            updateDragImageComponent(myDragContentImage, contentFactory, layeredPane, me, currentDrag, null);
          myDragActionImage = updateDragImageComponent(myDragActionImage, actionFactory, layeredPane, me, currentDrag,
            myDragContentImage);
          clearDrags = false;
        }
      }
    }
    if (clearDrags) {
      clearDragImage();
    }
  }

  private DragImageComponent updateDragImageComponent(DragImageComponent prev, Factory<Image> factory,
    JLayeredPane layeredPane, MouseEvent event, DragContext currentDrag, DragImageComponent preceding)
  {
    if (factory == null) {
      removeDragImageComponent(prev);
      return null;
    }
    DragImageComponent r = prev;
    if (prev == null || !Util.equals(prev.getImageFactory(), factory)) {
      removeDragImageComponent(prev);
      Image image = factory.create();
      if (image == null) {
        return null;
      }
      r = new DragImageComponent(factory, image);
    }
    Container container = r.getParent();
    if (container != layeredPane) {
      if (container != null) {
        container.remove(r);
        if (container.isVisible()) {
          Rectangle bounds = r.getBounds();
          container.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
        }
      }
      layeredPane.add(r, JLayeredPane.DRAG_LAYER);
    }
    if (preceding == null) {
      r.setMousePoint(event, currentDrag);
    } else {
      r.setRelative(preceding);
    }
    return r;
  }

  private void removeDragImageComponent(DragImageComponent prev) {
    if (prev != null) {
      Container container = prev.getParent();
      if (container != null) {
        Rectangle bounds = prev.getBounds();
        container.remove(prev);
        container.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
      }
    }
  }

  private void clearDragImage() {
    removeDragImageComponent(myDragContentImage);
    removeDragImageComponent(myDragActionImage);
    myDragContentImage = null;
    myDragActionImage = null;
  }

  public void dragStopped(DragContext context) {
    assert myCurrentDrag != null : context;
    assert myCurrentDrag == context : myCurrentDrag + " " + context;
    notifyTargets(new DndEvent(false, context, null));
    myCurrentDrag = null;
    updateDragImage(null);
  }

  @NotNull
  public static DndManager instance() {
    return Context.require(DndManager.class);
  }

  @Nullable
  public static DndManager instanceOrNull() {
    return Context.get(DndManager.class);
  }

  @Nullable
  public static DragContext currentDrag() {
    DndManager manager = instanceOrNull();
    return manager == null ? null : manager.myCurrentDrag;
  }

  public boolean isDndActive() {
    return myCurrentDrag != null;
  }

  @Override
  public boolean postProcess(AWTEvent event, boolean alreadyConsumed) {
    return false;
  }

  public boolean preprocess(AWTEvent event, boolean alreadyConsumed) {
    if(event.getID() == MouseEvent.MOUSE_DRAGGED && event instanceof MouseEvent) {
      final MouseEvent me = (MouseEvent)event;
      final Component component = me.getComponent();
      if(component != null) {
        myLastEvent = new SoftReference<MouseEvent>(new MouseEvent(
          component, me.getID(), me.getWhen(), me.getModifiers(), me.getX(), me.getY(),
          me.getClickCount(), me.isPopupTrigger(), me.getButton()));
        if(myCurrentDrag != null) {
          updateDrag(me);
        }
      }
    }
    return false;
  }

  public void start() {
    EventQueueReplacement.ensureInstalled().addPreprocessor(this);
  }

  public static DndManager require() {
    DndManager dndManager = Context.get(DndManager.class);
    if (dndManager == null) {
      dndManager = new DndManager();
      dndManager.start();
      Context.replace(new InstanceProvider<DndManager>(dndManager, null), "DndManager");
      try {
        Context.globalize();
      } finally {
        Context.pop();
      }
    }
    return dndManager;
  }

  @Nullable
  public MouseEvent getLastEvent() {
    SoftReference<MouseEvent> ref = myLastEvent;
    return ref == null ? null : ref.get();
  }
}
