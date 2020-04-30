package com.almworks.explorer.tree;

import com.almworks.util.Env;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.components.EditableText;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ColorUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

abstract class CounterPresentation extends EditableText {
  private static final boolean SHOW_UNSYNCED_COUNTERS = Env.getBoolean("show.unsync.counters", false);
  protected static final Color COUNT_COLOR = UIManager.getColor("Link.foreground");
  protected static final EmptyBorder COUNT_BORDER = new EmptyBorder(0, 4, 0, 0);

  protected int myCounter = -1;
  private boolean myCounterValid = false;
  protected GenericNodeImpl myNode = null;

  public CounterPresentation(String name, Icon openIcon, Icon closedIcon) {
    super(name, openIcon, closedIcon);
  }

  public void renderOn(Canvas canvas, CellState state) {
    super.renderOn(canvas, state);
    CanvasSection nameSection = canvas.getCurrentSection();
    boolean synced = isSynced();
    boolean counterValid = myCounterValid;
    int counter = myCounter;
    if (counter < 0)
      return;
    if (!(synced || SHOW_UNSYNCED_COUNTERS))
      return;
//    if (!counterValid && counter == 0)
//      return;
    CanvasSection countSection = canvas.newSection();
    countSection.setFontStyle(Font.PLAIN);
    countSection.setBackground(state.getDefaultBackground());
    if (counter == 0 && synced) {
      Color grey = getGrey(state);
      if (!state.isSelected()) {
        canvas.setForeground(grey);
        nameSection.setForeground(grey);
      }
      countSection.setForeground(grey);
    } else {
//      countSection.setForeground(synced && clean ? COUNT_COLOR : getGrey(state));
      countSection.setForeground(COUNT_COLOR);
    }
    countSection.appendText("(");
/*
        if (!clean)
          countSection.appendText("~");
*/
    countSection.appendInt(counter);
    if (!synced)
      countSection.appendText("?");
    countSection.appendText(")");
    countSection.setBorder(COUNT_BORDER);
  }

  static Color getGrey(CellState state) {
    return ColorUtil.between(state.getDefaultBackground(), state.getDefaultForeground(), 0.3F);
  }

  protected abstract boolean isSynced();

  public void setNode(GenericNodeImpl node) {
    if (myNode != null) {
      assert myNode == node : myNode + " " + node;
    }
    myNode = node;
  }

  protected void fireNodeChanged() {
    if (myNode != null)
      myNode.fireTreeNodeChanged();
  }

  public boolean setText(String text) {
    Threads.assertAWTThread();
    boolean changed = super.setText(text);
    if (changed)
      fireNodeChanged();
    return changed;
  }

  public void setValidCounter(int counter) {
    Threads.assertAWTThread();
    boolean changed = !myCounterValid || myCounter != counter;
    if (changed) {
      myCounter = counter;
      myCounterValid = true;
      fireNodeChanged();
    }
  }

  public void setCounterNotAvailable() {
    setValidCounter(-1);
  }

  public void invalidateCounter() {
    Threads.assertAWTThread();
    if (myCounterValid) {
      myCounterValid = false;
      fireNodeChanged();
    }
  }
}
