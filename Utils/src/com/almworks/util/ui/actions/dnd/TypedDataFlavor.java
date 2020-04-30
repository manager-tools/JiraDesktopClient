package com.almworks.util.ui.actions.dnd;

import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.IOException;

public class TypedDataFlavor<T> extends TypedKey<T> {
  private final DataFlavor myFlavor;

  public TypedDataFlavor(@NotNull DataFlavor flavor, Class<T> dataClass) {
    super(dataClass.getName(), dataClass, null);
    myFlavor = flavor;
  }

  public TypedDataFlavor(Class<T> nonObfuscatedDataClass, String mimeType, String displayName) {
    this(new CustomDataFlavor(mimeType, displayName, nonObfuscatedDataClass), nonObfuscatedDataClass);
  }

  @NotNull
  public T getData(Transferable transferable) throws CantPerformException {
    try {
      Object object = transferable.getTransferData(myFlavor);
      if (object == null)
        throw new CantPerformException("null " + transferable);
      Class<T> valueClass = getValueClass();
      if (valueClass == null) {
        assert false : this;
        throw new CantPerformException();
      }
      if (!valueClass.isInstance(object)) {
        throw new CantPerformException("bad class " + object.getClass() + " " + valueClass);
      }
      return (T)object;
    } catch (UnsupportedFlavorException e) {
      throw new CantPerformException(e);
    } catch (IOException e) {
      throw new CantPerformException(e);
    }
  }

  @Nullable
  public T getDataOrNull(Transferable transferable) {
    try {
      Object object = transferable.getTransferData(myFlavor);
      if (object != null) {
        Class<T> valueClass = getValueClass();
        if (valueClass == null) {
          assert false : this;
          return null;
        }
        if (valueClass.isInstance(object)) {
          return (T) object;
        }
      }
    } catch (UnsupportedFlavorException e) {
      // fall through
    } catch (IOException e) {
      // fall through
    } catch (InvalidDnDOperationException e) {
      // fall through
    }
    return null;
  }

  public DataFlavor getFlavor() {
    return myFlavor;
  }
}
