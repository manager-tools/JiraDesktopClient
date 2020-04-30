package com.almworks.explorer.tree;

import com.almworks.util.components.Canvas;
import com.almworks.util.components.EditableText;
import com.almworks.util.components.renderer.CellState;

import javax.swing.*;

/**
 * A presentation that switches a node's icon depending on
 * whether or not it hides empty child nodes.
 */
class HidingPresentation extends EditableText {
  private Icon myShowingIcon;
  private Icon myHidingIcon;
  private GenericNodeImpl myNode;

  public HidingPresentation(String name, Icon showingIcon, Icon hidingIcon, GenericNodeImpl node) {
    super(name, showingIcon);
    myShowingIcon = showingIcon;
    myHidingIcon = hidingIcon;
    myNode = node;
  }

  @Override
  public void renderOn(Canvas canvas, CellState state) {
    if(myNode.isHidingEmptyChildren()) {
      setIcon(myHidingIcon);
    } else {
      setIcon(myShowingIcon);
    }
    super.renderOn(canvas, state);
  }
}
