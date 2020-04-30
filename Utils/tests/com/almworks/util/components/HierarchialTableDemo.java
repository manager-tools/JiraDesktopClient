package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.tables.HierarchicalTable;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.InstanceProvider;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.dnd.*;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class HierarchialTableDemo implements Runnable {
  private final JScrollPane myScrollpane = new JScrollPane();
  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final OrderListModel<TableColumnAccessor<Yo, ?>> myColumnModel = OrderListModel.create();
  private final TreeModelBridge<Yo> myRoot = new TreeModelBridge<Yo>(yo("root"));
  private static final DataRole<Yo> YO_ROLE = DataRole.createRole(Yo.class);
  private static final DataRole<TreeModelBridge> NODE_ROLE = DataRole.createRole(TreeModelBridge.class);

  private static Yo yo(String name) {
    return new Yo(name);
  }

  private int myAW = 50;
  private ATable<Yo> myTable;
  private ATree<TreeModelBridge<String>> myTree;

  public HierarchialTableDemo() {
  }

  private void setColumns() {
    myColumnModel.addElement(new MyAccessor("A-A-A") {
      public boolean isOrderChangeAllowed() {
        return false;
      }

      public int getPreferredWidth(JTable table, ATableModel<Yo> tableModel, ColumnAccessor<Yo> renderingAccessor,
        int columnIndex)
      {
        return myAW;
      }

      public ColumnSizePolicy getSizePolicy() {
        return ColumnSizePolicy.FIXED;
      }
    });
    myColumnModel.addElement(new MyAccessor("B-B-B"));
    myColumnModel.addElement(new MyAccessor("C-C-C"));
  }

  public static void main(String[] args) {
    DndManager dnd = new DndManager();
    Context.add(new InstanceProvider<DndManager>(dnd, null), "x");
    Context.globalize();
    dnd.start();
    SwingUtilities.invokeLater(new HierarchialTableDemo());
  }

  public void run() {
    LAFUtil.initializeLookAndFeel();
    init();
    DebugFrame.show(myWholePanel);
  }

  private void init() {

    setColumns();
    setData();
    myTable = ATable.createInscrollPane(myScrollpane);
    final HierarchicalTable<Yo> htable = new HierarchicalTable<Yo>(myTable);
    htable.setColumnModel(myColumnModel);
    htable.setRoot(myRoot);
    htable.setRootVisible(false);
    htable.setGridHidden();
    htable.setDataRoles(YO_ROLE);
    htable.setTransfer(new MyTableTransfer());

    final InputMap map = htable.getSwingComponent().getInputMap(JComponent.WHEN_FOCUSED);
    final ActionMap actionMap = htable.getSwingComponent().getActionMap();
    final String printKEy = "printTable";
    map.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK), printKEy);
//    actionMap.put(printKEy, new AbstractAction() {
//      public void actionPerformed(ActionEvent e) {
//        //PrintingComponent.printTable(htable, new YoTableModel(htable.getTable().getTableModel()), htable.getTable().getColumnModel() );
//        PrintingComponent.printXX(htable, new YoTableModel(htable.getTable().getTableModel()), htable.getTable().getColumnModel() );
//      }
//    });

    myWholePanel.add(myScrollpane, BorderLayout.CENTER);

    myTree = new ATree<TreeModelBridge<String>>();
    myTree.setRoot(buildTree());
    myTree.setDataRoles(NODE_ROLE);
    myTree.setTransfer(new MyTreeTransfer());
    myWholePanel.add(new JScrollPane(myTree), BorderLayout.WEST);

    DndManager dndManager = DndManager.instance();
    dndManager.registerSource(Lifespan.FOREVER, myTable);

