package com.almworks.api.application.qb;

import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NamedStubConstraintEditor implements ConstraintEditor {
  private final String myDescription;
  private final String myName;

  protected NamedStubConstraintEditor(String name, String description) {
    myName = name;
    myDescription = description;
  }

  public void renderOn(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
    canvas.setIcon(Icons.QUERY_CONDITION_GENERIC);
    canvas.appendText(myName);
  }

  public boolean isModified() {
    return false;
  }

  @NotNull
  public FilterNode createFilterNode(ConstraintDescriptor descriptor) {
    assert false;
    return FilterNode.ALL_ITEMS;
  }

  public void onComponentDisplayble() {
  }

  public JComponent getComponent() {
    return UIUtil.createMessage(myDescription);
  }

  public void dispose() {
  }
}
