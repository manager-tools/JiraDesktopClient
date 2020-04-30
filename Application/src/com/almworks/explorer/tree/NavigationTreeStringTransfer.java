package com.almworks.explorer.tree;

import com.almworks.api.application.*;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.explorer.ItemUrlServiceImpl;
import com.almworks.explorer.ItemWrappersTransferrable;
import com.almworks.items.api.Database;
import com.almworks.util.Terms;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.components.ATree;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.TreeDropPoint;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.TreeStringTransfer;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

public class NavigationTreeStringTransfer extends TreeStringTransfer {
  private static final TypedKey<MultiMap<Connection, String>> CACHED_DROP_URLS = TypedKey.create("CDU");
  private final ATree<ATreeNode<GenericNode>> myTree;


  public NavigationTreeStringTransfer(Database db, ATree<ATreeNode<GenericNode>> tree) {
    super(new NavigationTransferService(db));
    myTree = tree;
  }

  public boolean canImportData(DragContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = getWrappers(context);
    if (wrappers != null && !wrappers.isEmpty())
      return true;
    MultiMap<?, ?> dropUrls = getDropUrls(context, false);
    if (dropUrls != null && !dropUrls.isEmpty())
      return true;
    return super.canImportData(context);
  }

  @Override
  public boolean canMove(ActionContext context) throws CantPerformException {
    return super.canMove(context) && canCopy(context);
  }

  public boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException {
    List<ItemWrapper> wrappers = getWrappers(context);
    boolean haveWrappers = wrappers != null && !wrappers.isEmpty();
    MultiMap<?, ?> dropUrls = haveWrappers ? null : getDropUrls(context, false);
    boolean haveUrls = dropUrls != null && !dropUrls.isEmpty();
    if (haveWrappers || haveUrls) {
      TreeDropPoint dropPoint = ATree.getDropPoint(context);
      if (dropPoint == null || dropPoint.isInsertNode())
        return false;
      ATreeNode targetNode = (ATreeNode) dropPoint.getNode();
      GenericNode userObject = Util.castNullable(GenericNode.class, targetNode.getUserObject());
      if (userObject == null) return false;
      if (haveWrappers)
        return QueryDropAcceptor.canAcceptItems(wrappers, userObject, context);
      if (haveUrls)
        return true; // todo check somehow?
    }
    return super.canImportDataNow(context, dropTarget);
  }

  private MultiMap<Connection, String> getDropUrls(DragContext context, boolean logClipboard) throws CantPerformException {
    MultiMap<Connection, String> r = context.getValue(CACHED_DROP_URLS);
    if (r != null)
      return r;
    r = extractDropUrls(context, logClipboard);
    if (r == null)
      r = MultiMap.empty();
    context.putValue(CACHED_DROP_URLS, r);
    return r;
  }

  /**
   * @param logClipboard record to log clipboard content.
   *                     While this may be useful for debugging, this discloses clipboard content, which may contain sensitive data
   */
  private MultiMap<Connection, String> extractDropUrls(DragContext context, boolean logClipboard) throws CantPerformException {
    List<String> stringList = getStrings(context);
    if (stringList == null || stringList.isEmpty())
      return null;
    MultiMap<Connection, String> r = MultiMap.create();
    ConnectionManager cm = context.getSourceObject(Engine.ROLE).getConnectionManager();
    for (String s : stringList) {
      int from = -1;
      int length = s.length();
      for (int i = 0; i <= length; i++) {
        char c = i < length ? s.charAt(i) : 0;
        boolean space = c == 0 || Character.isWhitespace(c);
        if (from < 0 && !space) {
          from = i;
        } else if (from >= 0 && space) {
          String url = s.substring(from, i);
          from = -1;
          try {
            url = URLDecoder.decode(url, "UTF-8");
          } catch (Exception e) {
            // ignore
          }
          Connection connection = cm.getConnectionForUrl(url);
          if (connection == null) {
            if (logClipboard)
              Log.debug("no connection for url [" + url + "]");
            return null;
          }
          r.add(connection, url);
        }
      }
    }
    return r;
  }

  private List<ItemWrapper> getWrappers(DragContext context) {
    return ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getDataOrNull(context.getTransferable());
  }

  public boolean canImportFlavor(DataFlavor flavor) {
    return super.canImportFlavor(flavor) || ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getFlavor().equals(flavor);
  }

  public void acceptTransfer(DragContext context, Transferable transfer)
    throws CantPerformException, UnsupportedFlavorException, IOException
  {
    if (DndUtil.isDataFlavorSupported(transfer, ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getFlavor())) {
      acceptWrappers(context, transfer);
      return;
    }
    MultiMap<Connection, String> dropUrls = getDropUrls(context, true);
    if (dropUrls != null && !dropUrls.isEmpty()) {
      acceptDropUrls(context, dropUrls);
      return;
    }
    super.acceptTransfer(context, transfer);
  }

