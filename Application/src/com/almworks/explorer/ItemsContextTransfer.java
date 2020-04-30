package com.almworks.explorer;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.util.ContinueOrBreak;
import com.almworks.util.files.FileUtil;
import com.almworks.util.images.ImageUtil;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.dnd.DndHack;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.actions.dnd.DragContext;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dyoma
 */
public class ItemsContextTransfer extends BaseItemContextTransfer {
  public void acceptTransfer(DragContext context, Transferable transferred)
    throws CantPerformException, UnsupportedFlavorException, IOException
  {
    final List<ItemWrapper> targets = getTargets(context);
    checkTargets(targets);
    ItemWrapper target = targets.get(0);

    try {
      tryAcceptArtifacts(context, targets);
      tryAcceptFiles(context, target, transferred);
      tryAcceptImage(context, target, transferred);
      // more?
    } catch (ContinueOrBreak cob) {
      return;
    }
  }

  private void checkTargets(List<ItemWrapper> targets) throws CantPerformException {
    if (targets.isEmpty()) {
      throw new CantPerformException("no targets");
    }

    for (ItemWrapper iw : targets)
      if (!(iw instanceof LoadedItem)) {
        throw new CantPerformException("target class " + iw.getClass());
    }
  }

  private void tryAcceptArtifacts(DragContext context, List<ItemWrapper> targets)
    throws ContinueOrBreak, CantPerformException
  {
    final List<ItemWrapper> items = context.getTransferData(ItemWrappersTransferrable.ARTIFACTS_FLAVOR);
    if (items != null && items.size() > 0) {
      importArtifacts(context, targets, items);
      ContinueOrBreak.throwBreak();
    }
  }

  private void tryAcceptImage(DragContext context, ItemWrapper target, Transferable transferred)
    throws ContinueOrBreak, CantPerformException
  {
    final Image image = getImage(transferred);
    if (image != null) {
      acceptImage(context, target, image);
      ContinueOrBreak.throwBreak();
    }
  }

  private void tryAcceptFiles(DragContext context, ItemWrapper target, Transferable transferred)
    throws ContinueOrBreak, CantPerformException
  {
    final List<File> files = getFiles(transferred);
    if (files != null && files.size() > 0) {
      tryAcceptSingleImageFile(context, target, files);
      tryAcceptRegularFiles(context, target, files);
    }
  }

  private void tryAcceptSingleImageFile(DragContext context, ItemWrapper target, List<File> files)
    throws CantPerformException, ContinueOrBreak
  {
    if (files.size() == 1) {
      final Image image = getImageFromFile(files.get(0));
      if (image != null) {
        acceptImage(context, target, image);
        ContinueOrBreak.throwBreak();
      }
    }
  }

  private void tryAcceptRegularFiles(DragContext context, ItemWrapper target, List<File> files)
    throws CantPerformException, ContinueOrBreak
  {
    acceptFiles(context, target, files);
    ContinueOrBreak.throwBreak();
  }

  private Image getImageFromFile(File file) {
    String mime = FileUtil.guessMimeType(file.getName());
    if (!ImageUtil.isImageMimeType(mime) && !ImageUtil.isBitmap(file, mime)) {
      return null;
    }

    // kludge: loading in AWT thread
    Image image = ImageUtil.loadImageFromFile(file, mime);
    if (image == null) {
      Log.warn("file " + file + " cannot be read as image");
      return null;
    }

    // ensure image is loaded
    ImageIcon icon = new ImageIcon(image);
    icon.getImage();

    int loadStatus = icon.getImageLoadStatus();
    if (loadStatus == MediaTracker.ABORTED || loadStatus == MediaTracker.ERRORED) {
      Log.warn("cannot load " + file + " as image");
      return null;
    }

    return image;
  }

  private static void acceptFiles(DragContext context, ItemWrapper target, List<File> files)
    throws CantPerformException
  {
    target.getMetaInfo().acceptFiles(context, target, files);
  }

  private static void acceptImage(DragContext context, ItemWrapper target, Image image)
    throws CantPerformException
  {
    target.getMetaInfo().acceptImage(context, target, image);
  }

  private Image getImage(Transferable transferred) {
    if (DndUtil.isDataFlavorSupported(transferred, DataFlavor.imageFlavor)) {
      Object data = null;
      try {
        data = transferred.getTransferData(DataFlavor.imageFlavor);
      } catch (Exception e) {
        // skip
      }
      if (data instanceof Image)
        return (Image) data;
    }
    return null;
  }

  private List<File> getFiles(Transferable transferred) throws CantPerformException {
    List<File> r = null;
    Object data;
    if (DndUtil.isDataFlavorSupported(transferred, DataFlavor.javaFileListFlavor)) {
      try {
        data = transferred.getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        throw new CantPerformException(e);
      }
      if (data instanceof List) {
        List list = (List) data;
        r = Collections15.arrayList();
        for (Object o : list) {
          if (o instanceof File) {
            File file = (File) o;
            if (file.isFile() && file.canRead()) {
              r.add(file);
            }
          }
        }
      }
    } else if (DndUtil.isDataFlavorSupported(transferred, DndHack.uriListFlavor)) {
      try {
        data = transferred.getTransferData(DndHack.uriListFlavor);
      } catch (Exception e) {
        throw new CantPerformException(e);
      }
      if (data instanceof String) {
        String uriList = (String) data;
        String[] list = uriList.split("[\\r\\n]+");
        r = Collections15.arrayList();
        for (String uri : list) {
          try {
            File file = new File(new URI(uri));
            if (file.isFile() && file.canRead()) {
              r.add(file);
            }
          } catch (URISyntaxException e) {
            Log.debug("cannot understand " + uri, e);
          } catch (IllegalArgumentException e) {
            Log.debug("not file uri" + uri, e);
          }
        }
      }
    }
    return r != null && r.size() > 0 ? r : null;
  }

  private void importArtifacts(DragContext context, List<ItemWrapper> targets, List<ItemWrapper> items)
    throws CantPerformException
  {
    targets.get(0).getMetaInfo().importItems(targets, items, context);
  }

  private List<File> extractFileList(String data) {
    String delim = TextUtil.LINE_SEPARATOR;
    String[] strings = data.split(delim);

    ArrayList<File> files = new ArrayList<File>(1);
    for (String string : strings) {
      if (string.startsWith("file://")) {
        string = string.substring(7);
      }
      files.add(new File(string));
    }

    return files;
  }

  public boolean canImportFlavor(DataFlavor flavor) {
    boolean r = super.canImportFlavor(flavor) || isAttachableFlavor(flavor);
    return r;
  }

  private boolean isAttachableFlavor(DataFlavor flavor) {
    return DataFlavor.javaFileListFlavor.equals(flavor) || DndHack.uriListFlavor.equals(flavor) ||
      DataFlavor.imageFlavor.equals(flavor);
  }

  protected boolean canImportDataNowToTarget(DragContext context, ItemWrapper target) throws CantPerformException {
    if (super.canImportDataNowToTarget(context, target))
      return true;
    return hasFileListTransferable(context);
  }

  private static boolean hasFileListTransferable(DragContext context) {
    Transferable transferable = context.getTransferable();
    return DndUtil.isDataFlavorSupported(transferable, DndHack.uriListFlavor) ||
      DndUtil.isDataFlavorSupported(transferable, DataFlavor.javaFileListFlavor) ||
      DndUtil.isDataFlavorSupported(transferable, DataFlavor.imageFlavor);
  }

  public boolean canImportData(DragContext context) throws CantPerformException {
    return super.canImportData(context) || hasFileListTransferable(context);
  }
}
