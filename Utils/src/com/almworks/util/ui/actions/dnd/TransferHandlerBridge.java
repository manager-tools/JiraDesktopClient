package com.almworks.util.ui.actions.dnd;

import com.almworks.util.Env;
import com.almworks.util.commons.Factory;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.TooManyListenersException;

/**
 * @author : Dyoma
 */
public class TransferHandlerBridge extends TransferHandler implements DropTargetListener {
  private final ContextTransfer myTransfer;
  private DragContext myDragContext;
  private DropContext myDropContext;

  private TransferHandlerBridge(ContextTransfer transfer) {
    myTransfer = transfer;
  }

  protected Transferable createTransferable(JComponent c) {
    DragContext dragContext = createDragContext(c);
    Transferable transferObject;
    try {
      transferObject = myTransfer.transfer(dragContext);
    } catch (CantPerformException e) {
      Log.error(e);
      return null;
    } catch (Exception e) {
      Log.error(e);
      return null;
    } catch (AssertionError e) {
      Log.error(e);
      return null;
    }
    assert transferObject != null;
    dragContext.putValue(DndUtil.TRANSFERABLE, transferObject);
    try {
      Factory<Image> image = myTransfer.getTransferImageFactory(dragContext);
      if (image != null) {
        dragContext.putValue(DndUtil.CONTENT_IMAGE_FACTORY, image);
      }
    } catch (CantPerformException e) {
      // ignore
    }
    return transferObject;
  }


  public ContextTransfer getTransfer() {
    return myTransfer;
  }

  private DragContext createDragContext(JComponent c) {
    if (myDragContext == null)
      myDragContext = new DragContext(c);
    return myDragContext;
  }

  public void exportAsDrag(JComponent comp, InputEvent event, int action) {
    assert myDragContext == null;
    super.exportAsDrag(comp, event, action);
    if (myDragContext == null) {
      Log.warn("cannot start drag");
      return;
    }
    DragContext dragContext = myDragContext;
    boolean success = false;
    try {
      dragContext.setAction(action);
      myTransfer.startDrag(dragContext, event);
      DndManager dndManager = DndManager.require();
      if (dndManager != null) {
        dragContext.putValue(DndUtil.DND_MANAGER, dndManager);
        dndManager.dragStarted(event, dragContext);
      }
      success = true;
    } catch (CantPerformException e) {
      Log.error(e);
    } finally {
      if (!success) {
        myDragContext = null;
      }
    }
  }

  public int getSourceActions(JComponent c) {
    ActionContext context = new DefaultActionContext(c);
    int r = 0;
    try {
      if (myTransfer.canCopy(context)) {
        r |= COPY;
      }
    } catch (CantPerformException e) {
      // ignore
    }
    try {
      if (myTransfer.canMove(context)) {
        r |= MOVE;
      }
    } catch (CantPerformException e) {
      // ignore
    }
    try {
      if (myTransfer.canLink(context)) {
        // Experiments revealed that adding ACTION_LINK to
        // ACTION_COPY here breaks issue DnD on Mac OS X 
        // (see JCO-62). So, here's this kludge.
        if(!Env.isMac() || r == 0) {
          r |= DnDConstants.ACTION_LINK;
        }
      }
    } catch (CantPerformException e) {
      // ignore
    }
    return r;
  }

  public boolean importData(JComponent c, Transferable t) {
    if (canImport(c, t.getTransferDataFlavors())) {
      DragContext context = getAnyDragContext(c);
      try {
        boolean can = myTransfer.canImportDataNow(context, c);
        if (can) {
          myTransfer.acceptTransfer(context, t);
        }
        return can;
      } catch (UnsupportedFlavorException ufe) {
        Log.warn(ufe);
      } catch (IOException ioe) {
        Log.warn(ioe);
      } catch (CantPerformExceptionExplained e) {
        e.explain(myDragContext != null ? "Drop" : "Paste", context);
      } catch (CantPerformException e) {
        Log.warn(e);
      }
    }
    return false;
  }

  protected void exportDone(JComponent c, Transferable data, int action) {
    DragContext context = myDragContext;
    if (context == null)
      return;
    try {
      context.setAction(action);
      myTransfer.cleanup(context);
      DndManager dndManager = context.getValue(DndUtil.DND_MANAGER);
      if (dndManager != null) {
        dndManager.dragStopped(context);
      }
    } catch (CantPerformException e) {
      Log.error(e);
    } finally {
      myDragContext = null;
    }
  }

