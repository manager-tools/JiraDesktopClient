package com.almworks.util.ui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

public class TransferableImage implements Transferable, ClipboardOwner {
  private static final DataFlavor FLAVOR = DataFlavor.imageFlavor;

  private Image myImage;
  private final DataFlavor[] myFlavors;

  public TransferableImage(Image image) {
    assert image != null;
    myImage = image;
    myFlavors = new DataFlavor[] {FLAVOR};
  }

  public DataFlavor[] getTransferDataFlavors() {
    return myFlavors;
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return flavor == FLAVOR;
  }

  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (flavor != FLAVOR)
      throw new UnsupportedFlavorException(flavor);
    return myImage;
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    if (contents == this)
      myImage = null;
  }
}
