package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public interface FlatCollectionComponent <T> extends ACollectionComponent<T> {
  public ListSelectionModel getSelectionModel();

  public AListModel<? extends T> getCollectionModel();

  public Detach setCollectionModel(AListModel<? extends T> model);

  int getElementIndexAt(int x, int y);

  @Nullable
  T getElementAt(Point point);

  int getScrollingElementAt(int x, int y);
}
