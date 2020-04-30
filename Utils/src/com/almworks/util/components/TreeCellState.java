package com.almworks.util.components;

import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.ComponentCellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.ui.ColorUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TreeCellState extends ComponentCellState {
  private static final boolean IS_MAC = Aqua.isAqua();
  private static final EmptyBorder EMPTY_BORDER = new EmptyBorder(1, 1, 1, 1);
  private static final Border MAC_SELECTED_BORDER = IS_MAC
    ? BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Tree.textBackground")),
        new EmptyBorder(1, 1, 0, 1))
    : null;

  private final boolean mySelected;
  private final boolean myHasFocus;
  private final boolean myExpanded;
  private final boolean myLeaf;
  private final int myRow;

  public TreeCellState(JTree tree, boolean selected, boolean hasFocus, boolean expanded, boolean leaf, int row) {
    super(tree);
    mySelected = selected;
    myHasFocus = hasFocus;
    myExpanded = expanded;
    myLeaf = leaf;
    myRow = row;
  }

  protected JTree getComponent() {
    return (JTree) super.getComponent();
  }

  @Override
  public Color getBackground() {
    return getBackground(true);
  }

  public Color getBackground(boolean opaque) {
    if (!mySelected) {
      return UIManager.getColor("Tree.textBackground");
    } else {
      if (shouldHaveSelectedColors()) {
        return UIManager.getColor("Tree.selectionBackground");
      } else {
//          Color c = UIManager.getColor("TableHeader.background"); -- that's how it's done in Outlook
        Color c = ColorUtil.between(UIManager.getColor("Tree.selectionBackground"), UIManager.getColor("Tree.textBackground"), 0.65F);
        return c != null ? c : UIManager.getColor("Tree.selectionBackground");
      }
    }
  }

  public Color getSelectionBackground() {
    return UIManager.getColor("Tree.selectionBackground");
  }

  private boolean shouldHaveSelectedColors() {
    return ListSpeedSearch.isFocusOwner(getComponent());
  }

  @NotNull
  public Color getForeground() {
    if (!mySelected) {
      return UIManager.getColor("Tree.textForeground");
    } else {
      if (shouldHaveSelectedColors()) {
        return UIManager.getColor("Tree.selectionForeground");
      } else {
        return UIManager.getColor("Tree.textForeground");
//          Color c = UIManager.getColor("TableHeader.foreground");
//          return c != null ? c : UIManager.getColor("Tree.selectionForeground");
      }
    }
  }

  public Border getBorder() {
    return IS_MAC
      ? (mySelected ? MAC_SELECTED_BORDER : EMPTY_BORDER)
      : (myHasFocus ? UIManager.getBorder("List.focusCellHighlightBorder") : EMPTY_BORDER);
  }

  public boolean isExpanded() {
    return myExpanded;
  }

  public boolean isLeaf() {
    return myLeaf;
  }

  public boolean isSelected() {
    return mySelected;
  }

  public boolean isFocused() {
    return myHasFocus;
  }

  public int getCellColumn() {
    return 0;
  }

  public int getCellRow() {
    return myRow;
  }

  public int getComponentCellWidth() {
    return 0;
  }

  public int getComponentCellHeight() {
    JTree tree = getComponent();
    if (tree.isFixedRowHeight())
      return tree.getRowHeight();
    return tree.getPathBounds(getComponent().getPathForRow(myRow)).height;
  }
}
