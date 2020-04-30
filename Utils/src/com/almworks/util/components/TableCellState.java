package com.almworks.util.components;

import com.almworks.util.Env;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.ComponentCellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Map;

public class TableCellState extends ComponentCellState {
  private static final boolean IS_MAC = Aqua.isAqua();

  private static final EmptyBorder EMPTY_BORDER = new EmptyBorder(1, 1, 1, 1);
  private static final Border FOCUS_CELL_BORDER = IS_MAC
    ? EMPTY_BORDER : UIManager.getBorder("Table.focusCellHighlightBorder");
  private static final Border FOCUS_ROW_BORDER = IS_MAC
    ? EMPTY_BORDER : new RowBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));

  private final boolean mySelected;
  private final boolean myHasFocus;
  private final int myRow;
  private final int myColumn;
  private final boolean myRowHasFocus;
  private final Map<Integer, Border> myMacSelectedBorders = IS_MAC ? Collections15.<Integer, Border>hashMap() : null;

  public TableCellState(JTable table, boolean selected, boolean hasFocus, int row, int column) {
    super(table);
    mySelected = selected;
    myHasFocus = hasFocus;
    myRow = row;
    myColumn = column;
    myRowHasFocus = isRowFocus(table, row);
  }

  private static boolean isRowFocus(JTable table, int row) {
    ListSelectionModel selectionModel = table.getSelectionModel();
    if (selectionModel == null)
      return false;
    int index = selectionModel.getLeadSelectionIndex();
    if (index < 0)
      return false;
    return index == row && isTableActive(table, row);
  }

  protected JTable getComponent() {
    return (JTable) super.getComponent();
  }

  @Override
  public Color getBackground() {
    return getBackground(getComponent().isOpaque());
  }

  public Color getBackground(boolean opaque) {
    JTable table = getComponent();
    if (!Env.isWindows() && !Env.isMacLeopardOrNewer() && !Env.isLinux() && !opaque)
      return null;
//    if (isFocused()) {
//      return UIManager.getColor("Table.focusCellBackground");
//    }
    if (mySelected) {
      if (isTableActive(table, myRow)) {
        return table.getSelectionBackground();
      } else {
        return getInactiveSelectionBackground(table);
      }
    } else {
      return getDefaultBackground();
    }
  }

  private boolean isStripe() {
    final JTable c = getComponent();
    if(!(c instanceof JTableAdapter)) {
      return false;
    }

    final JTableAdapter ta = (JTableAdapter)c;
    if(!ta.isStriped()) {
      return false;
    }

    return (myRow & 1) == 1;
  }

  @NotNull
  @Override
  public Color getDefaultBackground() {
    return isStripe() ? ((JTableAdapter)getComponent()).getStripeBackground() : super.getDefaultBackground();
  }

  private static boolean isTableActive(JTable table, int row) {
    return ListSpeedSearch.isFocusOwner(table) || table.getEditingRow() == row;
  }

  public Color getSelectionBackground() {
    return getComponent().getSelectionBackground();
  }

  private Color getInactiveSelectionBackground(JTable table) {
//      Color c = UIManager.getColor("TableHeader.background");
    Color c = ColorUtil.between(table.getSelectionBackground(), table.getBackground(), 0.65F);
    return c != null ? c : table.getSelectionBackground();
  }

  public Color getForeground() {
    if (isFocused()) {
      return UIManager.getColor("Table.focusCellForeground");
    }
    if (!mySelected) {
      return getDefaultForeground();
    } else {
      JTable table = getComponent();
      if (isTableActive(table, myRow)) {
        return table.getSelectionForeground();
      } else {
        return table.getForeground();
//          return getInactiveSelectionForeground(table);
      }
    }
  }

  public Border getBorder() {
    return IS_MAC
      ? (mySelected ? getMacSelectedCellBorder() : EMPTY_BORDER)
      : (myRowHasFocus ? FOCUS_ROW_BORDER : EMPTY_BORDER);
  }

  private Border getMacSelectedCellBorder() {
    if(!IS_MAC || !mySelected) {
      assert false;
      return EMPTY_BORDER;
    }

    final Color color = getDefaultBackground();
    final Integer rgb = color.getRGB();
    Border border = myMacSelectedBorders.get(rgb);

    if(border == null) {
      border = new RowBorder(UIUtil.getCompoundBorder(
        new BrokenLineBorder(color, 1, BrokenLineBorder.SOUTH),
        new EmptyBorder(1, 1, 0, 1)));
      myMacSelectedBorders.put(rgb, border);
    }

    return border;
  }

  public Border getDefaultBorder() {
    return myHasFocus ? FOCUS_CELL_BORDER : EMPTY_BORDER;
  }

  public boolean isExpanded() {
    return false;
  }

  public boolean isLeaf() {
    return true;
  }

  public boolean isSelected() {
    return mySelected;
  }

  public boolean isFocused() {
    return myHasFocus && getComponent().isCellEditable(myRow, myColumn);
  }

  public int getCellColumn() {
    return myColumn;
  }

  public int getCellRow() {
    return myRow;
  }

  public int getComponentCellWidth() {
    TableColumnModel columnModel = getComponent().getColumnModel();
    return columnModel.getColumn(myColumn).getWidth() - columnModel.getColumnMargin();
  }

  public int getComponentCellHeight() {
    JTable table = getComponent();
    return table.getRowHeight(myRow) - table.getRowMargin();
  }
}
