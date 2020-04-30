package com.almworks.util.components;

import com.almworks.util.Getter;
import com.almworks.util.components.plaf.macosx.MacKeyboardSelectionPatch;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.dnd.DndComponentAdapter;
import com.almworks.util.ui.actions.dnd.TreeInsertDropHintPaintOver;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

public class JTreeAdapter extends JTree implements HighlighterTree, DndComponentAdapter<TreeDropHint> {
  private static final boolean TAKE_PIXEL_MORE = false;
  private static final int ROUNDING = 10;

  private final List<HighlighterTreeElement> myHighlighted = Collections15.arrayList();
  private static final BasicStroke NORMAL_STROKE = new BasicStroke(0.8F);
  private static final BasicStroke COVERING_STROKE =
    new BasicStroke(0.8F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10F, new float[] {5f, 3f}, 0f);

  private boolean myDndActive;
  private boolean myDndEnabled;
  private boolean myInhibitSelectionChange;
  private TreeDropHint myTreeDropHint;
  private int myHeightDiff;
  private boolean myScrollSizeIsPref = false;
  private final Lifecycle myInsertTargetHintLifecycle = new Lifecycle();
  private final TreeModelListener myInsertHintModelListener = new DropHintInvalidator();
  private FocusListener myFocusListener;

  public JTreeAdapter(TreeModel model) {
    super(model);
    super.setOpaque(false);
  }

  public void updateUI() {
    FocusListener listener = getFocusListener();
    removeFocusListener(listener);
    super.updateUI();
    addFocusListener(listener);
    MacKeyboardSelectionPatch.install(this);
  }

  private FocusListener getFocusListener() {
    if (myFocusListener == null) {
      myFocusListener = new FocusListener() {
        public void focusGained(FocusEvent e) {
          repaintSelection();
        }

        public void focusLost(FocusEvent e) {
          repaintSelection();
        }
      };
    }
    return myFocusListener;
  }

  private void repaintSelection() {
    TreeSelectionModel model = getSelectionModel();
    int min = model.getMinSelectionRow();
    if (min < 0)
      return;
    int max = model.getMaxSelectionRow();
    if (max < min)
      return;
    Dimension size = getSize();
    for (int i = min; i <= Math.min(max, getRowCount() - 1); i++) {
      if (model.isRowSelected(i)) {
        Rectangle rect = getRowBounds(i);
        if (rect != null) {
          rect.x = 0;
          rect.width = size.width;
          repaint(rect);
        }
      }
    }
  }

  public void setOpaque(boolean isOpaque) {
    // ignore
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    paintOpaque(g);
//    boolean painted = doPaintHintedInsertion(g);
    boolean painted = false;
    if (!painted) {
      doPaint(g);
      if (!doPaintHintedInsertion2(g)) {
        doPaintHintedConsumption(g);
      }
    }
  }

  private void doPaintHintedConsumption(Graphics g) {
    TreeDropHint hint = myTreeDropHint;
    if (hint == null)
      return;
    TreeDropPoint dropPoint = hint.getDropPoint();
    if (dropPoint == null)
      return;
    if (dropPoint.isInsertNode())
      return;
    int row = hint.getHintRow();
    Rectangle bounds = getRowBounds(row);
    if (bounds == null)
      return;

    Rectangle clip = g.getClipBounds();
    if (!clip.intersects(bounds))
      return;

    Color c = GlobalColors.DRAG_AND_DROP_COLOR;
    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0x44));
    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    g.setColor(c);
    g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
  }

  private boolean doPaintHintedInsertion2(Graphics g) {
    TreeDropHint hint = myTreeDropHint;
    if (hint == null)
      return false;
    TreeDropPoint dropPoint = hint.getDropPoint();
    if (dropPoint == null)
      return false;
    if (!dropPoint.isInsertNode())
      return false;
    Object insertionParent = dropPoint.getNode();
    int insertionIndex = dropPoint.getInsertionIndex();
    int hintY;
    int rowCount = getRowCount();
    if (rowCount == 0) {
      assert false;
      return false;
    }
    int row = hint.getHintRow();
    if (row == rowCount) {
      Rectangle bounds = getRowBounds(row - 1);
      if (bounds == null)
        return false;
      hintY = bounds.y + bounds.height;
    } else {
      Rectangle bounds = getRowBounds(row);
      if (bounds == null)
        return false;
      hintY = bounds.y;
    }

    TreeInsertDropHintPaintOver painter = TreeInsertDropHintPaintOver.INSTANCE;
    boolean success = painter.paintSetup(this, insertionParent, insertionIndex, row, hint.getDragSourceSize());
    if (!success)
      return false;
    boolean wasDB = isDoubleBuffered();
    Graphics gg = g.create();
    try {
      gg.setColor(getBackground());
      gg.setColor(getForeground());
      painter.paint((Graphics2D) gg, this, hintY, getWidth());
    } finally {
      gg.dispose();
    }
    if (wasDB) {
      setDoubleBuffered(true);
    }
    return true;
  }
