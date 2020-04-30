package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ListModelHolder;
import com.almworks.util.components.renderer.ListCanvasWrapper;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class AList<T> extends BaseAList<T> {
  private final ListCanvasWrapper<T> myCanvasWrapper = new ListCanvasWrapper<T>(true);

  public AList() {
    this(new ListModelHolder<T>());
  }

  public AList(@NotNull AListModel<? extends T> model) {
    this(new ListModelHolder<T>(model));
  }

  public AList(ListModelHolder<T> listModelHolder) {
    super(listModelHolder);
    myCanvasWrapper.setLafRenderer(getSwingListRenderer());
    getScrollable().setCellRenderer(myCanvasWrapper);
  }

  public void updateUI() {
    boolean tmpRemove = myCanvasWrapper.beforeUpdateUI(getScrollable());
    getScrollable().updateUI();
    super.updateUI();
    myCanvasWrapper.afterUpdateUI(getScrollable(), tmpRemove);
  }

  public static <T> AList<T> create() {
    return new AList<T>();
  }

  public AListModel<T> getCollectionModel() {
    return getModelHolder();
  }

  public Detach setCollectionModel(AListModel<? extends T> model) {
    return setCollectionModel(model, false);
  }

  public void setCanvasRenderer(CanvasRenderer<? super T> renderer) {
    boolean changed = getScrollable().getCellRenderer() == myCanvasWrapper;
    getScrollable().setCellRenderer(myCanvasWrapper);
    changed |= myCanvasWrapper.setCanvasRenderer(renderer);
    if (changed && getCollectionModel().getSize() > 0) {
      getScrollable().fireRendererChanged();
      invalidate();
      revalidate();
      repaint();
    }
  }

  @Nullable
  public CanvasRenderer<? super T> getCanvasRenderer() {
    ListCellRenderer renderer = getScrollable().getCellRenderer();
    return renderer != myCanvasWrapper ? null : myCanvasWrapper.getCanvasRenderer();
  }
}