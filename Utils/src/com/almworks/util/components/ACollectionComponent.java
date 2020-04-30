package com.almworks.util.components;

import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public interface ACollectionComponent<T> {
  @NotNull
  SelectionAccessor<T> getSelectionAccessor();

  JComponent toComponent();

  JComponent getSwingComponent();

  void scrollSelectionToView();

  void setDataRoles(DataRole ... roles);

  void setTransfer(ContextTransfer transfer);

  void addGlobalRoles(DataRole<?>... roles);

  @Nullable
  Rectangle getElementRect(int elementIndex);
}
