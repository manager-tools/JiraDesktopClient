package com.almworks.util.ui.actions.dnd;

import com.almworks.util.LogHelper;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.ListDropPoint;
import com.almworks.util.components.TableDropPoint;
import com.almworks.util.components.TreeDropPoint;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class DndUtil {
  public static final TypedKey<Transferable> TRANSFERABLE = TypedKey.create("TRANSFERABLE");
  public static final TypedKey<DndManager> DND_MANAGER = TypedKey.create("DND_MANAGER");
  public static final TypedKey<Factory<Image>> CONTENT_IMAGE_FACTORY = TypedKey.create("CONTENT_IMAGE");
  public static final TypedKey<Factory<Image>> ACTION_IMAGE_FACTORY = TypedKey.create("ACTION_IMAGE");
  public static final TypedKey<Boolean> FROM_CLIPBOARD = TypedKey.create("FROM_CLIPBOARD");
  public static final TypedKey<Boolean> EXTERNAL = TypedKey.create("EXTERNAL");
  public static final TypedKey<Point> DRAG_SOURCE_OFFSET = TypedKey.create("DRAG_SOURCE_OFFSET");
  public static final TypedKey<Dimension> DRAG_SOURCE_SIZE = TypedKey.create("DRAG_SOURCE_SIZE");

  public static final TypedKey<TreeDropPoint> TREE_DROP_POINT = TypedKey.create("TREE_DROP_POINT");
  public static final TypedKey<TableDropPoint> TABLE_DROP_POINT = TypedKey.create("TABLE_DROP_POINT");
  public static final TypedKey<ListDropPoint> LIST_DROP_POINT = TypedKey.create("LIST_DROP_POINT");
  public static final TypedKey<Integer> DROP_HINT_ROW = TypedKey.create("DROP_HINT_ROW");
  public static final TypedKey<DndAction> ACTION = TypedKey.create("ACTION");
  public static final TypedKey<Boolean> DROP_ENABLED = TypedKey.create("DROP_ENABLED");

  public static final int DND_SUBNODE_POINT_OFFSET = 14;

  public static final TypedDataFlavor<List<String>> LIST_OF_STRINGS =
    new TypedDataFlavor(List.class, "application/x-list-of-strings", "list of strings");


  public static final float DRAG_IMAGE_OPACITY = 0.7F;
  public static final float DRAG_IMAGE_OPACITY_MIN = 0.05F;

  // used to get graphics to calculate the size of real drag image
  static final BufferedImage DRAG_IMAGE_KICKOFF = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

  @Nullable
  public static DragContext getClipboardDragContext(JComponent c) {
    if (!GraphicsEnvironment.isHeadless()) {
      Transferable contents = getClipboardContentsSafe(false);
      if (contents != null) {
        DragContext context = new DragContext(c);
        context.putValue(TRANSFERABLE, contents);
        context.putValue(FROM_CLIPBOARD, true);
        return context;
      }
    }
    return null;
  }

  public static boolean acceptsDrag(DragContext context, ContextTransfer transfer) {
    if (transfer == null)
      return false;
    Transferable transferable = context.getTransferable();
    if (transferable == null)
      return false;
    try {
      if (!transfer.canImportData(context))
        return false;
    } catch (CantPerformException e) {
      return false;
    }
    DataFlavor[] flavors = transferable.getTransferDataFlavors();
    for (DataFlavor flavor : flavors) {
      boolean can = transfer.canImportFlavor(flavor);
      if (can)
        return true;
    }
    return false;
  }

  @Nullable
  public static Transferable getClipboardContentsSafe(boolean logError) {
    Clipboard clipboard = getClipboardSafe(logError);
    if (clipboard != null) {
      try {
        return clipboard.getContents(null);
      } catch (Exception e) {
        if (logError) {
          Log.debug("cannot get clipboard contents", e);
        }
      }
    }
    return null;
  }

  public static String getClipboardTextSafe(boolean logError) {
    Transferable transferable = DndUtil.getClipboardContentsSafe(logError);
    String data;
    try {
      return transferable == null ? null : (String) transferable.getTransferData(DataFlavor.stringFlavor);
    } catch (UnsupportedFlavorException e) {
      return null;
    } catch (IOException e) {
      if (logError) LogHelper.debug("cannot get clipboard text", e);
      return null;
    }
  }

  @Nullable
  public static Clipboard getClipboardSafe(boolean logError) {
    try {
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      return toolkit.getSystemClipboard();
    } catch (Exception e) {
      if (logError) {
        Log.debug("cannot get system clipboard", e);
      }
    }
    return null;
  }

  public static void copyToClipboard(Transferable contents) {
    Clipboard clipboard = getClipboardSafe(true);
    if (clipboard == null)
      return;
    ClipboardOwner owner;
    if (contents instanceof ClipboardOwner) {
      owner = (ClipboardOwner) contents;
    } else {
      owner = new StringSelection("");
    }
    try {
      clipboard.setContents(contents, owner);
    } catch (Exception e) {
      Log.debug("cannot copy to clipboard", e);
    }
    try {
      Toolkit.getDefaultToolkit().beep();
    } catch (Exception e) {
    }
  }

  // todo optimize drawing algorithm - otherwize not smooth "jump" over split pane
  public static void paintDropBorder(Graphics2D g2, Component c) {
    int thick = 1;
    Dimension size = c.getSize();

    if (size.width > thick * 3 && size.height > thick * 3) {
      g2.setColor(GlobalColors.DRAG_AND_DROP_COLOR);
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, DRAG_IMAGE_OPACITY));
      g2.setStroke(new BasicStroke(thick));

      g2.drawRect(thick / 2, thick / 2, size.width - thick, size.height - thick);
    }
  }


  // workaround for NPE or other exceptions generated by SunDropTargetContextPeer
  public static boolean isDataFlavorSupported(Transferable t, DataFlavor flavor) {
    try {
      return t.isDataFlavorSupported(flavor);
    } catch (Exception e) {
      Log.warn(e);
      return false;
    }
  }
}
