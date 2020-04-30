package com.almworks.util.ui.actions.dnd;

import org.almworks.util.Collections15;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

public class StringListTransferable implements Transferable {
  private static final DataFlavor ourStringListFlavor = DndUtil.LIST_OF_STRINGS.getFlavor();
  private static final DataFlavor[] ourFlavors = {
    ourStringListFlavor, DataFlavor.stringFlavor, DataFlavor.plainTextFlavor
  };

  private final List<String> myStrings;
  private String mySingleString;

  public StringListTransferable(String[] strings) {
    myStrings = Collections15.unmodifiableArrayList(strings);
  }

  public StringListTransferable(Collection<String> strings) {
    myStrings = Collections15.unmodifiableListCopy(strings);
  }

  public DataFlavor[] getTransferDataFlavors() {
    return ourFlavors.clone();
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    for (int i = 0; i < ourFlavors.length; i++) {
      if (flavor.equals(ourFlavors[i])) {
        return true;
      }
    }
    return false;
  }

  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (flavor.equals(ourStringListFlavor)) {
      return myStrings;
    } else if (flavor.equals(DataFlavor.stringFlavor)) {
      return getSingleString();
    } else if (flavor.equals(DataFlavor.plainTextFlavor)) {
      return new StringReader(getSingleString());
    } else {
      throw new UnsupportedFlavorException(flavor);
    }
  }

  private String getSingleString() {
    int count = myStrings.size();
    if (count == 0)
      return "";
    else if (count == 1)
      return myStrings.get(0);
    synchronized(this) {
      if (mySingleString != null)
        return mySingleString;
      StringBuffer buf = new StringBuffer();
      for (String string : myStrings) {
        buf.append(string);
        buf.append('\n');
      }
      mySingleString = buf.toString();
      return mySingleString;
    }
  }
}
