package com.almworks.util.components.speedsearch;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.ATable;
import com.almworks.util.components.ATree;
import com.almworks.util.components.BaseAList;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

class SpeedSearchPopup implements FocusListener, KeyListener, ChangeListener {
  private static final UIUtil.Positioner POPUP_POSITIONER = Aqua.isAqua()
    ? new UIUtil.IndependentPositioner() {
        @Override
        public int getX(int screenX, int screenW, int ownerX, int ownerW, int childW) {
          return ownerX + 1;
        }
        @Override
        public int getY(int screenY, int screenH, int ownerY, int ownerH, int childH) {
          return ownerY - childH - 2;
        }
      }
    : new UIUtil.IndependentPositioner(UIUtil.ALIGN_START, UIUtil.BEFORE);

  private static final int DW = Aqua.isAqua() ? -2 : 0;

  private final JTextField myField = new JTextField();
  private final Controller myController;
  private final Color myNormalForeground;
  private Popup myPopup;
  private JComponent myReturnFocus;
  private final DetachComposite myLife = new DetachComposite();
  private boolean mySearching;

  private SpeedSearchPopup(Controller controller) {
    myController = controller;
    myNormalForeground = myField.getForeground();
    myField.setFocusTraversalKeysEnabled(false);
    Aqua.makeSearchField(myField);
  }

  public static SpeedSearchPopup open(JComponent swingComponent, Controller controller) {
    final SpeedSearchPopup search = new SpeedSearchPopup(controller);

    final JTextField field = search.myField;
    if(!Aqua.isAqua()) {
      field.setBackground(UIUtil.getNoticeBackground());
    }

    final JComponent visualParent = getVisualParent(swingComponent);
    final Dimension fieldSize = new Dimension(visualParent.getWidth() + DW, field.getPreferredSize().height);
    field.setPreferredSize(fieldSize);

    final Point location = UIUtil.calcPopupPosition(visualParent, POPUP_POSITIONER, fieldSize);
    search.myPopup = UIUtil.getPopup(swingComponent, field, true, location);

    UIUtil.addTextListener(Lifespan.FOREVER, field, search);
    field.addFocusListener(search);
    field.addKeyListener(search);
    search.myReturnFocus = swingComponent;

    search.myPopup.show();
    field.requestFocus();

    return search;
  }

  private static JComponent getVisualParent(JComponent component) {
    if(component instanceof JScrollPane) {
      return component;
    }

    Container parent = component.getParent();
    if(parent instanceof BaseAList || parent instanceof ATable || parent instanceof ATree) {
      parent = parent.getParent();
    }

    if(parent instanceof JViewport) {
      final Container granny = ((JViewport)parent).getParent();
      if(granny instanceof JScrollPane) {
        return (JComponent)granny;
      }
    }

    return component;
  }

  public void focusGained(FocusEvent e) {
    if(Aqua.isAqua()) {
      final int length = myField.getText().length();
      myField.select(length, length);
    }
  }

  public void focusLost(FocusEvent e) {
    Component c = e.getOppositeComponent();
    if(c == null || !SwingTreeUtil.isAncestor(myField, c)) hidePopup(false);
  }

  public void hidePopup(boolean returnFocus) {
    myLife.detach();
    myPopup.hide();
    myController.speedSearchClosed();
    if (returnFocus) {
      myReturnFocus.requestFocus();
    } else {
      myReturnFocus.repaint();
      myReturnFocus.requestFocusInWindow();
    }
  }

  public void addLetter(char letter) {
    myField.setText(myField.getText() + letter);
  }

  public Lifespan getLife() {
    return myLife;
  }

  public void keyTyped(KeyEvent e) {

  }

  public void keyPressed(KeyEvent e) {
    if (AwtUtil.traverseFocus(myReturnFocus, e)) {
      myReturnFocus.repaint();
      return;
    }
    if (myController.maybeStopSearch(this, e)) {
      hidePopup(true);
      return;
    }
    int keyCode = e.getKeyCode();
    boolean searchNext = false;
    boolean searchPrev = false;
    if (keyCode == KeyEvent.VK_UP) searchPrev = true;
    else if (keyCode == KeyEvent.VK_DOWN) searchNext = true;
    else if (keyCode == KeyEvent.VK_F3) {
      if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0) searchPrev = true;
      else searchNext = true;
    }
    if (searchNext) searchNext(1);
    else if (searchPrev) searchNext(-1);
  }

  public void keyReleased(KeyEvent e) {
    AwtUtil.traverseFocus(myReturnFocus, e);
  }

  public void onChange() {
    searchNext(0);
  }

  private void searchNext(int direction) {
    String text = myField.getText();
    mySearching = true;
    try {
      boolean found = myController.searchText(text, direction);
      myField.setForeground(found ? myNormalForeground : Color.RED);
    } finally {
      mySearching = false;
    }
  }

  public boolean isSearching() {
    return mySearching;
  }

  public boolean isFocused() {
    return SwingTreeUtil.isAncestorOfFocusOwner(myField);
  }

  public boolean ownsComponent(Component c) {
    return myPopup != null && SwingTreeUtil.isAncestor(myField, c);
  }

  public interface Controller {
    void speedSearchClosed();

    boolean searchText(String text, int direction);

    boolean maybeStopSearch(SpeedSearchPopup popup, KeyEvent e);
  }
}
