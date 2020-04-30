package com.almworks.util.ui.actions.dnd;

import com.almworks.util.collections.Convertor;
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
import java.util.List;

public class ConvertingToStringSourceTransfer<T> implements ContextTransfer {
  private final DataRole<T> myRole;
  private final Convertor<T, String> myConvertor;

  public ConvertingToStringSourceTransfer(DataRole<T> role, Convertor<T, String> convertor) {
    myRole = role;
    myConvertor = convertor;
  }

  public static <T> ConvertingToStringSourceTransfer<T> create(DataRole<T> role, Convertor<T, String> convertor) {
    return new ConvertingToStringSourceTransfer<T>(role, convertor);
  }

  @NotNull
  public Transferable transfer(DragContext context) throws CantPerformException {
    List<T> data = context.getSourceCollection(myRole);
    if (data.size() == 0)
      throw new CantPerformException();
    List<String> strings = myConvertor.collectList(data);
    return new StringListTransferable(strings);
  }

  public void acceptTransfer(DragContext context, Transferable tranferred)
    throws CantPerformException, UnsupportedFlavorException, IOException
  {
    throw new CantPerformException();
  }

  public void cleanup(DragContext context) throws CantPerformException {
  }

  public void remove(ActionContext context) throws CantPerformException {
    throw new CantPerformException();
  }

  public boolean canRemove(ActionContext context) throws CantPerformException {
    return false;
  }

  public boolean canMove(ActionContext context) throws CantPerformException {
    return canRemove(context);
  }

  public boolean canCopy(ActionContext context) throws CantPerformException {
    List<T> data = context.getSourceCollection(myRole);
    return data.size() > 0;
  }

  public boolean canLink(ActionContext context) throws CantPerformException {
    return false;
  }

  public boolean canImportData(DragContext context) throws CantPerformException {
    return false;
  }

  public boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException {
    return false;
  }

  public void startDrag(DragContext dragContext, InputEvent event) throws CantPerformException {
  }

  public boolean canImportFlavor(DataFlavor flavor) {
    return false;
  }

  @Nullable
  public Factory<Image> getTransferImageFactory(DragContext dragContext) throws CantPerformException {
    return null;
  }
}
