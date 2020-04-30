package com.almworks.util.ui.actions.dnd;

import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DefaultActionContext;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.IOException;

/**
 * @author dyoma
 */
public class DragContext extends DefaultActionContext {
  private final PropertyMap myProperties = new PropertyMap();

  public DragContext(JComponent component) {
    super(component);
  }

  public <T> T putValue(TypedKey<T> key, T value) {
    T old = myProperties.get(key);
    if (value == null) {
      myProperties.remove(key);
    } else {
      myProperties.put(key, value);
    }
    return old;
  }

  @Nullable
  public <T> T getValue(TypedKey<T> key) {
    return myProperties.get(key);
  }

  @Nullable
  public Transferable getTransferable() {
    return getValue(DndUtil.TRANSFERABLE);
  }

  @Nullable
  public <T> T getTransferData(TypedDataFlavor<T> flavor) {
    Transferable transferable = getTransferable();
    return transferable == null ? null : flavor.getDataOrNull(transferable);
  }

  @Nullable
  <T> T getTransferData(DataFlavor flavor, Class<T> dataClass) {
    Transferable transferable = getTransferable();
    if (transferable != null) {
      try {
        Object object = transferable.getTransferData(flavor);
        if (object != null && dataClass.isInstance(object)) {
          return (T) object;
        }
      } catch (UnsupportedFlavorException e) {
        // fall through
      } catch (IOException e) {
        // fall through
      }
    }
    return null;
  }

  @NotNull
  public <T> T getSourceObject(final TypedKey<? extends T> role) throws CantPerformException {
    if (role instanceof TypedDataFlavor) {
      Object object = getTransferData(((TypedDataFlavor) role));
      if (object != null) {
        return (T) object;
      } else {
        throw new CantPerformException();
      }
    } else {
      T value = getValue(role);
      return value != null ? value : super.getSourceObject(role);
    }
  }

  public boolean isKeyboardTransfer() {
    Boolean r = getValue(DndUtil.FROM_CLIPBOARD);
    return r != null && r;
  }

  public String toString() {
    return "DC[" + getTransferable() + "]";
  }

  public void setAction(int action) {
    DndAction value;
    if (action == DnDConstants.ACTION_COPY)
      value = DndAction.COPY;
    else if (action == DnDConstants.ACTION_MOVE)
      value = DndAction.MOVE;
    else if (action == DnDConstants.ACTION_LINK)
      value = DndAction.LINK;
    else
      value = null;
    putValue(DndUtil.ACTION, value);
  }

  @Nullable
  public DndAction getAction() {
    return getValue(DndUtil.ACTION);
  }
}
