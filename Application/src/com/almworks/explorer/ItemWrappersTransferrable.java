package com.almworks.explorer;

import com.almworks.api.application.ItemWrapper;
import com.almworks.util.ui.actions.dnd.DndHack;
import com.almworks.util.ui.actions.dnd.TypedDataFlavor;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ItemWrappersTransferrable implements Transferable {
  public static final TypedDataFlavor<List<ItemWrapper>> ARTIFACTS_FLAVOR =
    new TypedDataFlavor(List.class, "application/x-almworks-artifact-list", "Artifact List");

  public static final TypedDataFlavor<List<String>> URI_LIST_FLAVOR =
    new TypedDataFlavor(DndHack.uriListFlavor, List.class);

  private static DataFlavor[] flavors = new DataFlavor[] {ARTIFACTS_FLAVOR.getFlavor(), DataFlavor.stringFlavor};
  private final List<ItemWrapper> myWrappers;

  public ItemWrappersTransferrable(Collection<ItemWrapper> wrappers) {
    myWrappers = Collections15.arrayList(wrappers);
  }

  public DataFlavor[] getTransferDataFlavors() {
    return flavors;
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return flavors[0].equals(flavor) || flavors[1].equals(flavor);
  }

  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (ARTIFACTS_FLAVOR.getFlavor().equals(flavor)) {
      return myWrappers;
    } else if (DataFlavor.stringFlavor.equals(flavor)) {
      if (myWrappers.size() == 1) return Util.NN(myWrappers.get(0).getItemUrl());
      StringBuffer buffer = new StringBuffer();
      for (ItemWrapper wrapper : myWrappers) {
        exportToString(buffer, wrapper);
      }
      return buffer.toString();
    } else {
      throw new UnsupportedFlavorException(flavor);
    }
  }

  private void exportToString(StringBuffer buffer, ItemWrapper wrapper) {
    String url = wrapper.getItemUrl();
    if (url != null) {
      buffer.append(url);
      buffer.append('\n');
    }
  }
}
