package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ListModelHolder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import com.almworks.util.ui.actions.globals.GlobalData;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.List;

public class ThumbnailView<T> implements FlatCollectionComponent<T> {
  private static final boolean NOT_MAC = !Aqua.isAqua();

  private static final int MINIMUM_HEIGHT = 50;
  private static final int MINIMUM_DIMENSION = 20;

  private final ThumbnailViewCanvas myCanvas = new ThumbnailViewCanvas();
  private final NotScrollableScrollPane myScrollPane = new NotScrollableScrollPane(myCanvas);
  private final ThumbnailViewUI<T> myUI;
  private final ListModelHolder<T> myModel = ListModelHolder.create();
  private final SelectionAccessor<T> mySelection;
  private final ListSelectionModel mySwingSelectionModel = new DefaultListSelectionModel();

  @NotNull
  private ThumbnailViewCellGeometry myCellGeometry = ThumbnailViewCellGeometry.NO_GEOMETRY;

  private int myMaximumHeight = Short.MAX_VALUE;
  private int myLastSelectionMinIndex = Integer.MAX_VALUE;
  private int myLastSelectionMaxIndex = Integer.MIN_VALUE;

  public ThumbnailView(ThumbnailViewUI<T> ui) {
    myUI = ui;
    mySelection = new ListSelectionAccessor<T>(this);
    SelectionDataProvider.installTo(this);
    listen();
  }

  public void setImmediateTooltips(boolean immediate) {
    if (immediate) {
      ImmediateTooltips.installImmediateTooltipManager(Lifespan.FOREVER, myCanvas);
    } else {
      ImmediateTooltips.uninstallImmediateTooltipManager(myCanvas);
    }
  }

  public void setPopupMenu(MenuBuilder builder) {
    UIUtil.addPopupTriggerListener(this);
    builder.addToComponent(Lifespan.FOREVER, myCanvas);
  }

  public void setScrollableMode(boolean scrollable) {
    myScrollPane.setScrollableMode(scrollable);
  }

