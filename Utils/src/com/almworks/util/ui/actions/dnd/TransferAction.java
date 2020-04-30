package com.almworks.util.ui.actions.dnd;

import com.almworks.util.L;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Dyoma
 */
public abstract class TransferAction extends AnAbstractAction {
  private final String mySwingActionName;

  protected TransferAction(String text, Action action) {
    super(text);
    mySwingActionName = (String) action.getValue(Action.NAME);
//    Object acc = action.getValue(Action.ACCELERATOR_KEY);
//    if (acc instanceof KeyStroke)
//      setDefaultPresentation(PresentationKey.SHORTCUT, (KeyStroke) acc);
  }

  public void perform(ActionContext context) throws CantPerformException {
    ComponentContext<JComponent> cc = context.getComponentContext(JComponent.class, ContextTransfer.CONTEXT_TRANSFER);
    getSwingAction(cc).actionPerformed(cc.createActionEvent());
  }

  private Action getSwingAction(ComponentContext<JComponent> context) {
    return context.getComponent().getActionMap().get(mySwingActionName);
  }

  public void update(UpdateContext context) throws CantPerformException {
    super.update(context);
    ComponentContext<JComponent> cc = context.getComponentContext(JComponent.class, ContextTransfer.CONTEXT_TRANSFER);
    ContextTransfer handler = cc.getSourceObject(ContextTransfer.CONTEXT_TRANSFER);
    context.setEnabled(EnableState.INVISIBLE);
    if (getSwingAction(cc) != null && handler != null) {
      DragContext dragContext = DndUtil.getClipboardDragContext(cc.getComponent());
      if (dragContext != null) {
        boolean canPerform = canPerform(handler, dragContext);
        context.setEnabled(canPerform);
      }
    }
  }

  protected abstract boolean canPerform(ContextTransfer handler, @NotNull DragContext context)
    throws CantPerformException;

  private static ContextTransfer getHandler(ActionContext context) throws CantPerformException {
    ComponentContext<JComponent> cc = context.getComponentContext(JComponent.class, ContextTransfer.CONTEXT_TRANSFER);
    return cc.getSourceObject(ContextTransfer.CONTEXT_TRANSFER);
  }

  public static final AnAction COPY = new TransferAction(L.actionName("&Copy"), TransferHandler.getCopyAction()) {
    protected boolean canPerform(ContextTransfer handler, DragContext context) throws CantPerformException {
      return handler.canCopy(context);
    }
  };

  public static final AnAction CUT = new TransferAction(L.actionName("C&ut"), TransferHandler.getCutAction()) {
    protected boolean canPerform(ContextTransfer handler, DragContext context) throws CantPerformException {
      return handler.canCopy(context) && handler.canRemove(context);
    }
  };

  public static final AnAction PASTE = new TransferAction(L.actionName("&Paste"), TransferHandler.getPasteAction()) {
    protected boolean canPerform(ContextTransfer handler, DragContext context) throws CantPerformException {
      return handler.canImportDataNow(context, context.getComponent());
    }
  };

  public static final AnAction REMOVE =
    new AnAbstractAction(L.actionName("Remove"), Icons.ACTION_GENERIC_CANCEL_OR_REMOVE) {
      public void perform(ActionContext context) throws CantPerformException {
        ContextTransfer handler = getHandler(context);
        if (handler != null && handler.canRemove(context))
          handler.remove(context);
      }

      public void update(UpdateContext context) throws CantPerformException {
        super.update(context);
        context.watchRole(ATreeNode.ATREE_NODE);
        ContextTransfer handler = getHandler(context);
        if (handler == null)
          context.setEnabled(EnableState.INVISIBLE);
        else {
          context.setEnabled(handler.canRemove(context) ? EnableState.ENABLED : EnableState.DISABLED);
        }
      }
    };

  public static final List<AnAction> CUT_COPY_PASTE = Arrays.asList(new AnAction[] {CUT, COPY, PASTE});
}
