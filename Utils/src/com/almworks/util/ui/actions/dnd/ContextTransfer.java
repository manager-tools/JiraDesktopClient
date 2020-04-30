package com.almworks.util.ui.actions.dnd;

import com.almworks.util.commons.Factory;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.io.IOException;

/**
 * @author : Dyoma
 */
public interface ContextTransfer {
  DataRole<ContextTransfer> CONTEXT_TRANSFER = DataRole.createRole(ContextTransfer.class);

  @NotNull
  Transferable transfer(DragContext context) throws CantPerformException;

  void acceptTransfer(DragContext context, Transferable tranferred) throws CantPerformException,
    UnsupportedFlavorException, IOException;

  void cleanup(DragContext context) throws CantPerformException;

  void remove(ActionContext context) throws CantPerformException;

  boolean canRemove(ActionContext context) throws CantPerformException;

  boolean canMove(ActionContext context) throws CantPerformException;

  boolean canCopy(ActionContext context) throws CantPerformException;

  boolean canLink(ActionContext context) throws CantPerformException;

  boolean canImportData(DragContext context) throws CantPerformException;

  boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException;

  void startDrag(DragContext dragContext, InputEvent event) throws CantPerformException;

  boolean canImportFlavor(DataFlavor flavor);

  @Nullable
  Factory<Image> getTransferImageFactory(DragContext dragContext) throws CantPerformException;
}