  private void listen() {
    myModel.addListener(new AListModel.Listener() {
      public void onInsert(int index, int length) {
        revalidate();
      }

      public void onRemove(int index, int length, AListModel.RemovedEvent event) {
        revalidate();
      }

      public void onListRearranged(AListModel.AListEvent event) {
        revalidate();
      }

      public void onItemsUpdated(AListModel.UpdateEvent event) {
        repaintItems(event.getLowAffectedIndex(), event.getHighAffectedIndex());
      }
    });

    mySwingSelectionModel.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        int[] selected = mySelection.getSelectedIndexes();
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int index : selected) {
          if (index > max)
            max = index;
          if (index < min)
            min = index;
        }
        int lead = mySwingSelectionModel.getLeadSelectionIndex();
        int anchor = mySwingSelectionModel.getAnchorSelectionIndex();
        min = Math.min(Math.min(min, lead < 0 ? Integer.MAX_VALUE : lead), anchor < 0 ? Integer.MAX_VALUE : anchor);
        max = Math.max(Math.max(max, lead), anchor);
        repaintItems(Math.min(min, myLastSelectionMinIndex), Math.max(max, myLastSelectionMaxIndex));
        myLastSelectionMaxIndex = max;
        myLastSelectionMinIndex = min;
      }
    });
  }

  public static <T> ThumbnailView<T> create(ThumbnailViewUI<T> ui) {
    return new ThumbnailView<T>(ui);
  }

  public ListSelectionModel getSelectionModel() {
    return mySwingSelectionModel;
  }

  public AListModel<? extends T> getCollectionModel() {
    return myModel;
  }

  public Detach setCollectionModel(AListModel<? extends T> model) {
    return myModel.setModel(model);
  }

  public int getElementIndexAt(int x, int y) {
    return viewToModel(new Point(x, y));
  }

  public T getElementAt(Point point) {
    return AComponentUtil.getElementAtPoint(this, point);
  }

  public int getScrollingElementAt(int x, int y) {
    return getElementIndexAt(x, y);
  }

  public Rectangle getElementRect(int elementIndex) {
    ThumbnailViewCellGeometry geometry = myCellGeometry;
    if (geometry == ThumbnailViewCellGeometry.NO_GEOMETRY) {
      assert false : this;
      return new Rectangle();
    }
    if (elementIndex < 0 || elementIndex >= myModel.getSize()) {
      assert false : elementIndex + " " + myModel;
      return new Rectangle();
    }
    int cellWidth = geometry.getCellWidth();
    int cellHeight = getCellHeight(geometry);
    Insets insets = myCanvas.getInsets();
    int rowSize = getRowSize();
    int row = elementIndex / rowSize;
    int column = elementIndex % rowSize;
    int x = insets.left + column * cellWidth;
    int y = insets.top + row * cellHeight;
    return new Rectangle(x, y, cellWidth, cellHeight);
  }

  public SelectionAccessor<T> getSelectionAccessor() {
    return mySelection;
  }

  public JComponent toComponent() {
    return myScrollPane;
  }

  public JComponent getSwingComponent() {
    return myCanvas;
  }

  public void scrollSelectionToView() {
    int index = mySelection.getSelectedIndex();
    if (index >= 0) {
      Rectangle rect = getElementRect(index);
      if (rect != null) {
        myCanvas.scrollRectToVisible(rect);
      }
    }
  }

  public void setTransfer(ContextTransfer transfer) {
    throw new UnsupportedOperationException();
  }


  public void setDataRoles(DataRole... roles) {
    SelectionDataProvider.setRoles(this, roles);
  }

  public void addGlobalRoles(DataRole<?>... roles) {
    SelectionDataProvider.addRoles(this, roles);
    GlobalData.KEY.addClientValue(this.toComponent(), roles);
  }


  public int getPreferredHeight(int width) {
    ThumbnailViewCellGeometry geometry = myCellGeometry;
    if (geometry == ThumbnailViewCellGeometry.NO_GEOMETRY) {
      return MINIMUM_HEIGHT;
    }

    AListModel<T> model = myModel;
    if (model.getSize() == 0) {
      return MINIMUM_HEIGHT;
    }

    int cellHeight = getCellHeight(geometry);
    int cellWidth = geometry.getCellWidth();
    Insets insets = AwtUtil.uniteInsetsFromTo(myCanvas, myScrollPane);

    int realWidth = width - insets.left - insets.right;
    if (realWidth <= MINIMUM_DIMENSION) {
      return MINIMUM_HEIGHT;
    }

    int rowSize = Math.max(1, realWidth / cellWidth);
    int rows = (model.getSize() + rowSize - 1) / rowSize;
    int rowsHeight = rows * cellHeight;

    return Math.max(MINIMUM_HEIGHT, Math.min(rowsHeight + insets.top + insets.bottom, myMaximumHeight));
  }

  private int getCellHeight(ThumbnailViewCellGeometry g) {
    return g.getImageTopMargin() + g.getImageHeight() + g.getImageTextGap() + getTextHeight() + g.getTextBottomMargin();
  }

  private int getTextHeight() {
    Font font = myCanvas.getFont();
    FontMetrics metrics = myCanvas.getFontMetrics(font);
    int textHeight = metrics.getHeight();
    return textHeight;
  }

  public void setMaximumHeight(int maximumHeight) {
    myMaximumHeight = maximumHeight < MINIMUM_HEIGHT ? Short.MAX_VALUE : maximumHeight;
  }

  public void revalidate() {
    AListModel<T> model = myModel;
    if (model == null)
      return;
    List<T> list = model.toList();
    ThumbnailViewCellGeometry geometry = myUI.getCellGeometry(list, myCanvas);
    myCellGeometry = Util.NN(geometry, ThumbnailViewCellGeometry.NO_GEOMETRY);
    repaintAll();
  }

  private void repaintAll() {
    myScrollPane.invalidate();
    myCanvas.invalidate();
    myScrollPane.repaint();
    Container parent = myScrollPane.getParent();
    if (parent instanceof JComponent) {
      ((JComponent) parent).revalidate();
    }
  }

  public void repaintItems(int lowIndex, int highIndex) {
    if (lowIndex < 0)
      lowIndex = 0;
    int size = myModel.getSize();
    if (highIndex >= size)
      highIndex = size - 1;
    if (lowIndex > highIndex)
      return;
    if (lowIndex == 0 && highIndex == size - 1) {
      repaintAll();
    } else {
      Rectangle view = myScrollPane.getViewport().getViewRect();
      for (int i = lowIndex; i <= highIndex; i++) {
        Rectangle r = getElementRect(i);
        if (r != null) {
          Rectangle rect = r.intersection(view);
          if (!rect.isEmpty()) {
            myCanvas.repaint(rect);
          }
        }
      }
    }
  }

  private int getRowSize() {
    ThumbnailViewCellGeometry geometry = myCellGeometry;
    if (geometry == ThumbnailViewCellGeometry.NO_GEOMETRY) {
      return 1;
    }
    int cellWidth = geometry.getCellWidth();
    Insets insets = myCanvas.getInsets();
    return Math.max(1, (myCanvas.getWidth() - insets.left - insets.right) / cellWidth);
  }

  /**
   * Returns index in collection model that corresponds to the point p in ThumbnailView.Canvas coordinates.
   * Returns -1 if point does not correspond to any cell.
   */
  public int viewToModel(Point p) {
    if (p == null)
      return -1;

    ThumbnailViewCellGeometry geometry = myCellGeometry;
    if (geometry == ThumbnailViewCellGeometry.NO_GEOMETRY) {
      return -1;
    }

    int cellWidth = geometry.getCellWidth();
    int cellHeight = getCellHeight(geometry);
    Insets insets = myCanvas.getInsets();
    int rowSize = getRowSize();
    int x = p.x - insets.left;
    int y = p.y - insets.top;
    if (x < 0 || y < 0)
      return -1;
    int column = x / cellWidth;
    if (column >= rowSize)
      return -1;
    int row = y / cellHeight;
    int index = row * rowSize + column;
    if (index >= 0 && index < myModel.getSize())
      return index;
    else
      return -1;
  }

  private void select(int index, boolean ctrl, boolean shift, boolean keyboard) {
    int anchor = mySwingSelectionModel.getAnchorSelectionIndex();
    if (ctrl) {
      if (shift && anchor != -1) {
        if (mySwingSelectionModel.isSelectedIndex(anchor)) {
          mySwingSelectionModel.addSelectionInterval(anchor, index);
        } else {
          mySwingSelectionModel.removeSelectionInterval(anchor, index);
          mySwingSelectionModel.addSelectionInterval(index, index);
          mySwingSelectionModel.setAnchorSelectionIndex(anchor);
        }
      } else if (keyboard) {
        int lead = mySwingSelectionModel.getLeadSelectionIndex();
        if (mySwingSelectionModel.isSelectedIndex(index)) {
          mySwingSelectionModel.addSelectionInterval(index, index);
        } else {
          mySwingSelectionModel.removeSelectionInterval(index, index);
        }
        repaintItems(Math.min(Math.min(lead, index), anchor), Math.max(Math.max(lead, index), anchor));
      } else if (mySwingSelectionModel.isSelectedIndex(index)) {
        mySwingSelectionModel.removeSelectionInterval(index, index);
      } else {
        mySwingSelectionModel.addSelectionInterval(index, index);
      }
    } else if (shift && (anchor != -1)) {
      mySwingSelectionModel.setSelectionInterval(anchor, index);
    } else {
      mySwingSelectionModel.setSelectionInterval(index, index);
    }
  }


  @Nullable
  private Transferable createTransferrable() {
    if (!myUI.isTransferSupported())
      return null;
    List<T> selected = mySelection.getSelectedItems();
    if (selected.isEmpty())
      return null;
    return myUI.createTransferable(selected);
  }

  private boolean isDragPossible() {
    return myUI.isTransferSupported();
  }






  private class ThumbnailViewCanvas extends JComponent {
    private Color mySelectedBackground;
    private Color mySelectedForeground;

    private Handler myHandler;

    private final Insets myPaintInsets = new Insets(0, 0, 0, 0);
    private final Rectangle myPaintCellRect = new Rectangle();
    private final Rectangle myPaintImageRect = new Rectangle();
    private final Rectangle myPaintTextRect = new Rectangle();
    private final Rectangle myPaintLayoutTextR = new Rectangle();
    private final Rectangle myPaintLayoutIconR = new Rectangle();

    private final JLabel myDummyRendererLabel = new JLabel();

    private Point myLastMousePoint;

    public ThumbnailViewCanvas() {
      setOpaque(true);
      setAutoscrolls(true);
      updateUI();
    }

    public void updateUI() {
      super.updateUI();
      setBackground(UIUtil.getEditorBackground());
      setForeground(UIUtil.getEditorForeground());
      mySelectedBackground = UIManager.getColor("EditorPane.selectionBackground");
      mySelectedForeground = UIManager.getColor("EditorPane.selectionForeground");
      if (myHandler == null) {
        myHandler = new Handler();
        addMouseListener(myHandler);
        addFocusListener(myHandler);
        installActionMap();
        installTransferHandler();
      }
    }

    private void installTransferHandler() {
      setTransferHandler(new CanvasTransferHandler());
      DragHandler dragHandler = new DragHandler();
      addMouseListener(dragHandler);
      addMouseMotionListener(dragHandler);
    }

    private void installActionMap() {
      addArrowActions(KeyEvent.VK_LEFT);
      addArrowActions(KeyEvent.VK_RIGHT);
      addArrowActions(KeyEvent.VK_UP);
      addArrowActions(KeyEvent.VK_DOWN);
      addArrowActions(KeyEvent.VK_HOME);
      addArrowActions(KeyEvent.VK_END);
      addArrowActions(KeyEvent.VK_PAGE_UP);
      addArrowActions(KeyEvent.VK_PAGE_DOWN);

      AbstractAction spaceAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (!isEnabled())
            return;
          if (myModel.getSize() == 0)
            return;
          int lead = mySwingSelectionModel.getLeadSelectionIndex();
          if (lead < 0)
            return;
          if (mySwingSelectionModel.isSelectedIndex(lead)) {
            mySwingSelectionModel.removeSelectionInterval(lead, lead);
          } else {
            mySwingSelectionModel.addSelectionInterval(lead, lead);
          }
        }
      };
      addAction(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), spaceAction);
      addAction(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), spaceAction);

      addAction(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (!isEnabled())
            return;
          if (myModel.getSize() == 0)
            return;
          int lead = mySwingSelectionModel.getLeadSelectionIndex();
          mySelection.selectAll();
          mySwingSelectionModel.addSelectionInterval(lead, lead);
        }
      });
    }

    private void addArrowActions(int key) {
      ArrowAction action;

      action = new ArrowAction(key, false, false);
      addAction(KeyStroke.getKeyStroke(key, 0), action);

      action = new ArrowAction(key, true, false);
      addAction(KeyStroke.getKeyStroke(key, InputEvent.SHIFT_DOWN_MASK), action);

      action = new ArrowAction(key, false, true);
      addAction(KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK), action);

      action = new ArrowAction(key, true, true);
      addAction(KeyStroke.getKeyStroke(key, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK), action);
    }

    private void addAction(KeyStroke keyStroke, Action action) {
      getInputMap(WHEN_FOCUSED).put(keyStroke, action);
      getActionMap().put(action, action);
    }


    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      AwtUtil.applyRenderingHints(g2);

      int width = getWidth();

      ThumbnailViewCellGeometry geometry = myCellGeometry;
      if (geometry == ThumbnailViewCellGeometry.NO_GEOMETRY)
        return;

      AListModel<T> model = myModel;
      if (model == null)
        return;

      int size = model.getSize();
      if (size == 0)
        return;

      int cellWidth = geometry.getCellWidth();
      int cellHeight = getCellHeight(geometry);
      if (cellWidth < MINIMUM_DIMENSION || cellHeight < MINIMUM_DIMENSION) {
        assert false : "cell " + geometry;
        return;
      }

      Insets insets = getInsets(myPaintInsets);
      Rectangle clip = g.getClipBounds();

      g.setColor(getBackground());
