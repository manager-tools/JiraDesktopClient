package com.almworks.util.components.renderer;

import com.almworks.util.components.BackgroundCanvasRenderer;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ListCanvasWrapper<T> implements ListCellRenderer {
  private static final boolean IS_MAC = Aqua.isAqua();
  private final boolean myFixSpeedSearch;

  @Nullable
  private ListCellRenderer myLafRenderer = null;
  private final BackgroundCanvasRenderer myRenderer;

  /**
   * @param fixSpeedSearch set true if list speed search can be installed for list component. This flag is performance workaround for combo boxes. Combo boxes cannot have list speed search
   *                       but they works faster without checks for installed speed search.
   */
  public ListCanvasWrapper(boolean fixSpeedSearch) {
    this(new BackgroundCanvasRenderer(), fixSpeedSearch);
  }

  protected ListCanvasWrapper(BackgroundCanvasRenderer renderer, boolean fixSpeedSearch) {
    myRenderer = renderer;
    myFixSpeedSearch = fixSpeedSearch;
  }

  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    return prepareRendererComponent(list, value, index, isSelected, cellHasFocus, IS_MAC || index < 0);
  }

  protected Component prepareRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus,
    boolean useBgRenderer)
  {
    if (myFixSpeedSearch) cellHasFocus = ListSpeedSearch.fixFocusedState(list, cellHasFocus, index, 0);
    final boolean focus = cellHasFocus && !IS_MAC;
    if (myLafRenderer != null && useBgRenderer) {
      @SuppressWarnings({"ConstantConditions"})
      final Component c = myLafRenderer.getListCellRendererComponent(list, "", index, isSelected, focus);
      myRenderer.setBackgroundComponent(c);
    } else {
      myRenderer.setBackgroundComponent(null);
    }
    myRenderer.prepareCanvas(new ListCellState(list, isSelected, focus, index), value);
    return myRenderer;
  }

  public void setLafRenderer(ListCellRenderer renderer) {
    myLafRenderer = renderer;
  }

  public boolean setCanvasRenderer(CanvasRenderer<? super T> canvasRenderer) {
    return myRenderer.setRenderer(canvasRenderer);
  }

  @Nullable
  public CanvasRenderer<? super T> getCanvasRenderer() {
    return myRenderer.getRenderer();
  }

  public boolean beforeUpdateUI(JList list) {
    if (list.getCellRenderer() == list) {
      list.setCellRenderer(null);
      return true;
    }
    return false;
  }

  public void afterUpdateUI(JList list, boolean tmpRemove) {
    if (!tmpRemove)
      return;
    setLafRenderer(list.getCellRenderer());
    list.setCellRenderer(this);
  }

  public BackgroundCanvasRenderer getRenderer() {
    return myRenderer;
  }
}