//  private boolean doPaintHintedInsertion(Graphics g) {
//    TreeDropHint hint = myTreeDropHint;
//    if (hint == null)
//      return false;
//    TreeDropPoint dropPoint = hint.getDropPoint();
//    if (dropPoint == null)
//      return false;
//    if (!dropPoint.isInsertNode())
//      return false;
//    Object insertionParent = dropPoint.getNode();
//    int insertionIndex = dropPoint.getInsertionIndex();
//    int hintY;
//    int rowCount = getRowCount();
//    if (rowCount == 0) {
//      assert false;
//      return false;
//    }
//    int row = hint.getHintRow();
//    if (row == rowCount) {
//      Rectangle bounds = getRowBounds(row - 1);
//      if (bounds == null)
//        return false;
//      hintY = bounds.y + bounds.height;
//    } else {
//      Rectangle bounds = getRowBounds(row);
//      if (bounds == null)
//        return false;
//      hintY = bounds.y;
//    }
//
//    TreeInsertDropHint painter = TreeInsertDropHint.INSTANCE;
//    boolean success = painter.paintSetup(this, insertionParent, insertionIndex, row, hint.getDragSourceSize());
//    if (!success)
//      return false;
//
//    boolean wasDB = isDoubleBuffered();
//    int hintHeight = painter.getHeight(this);
//
//    Graphics gg;
//    gg = g.create();
//    try {
//      gg.clipRect(0, 0, Short.MAX_VALUE, hintY);
//      if (!gg.getClipBounds().isEmpty()) {
//        doPaint(gg);
//      }
//    } finally {
//      gg.dispose();
//    }
//
//    gg = g.create();
//    try {
//      gg.clipRect(0, hintY, Short.MAX_VALUE, Short.MAX_VALUE);
//      gg.translate(0, hintHeight);
//      if (!gg.getClipBounds().isEmpty()) {
//        doPaint(gg);
//      }
//    } finally {
//      gg.dispose();
//    }
//
//    gg = g.create();
//    try {
//      gg.setColor(getBackground());
//      gg.fillRect(0, hintY, getWidth(), hintHeight);
//      gg.setColor(getForeground());
//
//      painter.paint((Graphics2D) gg, this, new Rectangle(0, hintY, getWidth(), hintHeight));
//    } finally {
//      gg.dispose();
//    }
//    if (wasDB) {
//      setDoubleBuffered(true);
//    }
//    return true;
//  }

  private void doPaint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      try {
        super.paintComponent(g);
      } catch (NullPointerException e) {
        Log.warn("ignoring NPE in JTA", e);
      }
      paintHighlightedSubtrees(g2);
    } finally {
      g2.dispose();
    }
  }

  private void paintHighlightedSubtrees(Graphics2D g) {
    TreeModel m = getModel();
    if (!(m instanceof DefaultTreeModel)) {
      return;
    }
    DefaultTreeModel model = ((DefaultTreeModel) m);
    if (myHighlighted == null || myHighlighted.size() == 0) {
      return;
    }
    boolean orderChanged = calculateRows(model);
    if (orderChanged) {
      Collections.sort(myHighlighted);
    }
    paintCalculatedNodes(g, model);
  }

  private void paintCalculatedNodes(Graphics2D g, DefaultTreeModel model) {
    int rowCount = getRowCount();
    Rectangle clipBounds = g.getClipBounds();
    for (HighlighterTreeElement elem : myHighlighted) {
      int startRow = elem.getCachedRow();
      TreeNode subtreeRoot = elem.getCachedNode();
      if (startRow < 0 || startRow >= rowCount || subtreeRoot == null)
        continue;
      boolean covering = isCovering(startRow, subtreeRoot) && !(subtreeRoot == model.getRoot());
      int endRow = covering ? startRow : getEndRow(model, subtreeRoot, startRow);
      assert endRow >= startRow;
      Rectangle b1 = getRowBounds(startRow);
      Rectangle b2 = getRowBounds(endRow);
      Insets insets = getInsets();
      Rectangle r = new Rectangle(insets.left, b1.y - (TAKE_PIXEL_MORE ? 1 : 0),
        getWidth() - insets.left - insets.right, b2.y + b2.height - b1.y + (TAKE_PIXEL_MORE ? 2 : 0));
      if (!clipBounds.intersects(r)) {
        // no need to draw
        continue;
      }
      Container parent = getParent();
      if (parent instanceof ScrollableWrapper) {
        parent = parent.getParent();
      }
      if (parent instanceof JViewport) {
        Rectangle viewRect = ((JViewport) parent).getViewRect();
        r.x = Math.max(viewRect.x, r.x);
        r.width = Math.min(viewRect.width, r.width);
      }
      paintElement(g, covering, r, elem);
    }
  }

  private boolean calculateRows(DefaultTreeModel model) {
    boolean orderChanged = false;
    for (HighlighterTreeElement elem : myHighlighted) {
      Getter<TreeNode> getter = elem.getRootGetter();
      TreeNode subtreeRoot = getter.get();
      int startRow;
      if (subtreeRoot != null) {
        // assert: subtreeRoot must be in this model
        startRow = getStartRow(model, subtreeRoot);
        if (startRow == -1 && model.getRoot() == subtreeRoot && !isRootVisible() && getRowCount() > 0) {
          startRow = 0;
        }
      } else {
        startRow = -1;
      }
      boolean changed = elem.setCachedRow(startRow);
      elem.setCachedNode(subtreeRoot);
      orderChanged |= changed;
    }
    return orderChanged;
  }

  private void paintElement(Graphics2D graphics, boolean covering, Rectangle r, HighlighterTreeElement element) {
    Color c = element.getColor();
    if (c == null) {
      // do not paint
      return;
    }
    Graphics2D g = (Graphics2D) graphics.create();
    try {
      RoundRectangle2D.Float fill = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, ROUNDING, ROUNDING);
      RoundRectangle2D.Float frame =
        new RoundRectangle2D.Float(r.x, r.y, r.width - 1, r.height - 1, ROUNDING, ROUNDING);
      g.setColor(c);
      if (!covering) {
        fill(g, element, fill);
      }
      drawFrame(g, covering, frame);
      if (!covering) {
        paintCaption(g, element, r, frame, c);
      }
    } finally {
      g.dispose();
    }
  }

  private void drawFrame(Graphics2D g, boolean covering, RoundRectangle2D.Float frame) {
    RenderingHints savedHints = g.getRenderingHints();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    BasicStroke stroke = covering ? COVERING_STROKE : NORMAL_STROKE;
    g.setStroke(stroke);
    g.draw(frame);
    g.setRenderingHints(savedHints);
  }

  private void fill(Graphics2D g, HighlighterTreeElement element, RoundRectangle2D.Float fill) {
    Composite savedComposite = g.getComposite();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, element.getFillAlpha()));
    g.fill(fill);
    g.setComposite(savedComposite);
  }

  private void paintCaption(Graphics2D g, HighlighterTreeElement element, Rectangle r, RoundRectangle2D.Float frame,
    Color frameColor)
  {
    Icon icon = element.getIcon();
    BufferedImage caption = element.getCaption() == null ? null : element.getCachedCaptionImage(this, g);
    if (icon != null || caption != null) {
      int width = 0;
      if (icon != null) {
        width = icon.getIconWidth() + 6;
      }
      if (caption != null) {
        width = Math.max(width, caption.getWidth() + 6);
      }
      if (r.width >= width * 2) {
        g.clip(frame);
        Rectangle2D.Float captionRect = new Rectangle2D.Float(r.x + r.width - width, r.y, width, r.height);
        Color bg = element.getCaptionBackground();
        g.setColor(bg == null ? getBackground() : bg);
        g.fill(captionRect);
        g.setColor(frameColor);
        g.draw(captionRect);
        Rectangle b = captionRect.getBounds();
        int y = b.y + 1;
        if (icon != null) {
          if (r.height >= icon.getIconHeight() + ROUNDING + 2)
            y += ROUNDING / 2;
          int iconWidth = icon.getIconWidth();
          int dx = (width - iconWidth) / 2;
          icon.paintIcon(this, g, b.x + dx, y);
          y += icon.getIconHeight() + 5;
        }
        if (caption != null) {
          int imageWidth = caption.getWidth();
          int dx = (width - imageWidth) / 2;
          g.drawImage(caption, b.x + dx + 1, y, this);
        }
      }
    }
  }

  private int getEndRow(DefaultTreeModel model, TreeNode subtreeRoot, int subtreeRootRow) {
    int count = subtreeRoot.getChildCount();
    if (count == 0) {
      if (subtreeRootRow != -1)
        return subtreeRootRow;
      TreeNode[] path = model.getPathToRoot(subtreeRoot);
      return getVisibleRowForPath(new TreePath(path));
    } else {
      return getEndRow(model, subtreeRoot.getChildAt(count - 1), -1);
    }
  }

  private boolean isCovering(int startRow, TreeNode subtreeRoot) {
    TreePath visiblePath = getPathForRow(startRow);
    assert visiblePath != null;
    boolean covering = visiblePath.getLastPathComponent() != subtreeRoot;
    return covering;
  }

  private int getStartRow(DefaultTreeModel model, TreeNode subtreeRoot) {
    TreeNode[] nodes = model.getPathToRoot(subtreeRoot);
    TreePath path = new TreePath(nodes);
    int startRow = getVisibleRowForPath(path);
    return startRow;
  }

  private int getVisibleRowForPath(TreePath path) {
    int result = getRowForPath(path);
    if (result == -1) {
      TreePath parentPath = path.getParentPath();
      if (parentPath != null) {
        result = getVisibleRowForPath(parentPath);
      }
    }
    return result;
  }


  private void paintOpaque(Graphics g) {
    g.setColor(getBackground());
    g.fillRect(0, 0, getWidth(), getHeight());
  }

  public void addHighlightedElement(HighlighterTreeElement highlighted) {
    myHighlighted.add(highlighted);
    repaint();
  }

  public void removeAllHighlighted() {
    if (!myHighlighted.isEmpty()) {
      myHighlighted.clear();
      repaint();
    }
  }

  public void setDndActive(boolean dndActive, boolean dndEnabled) {
    myInhibitSelectionChange = dndActive;
    if (myDndActive != dndActive) {
      myDndActive = dndActive;
      myDndEnabled = dndEnabled;
      if (!dndActive || !dndEnabled) {
        setDropHint(null);
      }
      JScrollPane scrollPane = SwingTreeUtil.findAncestorOfType(this, JScrollPane.class);
      if (scrollPane instanceof AScrollPane) {
        ((AScrollPane) scrollPane).setDndActive(dndActive && dndEnabled);
      }
    }
  }

  public void setSelectionPath(TreePath path) {
    // do not touch selection if dnd is in progress
    if (!myInhibitSelectionChange) {
      super.setSelectionPath(path);
    }
  }

  public void setDropHint(TreeDropHint hint) {
    if (!Util.equals(myTreeDropHint, hint)) {
      int repaintStartRow = Integer.MAX_VALUE;
      int repaintEndRow = 0;
      if (myTreeDropHint != null) {
        if (myTreeDropHint.isValid()) {
          TreeDropPoint dropPoint = myTreeDropHint.getDropPoint();
          if (dropPoint != null) {
            TreePath parentPath = ((ATreeNode) dropPoint.getNode()).getPathFromRoot();
            repaintStartRow = getRowForPath(parentPath);
            if (dropPoint.isInsertNode()) {
              repaintEndRow = myTreeDropHint.getHintRow();
            } else {
              repaintEndRow = repaintStartRow;
            }
          }
        }
      }
      myTreeDropHint = hint;
      myInsertTargetHintLifecycle.cycle();
      if (myTreeDropHint != null) {
        assert hint.isValid(this) : hint;
        TreeDropPoint dropPoint = myTreeDropHint.getDropPoint();
        if (dropPoint != null) {
          TreePath parentPath = ((ATreeNode) dropPoint.getNode()).getPathFromRoot();
          repaintStartRow = Math.min(repaintStartRow, getRowForPath(parentPath));
          if (dropPoint.isInsertNode()) {
            repaintEndRow = Math.max(repaintEndRow, myTreeDropHint.getHintRow());
          } else {
            repaintEndRow = Math.max(repaintEndRow, repaintStartRow);
          }
        }
        UIUtil.addTreeModelListener(myInsertTargetHintLifecycle.lifespan(), getModel(), myInsertHintModelListener);
      }
      repaintStartRow = Math.max(0, repaintStartRow - 1);
      repaintEndRow = Math.min(getRowCount() - 1, repaintEndRow + 1);
      if (repaintEndRow >= repaintStartRow) {
        Rectangle start = getRowBounds(repaintStartRow);
        Rectangle end = repaintEndRow == repaintStartRow ? start : getRowBounds(repaintEndRow);
        if (start == null || end == null) {
          assert false : this;
          repaint();
        } else {
          repaint(0, start.y, getWidth(), end.y + end.height - start.y);
        }
      }
    }
//    if (!Util.equals(myTreeDropHint, hint)) {
//      int repaintFromRow = Integer.MAX_VALUE;
//      if (myTreeDropHint != null) {
//        removeHeightDiff();
//        repaintFromRow = myTreeDropHint.isValid() ? myTreeDropHint.getHintRow() : 0;
//      }
//      myTreeDropHint = hint;
//      myInsertTargetHintLifecycle.cycle();
//      if (myTreeDropHint != null) {
//        assert hint.isValid(this) : hint;
//        if (myTreeDropHint.getDropPoint().isInsertNode()) {
//          addHeightDiff();
//        }
//        repaintFromRow = Math.min(repaintFromRow, myTreeDropHint.getHintRow());
//        UIUtil.addTreeModelListener(myInsertTargetHintLifecycle.lifespan(), getModel(), myInsertHintModelListener);
//      }
//      if (repaintFromRow < Integer.MAX_VALUE) {
//        int rowCount = getRowCount();
//        assert repaintFromRow >= 0 && repaintFromRow <= rowCount;
//        if (rowCount == 0) {
//          repaint();
//        } else {
//          if (repaintFromRow == rowCount) {
//            repaintFromRow--;
//          }
//          Rectangle bounds = getRowBounds(repaintFromRow);
//          if (bounds == null) {
//            assert false : this;
//            repaint();
//          } else {
//            repaint(0, bounds.y, getWidth(), getHeight() - bounds.y);
//          }
//        }
//      }
//    }
  }

  private void addHeightDiff() {
//    JViewport viewport = getViewport();
//    if (viewport != null) {
//      int height = TreeInsertDropHint.INSTANCE.getHeight(this);
//      myHeightDiff += height;
//      Dimension viewSize = viewport.getViewSize();
//      viewSize.height += height;
//      viewport.setViewSize(viewSize);
//    }
  }

  private void removeHeightDiff() {
    if (myHeightDiff > 0) {
      JViewport viewport = getViewport();
      if (viewport != null) {
        Dimension viewSize = viewport.getViewSize();
        viewSize.height -= myHeightDiff;
        viewport.setViewSize(viewSize);
      }
      myHeightDiff = 0;
    }
  }

  private JViewport getViewport() {
    Container parent = getParent();
    if (parent instanceof ATree) {
      Container grandParent = parent.getParent();
      if (grandParent instanceof JViewport) {
        return ((JViewport) grandParent);
      }
    }
    return null;
  }

  @Nullable
  public Rectangle getInsertTargetHintRect() {
//    TreeDropHint hint = myTreeDropHint;
//    if (hint == null || !myTreeDropHint.getDropPoint().isInsertNode())
//      return null;
//    int row = hint.getHintRow();
//    Rectangle bounds = getRowBounds(row);
//    if (bounds == null)
//      return null;
//    return new Rectangle(0, bounds.y, getWidth(), TreeInsertDropHint.INSTANCE.getHeight(this));
    return null;
  }

  public int getInsertTargetHintRow() {
    TreeDropHint hint = myTreeDropHint;
    return hint == null || !hint.getDropPoint().isInsertNode() ? -1 : hint.getHintRow();
  }

  public Dimension getPreferredSize() {
    Dimension pref = super.getPreferredSize();
    pref.height += myHeightDiff;
    return pref;
  }


  public Dimension getPreferredScrollableViewportSize() {
    if (myScrollSizeIsPref) return getPreferredSize();
    Dimension pref = super.getPreferredScrollableViewportSize();
    pref.height += myHeightDiff;
    return pref;
  }

  public void setScrollSizeIsPref(boolean scrollSizeIsPref) {
    myScrollSizeIsPref = scrollSizeIsPref;
  }

  public TreePath getClosestPathForLocation(int x, int y) {
//    TreeDropHint hint = myTreeDropHint;
//    if (hint != null && hint.getDropPoint().isInsertNode()) {
//      int height = TreeInsertDropHint.INSTANCE.getHeight(this);
//      Rectangle bounds = getRowBounds(hint.getHintRow());
//      if (bounds != null) {
//        if (y >= bounds.y + height) {
//          y -= height;
//        }
//      }
//    }
    return super.getClosestPathForLocation(x, y);
  }

  public boolean isDndWorking() {
    return myDndActive && myDndEnabled;
  }

  private class DropHintInvalidator extends TreeModelAdapter {
    public void treeNodesInserted(TreeModelEvent e) {
      invalidateHint();
    }

    public void treeNodesRemoved(TreeModelEvent e) {
      invalidateHint();
    }

    public void treeStructureChanged(TreeModelEvent e) {
      invalidateHint();
    }

    private void invalidateHint() {
      TreeDropHint hint = myTreeDropHint;
      if (hint != null) {
        hint.setValid(false);
        setDropHint(null);
      }
    }
  }
}