//      g.fillRect(clip.x, clip.y, clip.width + 1, clip.height + 1);
      g.fillRect(0, 0, width, getHeight());

      int rowSize = getRowSize();
      int skipRows = clip.y > insets.top ? (clip.y - insets.top) / cellHeight : 0;
      int skipColumns = clip.x > insets.left ? (clip.x - insets.left) / cellWidth : 0;

      boolean hasFocus = ListSpeedSearch.isFocusOwner(this);

      for (int row = skipRows; row * rowSize + skipColumns < size; row++) {
        int base = row * rowSize;
        int y = insets.top + row * cellHeight;
        if (y >= clip.y + clip.height) {
          break;
        }
        for (int column = skipColumns; base + column < size && column < rowSize; column++) {
          int index = base + column;
          int x = insets.left + column * cellWidth;
          if (x >= clip.x + clip.width) {
            break;
          }

          if (index < model.getSize()) {
            T item = model.getAt(index);
            boolean selected = mySelection.isSelectedAt(index);
            boolean focused = hasFocus && index == mySwingSelectionModel.getLeadSelectionIndex();
            myPaintCellRect.x = x;
            myPaintCellRect.y = y;
            myPaintCellRect.width = cellWidth;
            myPaintCellRect.height = cellHeight;
            paintItem(g2, item, myPaintCellRect, geometry, selected, focused);
          } else {
            assert false : index + " " + model;
            break;
          }
        }
      }
    }

    private void paintItem(Graphics2D g, T item, Rectangle cellRect, ThumbnailViewCellGeometry geometry,
      boolean selected, boolean focused)
    {
      myPaintImageRect.width = geometry.getImageWidth();
      myPaintImageRect.height = geometry.getImageHeight();
      myPaintImageRect.x = cellRect.x + (cellRect.width - myPaintImageRect.width) / 2;
      myPaintImageRect.y = cellRect.y + geometry.getImageTopMargin();

      myPaintTextRect.x = cellRect.x + geometry.getTextSideMargin();
      myPaintTextRect.y = myPaintImageRect.y + myPaintImageRect.height + geometry.getImageTextGap();
      myPaintTextRect.width = cellRect.width - 2 * geometry.getTextSideMargin();
      myPaintTextRect.height = cellRect.y + cellRect.height - geometry.getTextBottomMargin() - myPaintTextRect.y;

      paintItemImage(g, item, myPaintImageRect);

      if (selected) {
        g.setColor(mySelectedBackground);
        myPaintImageRect.grow(1, 1);
        g.draw(myPaintImageRect);
        myPaintImageRect.grow(1, 1);
        g.draw(myPaintImageRect);
      } else {
        myPaintImageRect.grow(2, 2);
      }

      paintItemText(g, item, myPaintTextRect, selected, focused);
    }

    private void paintItemText(Graphics2D g, T item, Rectangle paintViewR, boolean selected, boolean focused) {
      Font font = getFont();
      g.setFont(font);
      FontMetrics metrics = getFontMetrics(font);
      String text = myUI.getItemText(item);

      myPaintLayoutTextR.x = myPaintLayoutTextR.y = myPaintLayoutTextR.width = myPaintLayoutTextR.height = 0;
      myPaintLayoutIconR.x = myPaintLayoutIconR.y = myPaintLayoutIconR.width = myPaintLayoutIconR.height = 0;

      String clippedText = SwingUtilities.layoutCompoundLabel(this, metrics, text, null, SwingConstants.CENTER,
        SwingConstants.CENTER, SwingConstants.CENTER, SwingConstants.LEADING, paintViewR, myPaintLayoutIconR,
        myPaintLayoutTextR, 0);

      int textX = myPaintLayoutTextR.x;
      int textY = myPaintLayoutTextR.y + metrics.getAscent();

      myPaintLayoutTextR.grow(2, 0);
      myPaintLayoutTextR.height += 1;
      g.setPaint(selected ? mySelectedBackground : getBackground());
      g.fill(myPaintLayoutTextR);

      g.setColor(selected ? mySelectedForeground : getForeground());

      g.drawString(clippedText, textX, textY);

      if (focused) {
        myDummyRendererLabel.setBackground(selected ? mySelectedBackground : getBackground());
        if(NOT_MAC) {
          Border border = UIManager.getBorder("List.focusCellHighlightBorder");
          border.paintBorder(myDummyRendererLabel, g, myPaintLayoutTextR.x, myPaintLayoutTextR.y,
            myPaintLayoutTextR.width, myPaintLayoutTextR.height);
        }
      }
    }

    private void paintItemImage(Graphics g, T item, Rectangle r) {
      myUI.paintItemImage(this, g, item, new Rectangle(r));
    }


    public String getToolTipText() {
      int index = viewToModel(myLastMousePoint);
      if (index < 0 || index >= myModel.getSize())
        return null;
      else
        return myUI.getTooltipText(myModel.getAt(index));
    }


    protected void processMouseEvent(MouseEvent e) {
      myLastMousePoint = e.getPoint();
      super.processMouseEvent(e);
    }


    protected void processMouseMotionEvent(MouseEvent e) {
      myLastMousePoint = e.getPoint();
      super.processMouseMotionEvent(e);
    }

    public ThumbnailView<T> getThumbnailView() {
      return ThumbnailView.this;
    }
  }


  /**
   * Partly copied from BasicListUI
   */
  private class Handler implements MouseListener, MouseMotionListener, FocusListener {
    private boolean mySelectedOnPress;

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
      mySelectedOnPress = !e.isConsumed();
      if (mySelectedOnPress) {
        adjustFocusAndSelection(e);
      }
    }

    public void mouseReleased(MouseEvent e) {
      if (mySelectedOnPress) {
        if (!SwingUtilities.isLeftMouseButton(e)) {
          return;
        }
        mySwingSelectionModel.setValueIsAdjusting(false);
      } else {
        adjustFocusAndSelection(e);
      }
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    private void adjustFocusAndSelection(MouseEvent e) {
      if (!SwingUtilities.isLeftMouseButton(e)) {
        return;
      }

      if (!myCanvas.isEnabled()) {
        return;
      }

      /* Request focus before updating the list selection.  This implies
             * that the current focus owner will see a focusLost() event
             * before the lists selection is updated IF requestFocus() is
             * synchronous (it is on Windows).  See bug 4122345
             */
      if (!myCanvas.hasFocus() && myCanvas.isRequestFocusEnabled()) {
          myCanvas.requestFocus();
      }
      adjustSelection(e);
    }

    protected void adjustSelection(MouseEvent e) {
      int index = viewToModel(e.getPoint());
      if (index < 0) {
        // If shift is down in multi-select, we should do nothing.
        // For single select or non-shift-click, clear the selection
        if (e.getID() == MouseEvent.MOUSE_PRESSED && !e.isShiftDown()) {
          mySelection.clearSelection();
        }
      } else {
        select(index, e.isControlDown(), e.isShiftDown(), false);
      }
    }


    protected void repaintCellFocus() {
      int leadIndex = mySwingSelectionModel.getLeadSelectionIndex();
      if (leadIndex >= 0 && leadIndex < myModel.getSize()) {
        Rectangle r = getElementRect(leadIndex);
        if (r != null) {
          myCanvas.repaint(r.x, r.y, r.width, r.height);
        }
      }
    }

    public void focusGained(FocusEvent e) {
      repaintCellFocus();
    }

    public void focusLost(FocusEvent e) {
      repaintCellFocus();
    }


    public void mouseDragged(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {

    }
  }


  private class ArrowAction extends AbstractAction {
    private final int myKey;
    private final boolean myShift;
    private final boolean myCtrl;

    public ArrowAction(int key, boolean shift, boolean ctrl) {
      myKey = key;
      myShift = shift;
      myCtrl = ctrl;
    }

    public void actionPerformed(ActionEvent e) {
      int size = myModel.getSize();
      if (size == 0)
        return;
      if (!isEnabled())
        return;
      int lead = mySwingSelectionModel.getLeadSelectionIndex();
      int jump = getJump(lead, size);
      select(jump, myCtrl, myShift, true);
    }

    private int getJump(int lead, int size) {
      int jump;
      if (lead == -1) {
        if (myKey == KeyEvent.VK_END) {
          jump = size - 1;
        } else {
          jump = 0;
        }
      } else {
        int rowSize = getRowSize();
        if (myKey == KeyEvent.VK_LEFT)
          jump = lead - 1;
        else if (myKey == KeyEvent.VK_RIGHT)
          jump = lead + 1;
        else if (myKey == KeyEvent.VK_UP)
          jump = lead >= rowSize ? lead - rowSize : lead;
        else if (myKey == KeyEvent.VK_DOWN)
          jump = lead < size - rowSize ? lead + rowSize : lead;
        else if (myKey == KeyEvent.VK_HOME)
          jump = 0;
        else if (myKey == KeyEvent.VK_END)
          jump = size - 1;
        else if (myKey == KeyEvent.VK_PAGE_UP)
          jump = lead - rowSize;
        else if (myKey == KeyEvent.VK_PAGE_DOWN)
          jump = lead + rowSize;
        else {
          assert false : myKey;
          jump = lead;
        }
      }
      jump = Math.max(0, Math.min(size - 1, jump));
      return jump;
    }
  }


  private class CanvasTransferHandler extends TransferHandler {
    protected Transferable createTransferable(JComponent c) {
      return ThumbnailView.this.createTransferrable();
    }

    public int getSourceActions(JComponent c) {
      return COPY;
    }
  }


  private class DragHandler implements MouseListener, MouseMotionListener {
    private MouseEvent myArmEvent = null;
    private static final int DRAG_TRESHOLD = 5; // todo get from desktop property

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
      myArmEvent = null;
      if (ThumbnailView.this.isDragPossible()) {
        myArmEvent = e;
//        e.consume();
      }
    }

    public void mouseReleased(MouseEvent e) {
      myArmEvent = null;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
      if (myArmEvent != null) {
        e.consume();
        int action = TransferHandler.COPY;
        int dx = Math.abs(e.getX() - myArmEvent.getX());
        int dy = Math.abs(e.getY() - myArmEvent.getY());
        int threshold = DRAG_TRESHOLD;
        if ((dx > threshold) || (dy > threshold)) {
          // start transfer... shouldn't be a click at this point
//          JComponent c = getComponent(e);
          JComponent c = myCanvas;
          TransferHandler th = c.getTransferHandler();
          assert th != null;
          th.exportAsDrag(c, myArmEvent, action);
          myArmEvent = null;
        }
      }
    }

    public void mouseMoved(MouseEvent e) {
    }
  }

}