  public boolean canImport(JComponent c, DataFlavor[] flavors) {
    for (DataFlavor flavor : flavors) {
      boolean can = myTransfer.canImportFlavor(flavor);
      if (can)
        return true;
    }
    return false;
  }

  @NotNull
  private DragContext getAnyDragContext(JComponent c) {
    assert myDragContext == null || myDragContext.getComponent() == c;
    DragContext context = getInnerDragContext(c);
    if (context == null) {
      context = DndUtil.getClipboardDragContext(c);
    }
    if (context == null) {
      assert false;
      context = new DragContext(c);
      context.putValue(DndUtil.FROM_CLIPBOARD, true);
    }
    return context;
  }

  @Nullable
  private DragContext getInnerDragContext(@Nullable JComponent c) {
    if (myDragContext != null) {
      return myDragContext;
    }
    DragContext globalDrag = DndManager.currentDrag();
    if (globalDrag == null) {
      myDropContext = null;
      return null;
    }
    if (myDropContext != null) {
      if (myDropContext.getDrag() != globalDrag || (c != null && myDropContext.getComponent() != c)) {
        myDropContext = null;
      }
    }
    if (myDropContext == null) {
      if (c == null)
        c = (JComponent) globalDrag.getComponent();
      myDropContext = new DropContext(globalDrag, c);
    }
    return myDropContext;
  }

  public static void install(JComponent component, ContextTransfer transfer) {
    TransferHandlerBridge bridge = new TransferHandlerBridge(transfer);
    component.setTransferHandler(bridge);
    if (!GraphicsEnvironment.isHeadless()) {
      DropTarget dropTarget = component.getDropTarget();
      if (dropTarget != null) {
        try {
          dropTarget.addDropTargetListener(bridge);
        } catch (TooManyListenersException e) {
          assert false : "not a swing drop target on " + component;
        }
      } else {
        assert false : component;
      }
    }
    ConstProvider.addRoleValue(component, ContextTransfer.CONTEXT_TRANSFER, transfer);
  }

  public void dragEnter(DropTargetDragEvent event) {
    DragContext c = DndManager.currentDrag();
    if (c == null) {
      DndManager dnd = DndManager.instance();
      MouseEvent lastEvent = dnd.getLastEvent();
      if (lastEvent != null) {
        Transferable t;
        try {
          t = event.getTransferable();
        } catch (Exception e) {
          t = null;
        }
        Component component = lastEvent.getComponent();
        if (t != null && component != null && component.isShowing()) {
          JComponent contextComponent;
          Window window =
            component instanceof Window ? (Window) component : SwingUtilities.getWindowAncestor(component);
          if (window instanceof JFrame)
            contextComponent = ((JFrame) window).getRootPane();
          else if (window instanceof JDialog)
            contextComponent = ((JDialog) window).getRootPane();
          else
            contextComponent = null;
          c = new DragContext(contextComponent);
          c.putValue(DndUtil.TRANSFERABLE, t);
          c.putValue(DndUtil.EXTERNAL, true);
          dnd.dragStarted(lastEvent, c);
        }
      }
    }
  }

  public void dragOver(DropTargetDragEvent e) {
    DragContext context = getInnerDragContext(null);
    if (context != null) {
      Boolean b = context.getValue(DndUtil.DROP_ENABLED);
      DndAction action = context.getAction();
      if (b != null && action != null) {
        if (b) {
          e.acceptDrag(action.getAwtCode());
        } else {
          e.rejectDrag();
        }
      }
    }
  }

  public void dropActionChanged(DropTargetDragEvent e) {
    DragContext dragContext = getInnerDragContext(null);
    if (dragContext instanceof DropContext) {
      dragContext = ((DropContext) dragContext).getDrag();
    }
    if (dragContext != null)
      dragContext.setAction(e.getDropAction());
  }

  public void dragExit(DropTargetEvent dte) {
    DragContext c = DndManager.currentDrag();
    if (c != null && c.getValue(DndUtil.EXTERNAL) != null) {
      DndManager.instance().dragStopped(c);
    }
  }

  public void drop(DropTargetDropEvent dtde) {
    DragContext c = DndManager.currentDrag();
    if (c != null && c.getValue(DndUtil.EXTERNAL) != null) {
      DndManager.instance().dragStopped(c);
    }
  }
}