//    Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
//      public void eventDispatched(AWTEvent event) {
//        if (event instanceof MouseEvent)
//          System.out.println(event);
//      }
//    }, -1);
    //PrintingComponent.printTable(htable, new YoTableModel(htable.getTable().getTableModel()), htable.getTable().getColumnModel() );
  }


  private MyNode buildTree() {
    MyNode root = new MyNode("root");
    root.add(new MyNode("aa -- aa"));
    root.add(new MyNode("bb -- bb"));
    root.add(new MyNode("cc -- cc"));
    return root;
  }

  private void setData() {
    for (int i = 0; i < 10; i++) {
      myRoot.add(TreeModelBridge.create(yo("aaa " + i)));
      myRoot.add(TreeModelBridge.create(yo("bbb " + i)));
      myRoot.add(TreeModelBridge.create(yo("ccc " + i)));
      myRoot.add(TreeModelBridge.create(yo("ddd " + i)));
    }
  }

  private static class DataObject {

  }


  private static class MyRenderer extends LabelRenderer<Yo> {
    protected void setElement(Yo element, CellState state) {
      myLabel.setText(element.toString());
    }

    public JComponent getRendererComponent(CellState state, Yo element) {
      return super.getRendererComponent(state, element);
    }
  }


  private static class MyAccessor extends BaseTableColumnAccessor<Yo, String> {
    public MyAccessor(String name) {
      super(name, new MyRenderer());
    }

    public String getValue(Yo object) {
      return getName() + ": " + object;
    }
  }


  private static class MyNode extends TreeModelBridge<String> {
    public MyNode(String userObject) {
      super(userObject);
    }

    public void insert(ATreeNode<String> child, int index) {
      super.insert(child, index);
    }
  }


  private static class YoTransfer implements Transferable {
    private static final DataFlavor YO_FLAVOR = new DataFlavor("application/x-demo-yo-list", "Yo List") {
      public String getParameter(String paramName) {
        if ("class".equals(paramName)) {
          return java.util.List.class.getName();
        } else {
          return super.getParameter(paramName);
        }
      }
    };
    public static final TypedDataFlavor<List<Yo>> YO_DATA = new TypedDataFlavor(YO_FLAVOR, List.class);

    private static final DataFlavor textFlavor = DataFlavor.getTextPlainUnicodeFlavor();
    private static DataFlavor[] flavors = new DataFlavor[] {
      YO_FLAVOR, DataFlavor.stringFlavor, DataFlavor.plainTextFlavor, textFlavor};


    private final List<Yo> myYos;

    public YoTransfer(List<Yo> yos) {
      myYos = Collections15.arrayList(yos);
    }

    public DataFlavor[] getTransferDataFlavors() {
      return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
      System.out.println("asked about " + flavor);
      for (DataFlavor f : flavors)
        if (f.equals(flavor))
          return true;
      return false;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      if (YO_FLAVOR.equals(flavor)) {
        return myYos;
      } else if (DataFlavor.stringFlavor.equals(flavor)) {
        return getString();
      } else if (DataFlavor.plainTextFlavor.equals(flavor)) {
        return new ByteArrayInputStream(getString().getBytes("UTF-8"));
      } else if (textFlavor.equals(flavor)) {
        return new ByteArrayInputStream(getString().getBytes());
      } else {
        throw new UnsupportedFlavorException(flavor);
      }
    }

    private String getString() {
      StringBuffer buffer = new StringBuffer();
      for (Yo yo : myYos) {
        buffer.append(yo);
        buffer.append('\n');
      }
      return buffer.toString();
    }
  }


  private class MyTableTransfer implements ContextTransfer {
    public Transferable transfer(DragContext context) throws CantPerformException {
      List<Yo> yos = context.getSourceCollection(YO_ROLE);
      if (yos.size() == 0)
        throw new CantPerformException();
      return new YoTransfer(yos);
    }

    public void acceptTransfer(DragContext context, Transferable transferable) throws CantPerformException {

    }

    public void cleanup(DragContext context) throws CantPerformException {

    }

    public void remove(ActionContext context) throws CantPerformException {

    }

    public boolean canRemove(ActionContext context) throws CantPerformException {
      return false;
    }

    public boolean canMove(ActionContext context) throws CantPerformException {
      return canRemove(context);
    }

    public boolean canCopy(ActionContext context) throws CantPerformException {
      return true;
    }

    public boolean canLink(ActionContext context) throws CantPerformException {
      return true;
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


    public Factory<Image> getTransferImageFactory(DragContext dragContext) throws CantPerformException {
      List<Yo> yos = dragContext.getTransferData(YoTransfer.YO_DATA);
      return new StringDragImageFactory(yos.size() + " yos are dragged", myTable, null);
    }
  }


  private class MyTreeTransfer implements ContextTransfer {
    public Transferable transfer(DragContext context) throws CantPerformException {
      List<TreeModelBridge> nodes = context.getSourceCollection(NODE_ROLE);
      List<Yo> yos = Collections15.arrayList();
      for (TreeModelBridge node : nodes) {
        yos.add(new Yo(String.valueOf(node.getUserObject())));
      }
      return new YoTransfer(yos);
    }

    public void acceptTransfer(DragContext context, Transferable tranferred)
      throws CantPerformException, UnsupportedFlavorException, IOException
    {
      List<Yo> yos = YoTransfer.YO_DATA.getData(tranferred);
      TreeDropPoint dropPoint = ATree.getDropPoint(context);
      if (dropPoint == null || !dropPoint.isInsertNode())
        return;
      ATreeNode parent = (ATreeNode) dropPoint.getNode();
      int i = dropPoint.getInsertionIndex();
      for (Yo yo : yos) {
        parent.insert(TreeModelBridge.create(yo), i++);
      }
      myTree.expand(parent);
    }

    public void cleanup(DragContext context) throws CantPerformException {

    }

    public void remove(ActionContext context) throws CantPerformException {

    }

    public boolean canRemove(ActionContext context) throws CantPerformException {
      return false;
    }

    public boolean canMove(ActionContext context) throws CantPerformException {
      return canRemove(context);
    }

    public boolean canCopy(ActionContext context) throws CantPerformException {
      return context.getSourceCollection(NODE_ROLE).size() > 0;
    }

    public boolean canLink(ActionContext context) throws CantPerformException {
      return context.getSourceCollection(NODE_ROLE).size() > 0;
    }

    public boolean canImportData(DragContext context) throws CantPerformException {
      List<Yo> yos = context.getSourceObject(YoTransfer.YO_DATA);
      return yos.size() == 2;
    }


    public boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException {
      return canImportData(context) && ATree.getDropPoint(context) != null;
    }

    public void startDrag(DragContext dragContext, InputEvent event) throws CantPerformException {
      JTree jtree = myTree.getScrollable();
      if (event instanceof MouseEvent) {
        Point p = ((MouseEvent) event).getPoint();
        Point treePoint = SwingUtilities.convertPoint(event.getComponent(), p, jtree);
        TreePath path = jtree.getClosestPathForLocation(treePoint.x, treePoint.y);
        if (path != null) {
          Rectangle bounds = jtree.getPathBounds(path);
          if (bounds != null && bounds.contains(treePoint)) {
            dragContext.putValue(DndUtil.DRAG_SOURCE_OFFSET, new Point(treePoint.x - bounds.x, bounds.height));
          }
        }
      }
    }


    public boolean canImportFlavor(DataFlavor flavor) {
      return flavor.equals(YoTransfer.YO_FLAVOR);
    }

    public Factory<Image> getTransferImageFactory(DragContext context) throws CantPerformException {
      List<TreeModelBridge> nodes = context.getSourceCollection(NODE_ROLE);
      return new TreeNodesDragImageFactory(myTree.getScrollable(), nodes);
    }
  }


  private class YoTableModel extends AbstractTableModel {
    private ATableModel<Yo> myModel;

    public String getColumnName(int column) {
      final String id = myModel.getColumnModel().getAt(column).getId();
      return id;
    }

    public YoTableModel(ATableModel<Yo> tableModel) {
      myModel = tableModel;
    }

    public int getRowCount() {
      return myModel.getDataModel().getSize();
    }

    public int getColumnCount() {
      return 1;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      return myModel.getDataModel().getAt(rowIndex).toString();
    }
  }
}
