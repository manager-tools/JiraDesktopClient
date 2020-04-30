package com.almworks.util.ui.actions.dnd;

import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class DndHelper<H extends DropHint, C extends JComponent & DndComponentAdapter<H>> {
  private final DropHintProvider<H, C> myHintProvider;

  private Point myLastNotifyPoint;
  private DndAction myLastNotifyAction;
  private DragContext myLastDragContext;
  private boolean myDnDActivated = false;

  public DndHelper(DropHintProvider<H, C> hintProvider) {
    myHintProvider = hintProvider != null ? hintProvider : EmptyHintProvider.INSTANCE;
  }

  public void dragNotify(DndEvent event, ContextTransfer transfer, C component) {
    if (event.isDndActive() && transfer != null) {
      if (component.isShowing()) {
        if (!myDnDActivated) {
          assert myLastDragContext == null;
          myDnDActivated = true;
          myLastDragContext = event.getContext();
          boolean accept = DndUtil.acceptsDrag(event.getContext(), transfer);
          component.setDndActive(true, accept);
          myLastNotifyPoint = null;
          myLastNotifyAction = null;
        }
        boolean hinted = false;
        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent != null && inputEvent.getSource() instanceof Component && SwingTreeUtil.isAncestor(
          ((Component) inputEvent.getSource()), component)) {
          Point p = event.getMousePointFor(component);
          Rectangle r = component.getVisibleRect();
          if (p != null && r.contains(p)) {
            if (myLastDragContext == null)
              myLastDragContext = event.getContext();
            if (component.isDndWorking()) {
              DndAction action = myLastDragContext.getAction();
              if (Util.equals(p, myLastNotifyPoint) && Util.equals(action, myLastNotifyAction)) {
                hinted = true;
              } else {
                myLastNotifyPoint = p;
                myLastNotifyAction = action;
                boolean updateNeeded = myHintProvider.prepareDropHint(component, p, myLastDragContext, transfer);
                if (!updateNeeded) {
                  hinted = true;
                } else {
                  H hint = myHintProvider.createDropHint(component, myLastDragContext);
                  if (hint != null) {
                    component.setDropHint(hint);
                    hinted = true;
                  }
                }
              }
            }
          }
        }
        if (!hinted) {
          if (myLastDragContext != null) {
            myHintProvider.cleanContext(myLastDragContext);
            component.setDropHint(null);
            myLastDragContext = null;
          }
        }
      }
    } else {
      myDnDActivated = false;
      myHintProvider.cleanContext(myLastDragContext);
      myLastDragContext = null;
      myLastNotifyAction = null;
      myLastNotifyPoint = null;
      component.setDndActive(false, false);
    }
  }


  private static final class EmptyHintProvider implements DropHintProvider {
    public static final EmptyHintProvider INSTANCE = new EmptyHintProvider();

    public boolean prepareDropHint(JComponent component, Point p, DragContext context, ContextTransfer transfer) {
      return false;
    }

    public DropHint createDropHint(JComponent component, DragContext context) {
      return null;
    }

    public void cleanContext(DragContext context) {
    }
  }
}
