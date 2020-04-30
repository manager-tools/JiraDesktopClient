package com.almworks.util.ui.widgets.impl;

import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.Widget;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

class CellContextImpl implements CellContext {
  private final HostComponentState<?> myState;
  private HostCellImpl myCell = null;
  private CellContextImpl myTmpChildContext = null;

  CellContextImpl(HostComponentState<?> state) {
    if (state == null) throw new NullPointerException();
    myState = state;
  }

  @Nullable
  protected final HostCellImpl getCurrentCell() {
    return myCell;
  }

  protected final void setCurrentCell(HostCellImpl cell) {
    myCell = cell;
  }

  @Nullable
  @Override
  public final HostCellImpl getActiveCell() {
    if (myCell == null) return null;
    return myCell.isActive() ? myCell : null;
  }

  public boolean isActive() {
    return myCell.isActive();
  }

  @Nullable
  @Override
  public <T> T getStateValue(TypedKey<? extends T> key) {
    return myCell != null ? myCell.getStateValue(key) : null;
  }

  @Override
  public <T> T getStateValue(TypedKey<? extends T> key, T nullValue) {
    return myCell != null ? myCell.getStateValue(key, nullValue) : nullValue;
  }

  @Override
  public <T> void putStateValue(TypedKey<? super T> key, T value, boolean permanent) {
    if (myCell == null) return;
    myCell.putStateValue(key, value, permanent);
  }

  @Nullable
  @Override
  public JComponent getLiveComponent() {
    HostCellImpl cell = getActiveCell();
    return cell != null ? cell.getLiveComponent() : null;
  }

  @Override
  public HostComponentState getHost() {
    return myState;
  }

  @Override
  public <T> int getChildPreferedWidth(int id, Widget<? super T> child, T value) {
    HostCellImpl cell = getActiveCell();
    if (cell != null) {
      HostCellImpl childCell = cell.findChild(id);
      if (childCell != null) return child.getPreferedWidth(childCell, value);
    }
    CellContextImpl childContext = getTmpChildContext();
    int width = child.getPreferedWidth(childContext, value);
    releaseTmpChildContext(childContext);
    return width;
  }

  @Override
  public <T> int getChildPreferedHeight(int id, Widget<? super T> child, T value, int width) {
    HostCellImpl cell = getActiveCell();
    if (cell != null) {
      HostCellImpl childCell = cell.findChild(id);
      if (childCell != null) return child.getPreferedHeight(childCell, width, value);
    }
    CellContextImpl childContext = getTmpChildContext();
    int height = child.getPreferedHeight(childContext, width, value);
    releaseTmpChildContext(childContext);
    return height;
  }

  @Override
  public int getHostX() {
    HostCellImpl cell = getActiveCell();
    return cell != null ? cell.getHostX() : -1;
  }

  @Override
  public int getHostY() {
    HostCellImpl cell = getActiveCell();
    return cell != null ? cell.getHostY() : -1;
  }

  @Override
  public int getWidth() {
    HostCellImpl cell = getCurrentCell();
    return cell != null ? cell.getWidth() : 0;
  }

  @Override
  public int getHeight() {
    HostCellImpl cell = getCurrentCell();
    return cell != null ? cell.getHeight() : 0;
  }

  @Override
  public void repaint() {
    HostCellImpl cell = getActiveCell();
    if (cell != null) cell.repaint();
  }

  @Override
  public void requestFocus() {
    HostCell cell = getActiveCell();
    if (cell != null) cell.requestFocus();
  }

  @NotNull
  @Override
  public Rectangle getHostBounds(@Nullable Rectangle target) {
    HostCellImpl cell = getActiveCell();
    if (cell != null) return cell.getHostBounds(target);
    else {
      if (target == null) target = new Rectangle();
      target.setBounds(-1, -1, -1, -1);
      return target;
    }
  }

  @Override
  public void invalidate() {
    HostCellImpl cell = getActiveCell();
    if (cell != null) cell.invalidate();
  }

  @Override
  public void deleteAll() {
    HostCellImpl cell = getActiveCell();
    if (cell != null) cell.deleteAll();
  }

  @Override
  public boolean isFocused() {
    HostCellImpl cell = getActiveCell();
    return cell != null && cell.isFocused();
  }

  @Override
  public void removeLiveComponent() {
    HostCellImpl cell = getActiveCell();
    if (cell != null) cell.setLiveComponent(null);
  }

  @Override
  public <T> void postEvent(TypedKey<T> reason, T data) {
    HostCellImpl cell = getActiveCell();
    if (cell != null) cell.postEvent(reason, data);
  }

  @Nullable
  @Override
  public HostCell getFirstChild(boolean fromBeginning) {
    HostCellImpl cell = getActiveCell();
    return cell != null ? cell.getFirstChild(fromBeginning) : null;
  }

  @Nullable
  @Override
  public HostCell findChild(int id) {
    HostCellImpl cell = getActiveCell();
    return cell != null ? cell.findChild(id) : null;
  }

  private CellContextImpl getTmpChildContext() {
    if (myTmpChildContext != null) {
      CellContextImpl context = myTmpChildContext;
      myTmpChildContext = null;
      return context;
    }
    return new CellContextImpl(getHost());
  }

  private void releaseTmpChildContext(CellContextImpl context) {
    if (myTmpChildContext == null) myTmpChildContext = context;
  }
}
