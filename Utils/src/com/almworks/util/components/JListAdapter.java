package com.almworks.util.components;

import com.almworks.util.components.plaf.macosx.MacKeyboardSelectionPatch;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.model.LightScalarModel;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.ListDataAdapter;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.dnd.DndComponentAdapter;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class JListAdapter extends JList implements DndComponentAdapter<ListDropHint> {
  private final Lifecycle myInsertTargetHintLifecycle = new Lifecycle();
  private final ListDataListener myHintInvalidator = new DropHintInvalidator();
  private boolean myDndActive;
  private boolean myDndEnabled;
  private final LightScalarModel<Boolean> myInhibitSelectionChange = LightScalarModel.create(false);
  private ListDropHint myDropHint;

  private Color myInactiveSelectionForeground;
  private Color myInactiveSelectionBackground;
  private FocusListener myFocusListener;

  private boolean myGetNextMatchOverridden = false;

  public JListAdapter(ListModel dataModel) {
    super(dataModel);
    setupSelectionModel();
  }

  private void setupSelectionModel() {
    try {
      setSelectionModel(new PatchedListSelectionModel(myInhibitSelectionChange));
    } catch(CantPerformException e) {
      Log.warn("PLSM installation failed: " + e.getCause().getClass().getSimpleName());
      setSelectionModel(new VetoableListSelectionModel(myInhibitSelectionChange));
    }
  }

  public void setDndActive(boolean dndActive, boolean dndEnabled) {
    myInhibitSelectionChange.setValue(dndActive);
    if (myDndActive != dndActive) {
      myDndActive = dndActive;
      myDndEnabled = dndEnabled;
      if (!dndActive || !dndEnabled) {
        setDropHint(null);
      }
      JScrollPane scrollPane = SwingTreeUtil.findAncestorOfType(this, JScrollPane.class);
      if (scrollPane instanceof AScrollPane) {
        ((AScrollPane) scrollPane).setDndActive(dndActive && dndEnabled);
      }
    }
  }

  public boolean isDndWorking() {
    return myDndActive && myDndEnabled;
  }

  public void setDropHint(ListDropHint hint) {
    if (!Util.equals(myDropHint, hint)) {
      repaintAfterDropHint(myDropHint);
      myDropHint = hint;
      myInsertTargetHintLifecycle.cycle();
      if (myDropHint != null) {
        assert hint.isValid() : hint;
        repaintAfterDropHint(myDropHint);
        UIUtil.addListDataListener(myInsertTargetHintLifecycle.lifespan(), getModel(), myHintInvalidator);
      }
    }
  }

  /**
   * To turn off Swing speedsearch
   *
   * -- thanks, it's not working anymore :(
   *
   * -- hmm, looks like it *does* work... (pzvyagin 2009-08-31)
   */
  public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
    if(myGetNextMatchOverridden) {
      return -1;
    }
    return super.getNextMatch(prefix, startIndex, bias);
  }

  /**
   * If passed {@code true}, disables Swing's default JList keyboard
   * navigation for this list. Useful if there's another facility
   * installed for performing speed search or keyboard navigation.
   * @param b {@code true} to disable default keyboard navigation.
   */
  public void setGetNextMatchOverridden(boolean b) {
    myGetNextMatchOverridden = b;
  }

  public void fireRendererChanged() {
    firePropertyChange("cellRenderer", null, getCellRenderer());
  }

  private void repaintAfterDropHint(ListDropHint dh) {
    if (dh != null) {
      if (dh.isValid()) {
        int row = dh.getDropPoint().getTargetRow();
        int count = getModel().getSize();
        if (row >= 0 && row <= count) {
          if (row < count) {
            Rectangle r = getCellBounds(row, row);
            if (r != null) {
              repaint(r);
            }
          }
          if (row > 0) {
            Rectangle r = getCellBounds(row - 1, row - 1);
            if (r != null) {
              repaint(r);
            }
          }
        }
      }
    }
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    super.paintComponent(g);
    ListDropHint dropHint = myDropHint;
    if (dropHint != null) {
      paintDropHint(g, dropHint);
    }
  }

  private void paintDropHint(Graphics g, ListDropHint hint) {
    if (getLayoutOrientation() != VERTICAL) {
      // todo
      return;
    }
    int row = hint.getDropPoint().getTargetRow();
    assert hint.getDropPoint().isInsert();
    int count = getModel().getSize();
    if (row < 0 || row > count)
      return;
    int y;
    if (row == 0) {
      y = 0;
    } else {
      Rectangle bounds = getCellBounds(row - 1, row - 1);
      if (bounds == null)
        return;
      y = bounds.y + bounds.height - 2; 
    }
    Rectangle r = new Rectangle(0, y, getWidth(), 2);
    if (!g.getClipBounds().intersects(r))
      return;

    Graphics2D gg = (Graphics2D) g.create();
    try {
      Color c = GlobalColors.DRAG_AND_DROP_COLOR;
      gg.setColor(c);
      gg.fillRect(r.x, r.y, r.width, r.height);
    } finally {
      gg.dispose();
    }

  }

  public Color getSelectionForeground() {
    if (ListSpeedSearch.isFocusOwner(this)) {
      return super.getSelectionForeground();
    } else {
      return getInactiveSelectionForeground();
    }
  }

  public Color getSelectionBackground() {
    if (ListSpeedSearch.isFocusOwner(this)) {
      return super.getSelectionBackground();
    } else {
      return getInactiveSelectionBackground();
    }
  }

  private Color getInactiveSelectionForeground() {
    if (myInactiveSelectionForeground == null) {
      myInactiveSelectionForeground = super.getForeground();
    }
    return myInactiveSelectionForeground;
  }

  private Color getInactiveSelectionBackground() {
    if (myInactiveSelectionBackground == null) {
      Color sbg = super.getSelectionBackground();
      Color bg = super.getBackground();
      if (sbg == null || bg == null)
        return sbg;
      myInactiveSelectionBackground = ColorUtil.between(sbg, bg, 0.65F);
    }
    return myInactiveSelectionBackground;
  }

  private FocusListener getFocusListener() {
    if (myFocusListener == null) {
      myFocusListener = new FocusListener() {
        public void focusGained(FocusEvent e) {
          repaintSelection();
        }

        public void focusLost(FocusEvent e) {
          repaintSelection();
        }
      };
    }
    return myFocusListener;
  }

  private void repaintSelection() {
    ListSelectionModel model = getSelectionModel();
    int min = model.getMinSelectionIndex();
    if (min < 0)
      return;
    int max = model.getMaxSelectionIndex();
    if (max < min)
      return;
    Dimension size = getSize();
    for (int i = min; i <= Math.min(max, getModel().getSize() - 1); i++) {
      if (model.isSelectedIndex(i)) {
        Rectangle rect = getCellBounds(i, i);
        if (rect != null) {
          rect.x = 0;
          rect.width = size.width;
          repaint(rect);
        }
      }
    }
  }

  public void updateUI() {
    FocusListener focusListener = getFocusListener();
    removeFocusListener(focusListener);
    super.updateUI();
    addFocusListener(focusListener);
    MacKeyboardSelectionPatch.install(this);
  }

  private class DropHintInvalidator extends ListDataAdapter {
    protected void listModelChanged(ListDataEvent e) {
      ListDropHint hint = myDropHint;
      if (hint != null) {
        hint.setValid(false);
        setDropHint(null);
      }
    }
  }
}