  private void acceptDropUrls(final DragContext context, MultiMap<Connection, String> dropUrls) throws CantPerformException {
    TreeDropPoint dropPoint = ATree.getDropPoint(context);
    if (dropPoint == null || dropPoint.isInsertNode())
      throw new CantPerformException();
    ATreeNode targetNode = (ATreeNode) dropPoint.getNode();
    final GenericNode node = CantPerformException.cast(GenericNode.class, targetNode.getUserObject());
    ItemSource source = createItemSourceForUrls(dropUrls);
    ExplorerComponent explorer = Context.require(ExplorerComponent.class);
    final ImportingDialog dialog = new ImportingDialog(SwingTreeUtil.findAncestorOfType(myTree, JFrame.class));
    TreePath path = UIUtil.getPathToRoot(targetNode);
    JTree jtree = myTree.getScrollable();
    Point p = new Point(0, 0);
    Rectangle bounds = jtree.getPathBounds(path);
    if (bounds != null) {
      p.x = bounds.x + bounds.width + 8;
      p.y = bounds.y;
    }
    SwingUtilities.convertPointToScreen(p, jtree);
    dialog.setLocation(p);

    final ItemsCollectionController[] loaderContainer = {null};
    ItemsCollectionController loader = explorer.createLoader(new ItemCollectorWidget() {
      public void loadingDone() {
        ItemsCollectionController loader = loaderContainer[0];
        if (loader == null) {
          assert false;
          Log.warn("loader null", new RuntimeException());
          return;
        }
        try {
          if (!node.isNode()) {
            Log.debug("node removed before items dropped");
            return;
          }
          List<? extends LoadedItem> list = loader.getListModelUpdater().getModel().toList();
          if (!list.isEmpty()) {
            // todo can we use parent context here (not dragging anymore)
            DragContext acceptContext = context;
            try {
              List<ItemWrapper> items = (List) list;
              boolean can = QueryDropAcceptor.canAcceptItems(items, node, acceptContext);
              if (can) {
                QueryDropAcceptor.acceptItems(items, node, acceptContext);
              }
            } catch (CantPerformException e) {
              Log.debug("cannot drop", e);
            }
          }
        } finally {
          if (!dialog.hasErrors()) {
            dialog.dispose();
          } else {
            dialog.setDone();
          }
          loader.dispose();
        }
      }

      public void showLoadingMessage(String message) {
        dialog.setMessage(message);
      }

      public void showProgress(float percent) {
        dialog.setProgress(percent);
      }

      public void showErrors(List<String> errors) {
        if (errors != null) {
          dialog.setErrors(errors);
        }
      }
    }, source);
    loaderContainer[0] = loader;
    dialog.show();
    loader.reload();
  }

  private static ItemSource createItemSourceForUrls(MultiMap<Connection, String> dropUrls) throws CantPerformExceptionExplained {
    return ItemUrlServiceImpl.getAggregateItemSource(dropUrls, dropUrls.size() + " dropped", Local.parse("Collecting " + Terms.ref_Artifacts));
  }

  private void acceptWrappers(DragContext context, Transferable transfer) throws CantPerformException {
    List<ItemWrapper> wrappers = ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getData(transfer);
    if (wrappers != null && wrappers.size() > 0) {
      TreeDropPoint dropPoint = ATree.getDropPoint(context);
      if (dropPoint == null || dropPoint.isInsertNode())
        throw new CantPerformException();
      ATreeNode targetNode = (ATreeNode) dropPoint.getNode();
      GenericNode userObject = Util.castNullable(GenericNode.class, targetNode.getUserObject());
      if (userObject != null) QueryDropAcceptor.acceptItems(wrappers, userObject, context);
    }
  }


  private class ImportingDialog extends JDialog {
    private JProgressBar myProgressBar;
    private JPanel myWholePanel;
    private JTextArea myErrorArea;
    private JScrollPane myErrorScrollpane;
    private final List<String> myErrors = Collections15.arrayList();

    private ImportingDialog(Frame owner) throws HeadlessException {
      super(owner);
      setModal(false);
      setTitle(Local.parse("Importing " + Terms.ref_Artifacts));
      add(myWholePanel);
      myWholePanel.setBorder(UIUtil.BORDER_5);
      Dimension d1 = UIUtil.getRelativeDimension(myProgressBar, 40, 1);
      Dimension d2 = UIUtil.getProgressBarPreferredSize(myProgressBar);
      myProgressBar.setPreferredSize(new Dimension(d1.width, d2.height));
      myErrorScrollpane.setVisible(false);
      myErrorArea.setEditable(false);
      UIUtil.setCaretAlwaysVisible(Lifespan.FOREVER, myErrorArea);
      pack();
    }

    public boolean hasErrors() {
      return !myErrors.isEmpty();
    }

    public void setMessage(String message) {
      myProgressBar.setString(message);
    }

    public void setDone() {
      myProgressBar.setValue(100);
      setMessage("Import finished");
    }

    public void setProgress(float percent) {
      int progress = Math.min(100, Math.max(0, (int) (percent * 100)));
      int diff = progress - myProgressBar.getValue();
      if (diff < 0 || diff >= 2) {
        myProgressBar.setValue(progress);
      }
    }

    public void setErrors(List<String> errors) {
      myErrors.clear();
      boolean hasErrors = errors != null && !errors.isEmpty();
      if (hasErrors) {
        myErrors.addAll(errors);
        StringBuilder errorText = new StringBuilder("Errors:");
        for (String error : errors) {
          errorText.append('\n').append(error);
        }
        String s = errorText.toString();
        if (!s.equals(myErrorArea.getText())) {
          myErrorArea.setText(s);
          UIUtil.scrollToBottom(myErrorArea);
        }
      }
      if (myErrorScrollpane.isVisible() != hasErrors) {
        myErrorScrollpane.setVisible(hasErrors);
        pack();
      }
    }
  }
}

