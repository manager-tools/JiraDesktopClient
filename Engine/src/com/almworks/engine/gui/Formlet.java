package com.almworks.engine.gui;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.ui.actions.ToolbarEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Formlet {
  Modifiable getModifiable();

  @Nullable
  String getCaption();

  boolean isCollapsible();

  boolean isCollapsed();

  boolean isVisible();

  @NotNull
  WidthDrivenComponent getContent();

  @Nullable
  List<? extends ToolbarEntry> getActions();

  void toggleExpand();

  void expand();
}
