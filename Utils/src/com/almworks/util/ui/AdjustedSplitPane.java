package com.almworks.util.ui;

import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AdjustedSplitPane extends JSplitPane implements PropertyChangeListener {
  /**
   * The key of a Boolean client property responsible for
   * Leopard-style divider decorations.
   */
  public static final String MAC_LEOPARD_STYLE = "AdjustedSplitPane.macLeopardStyle";
  public static final String AERO_BORDERED_DIVIDER = "AdjustedSplitPane.aeroDividerStyle";

  private static final int DIVIDER_SHOWN = Integer.MIN_VALUE;

  private boolean myValidateRoot = true;
  private BasicSplitPaneDivider myDivider;
  private int myHiddenDividerSize = DIVIDER_SHOWN;
  private Border myHiddenDividerBorder;

  public AdjustedSplitPane(int orientation, Component leftTop, Component rightBottom) {
    super(orientation, true, leftTop, rightBottom);
    updateUI();
    setOneTouchExpandable(false);
    setResizeWeight(0.5);

    if(Aqua.isAqua() || Aero.isAero()) {
      addPropertyChangeListener(this);
    }
  }

  public void setUI(SplitPaneUI ui) {
    super.setUI(ui);
    setBorder(null);
  }

  public void add(Component comp, Object constraints) {
    if (constraints == JSplitPane.DIVIDER) {
      assert comp instanceof BasicSplitPaneDivider : String.valueOf(comp);
      myDivider = ((BasicSplitPaneDivider) comp);
      decorateDivider(myDivider);
    }

    super.add(comp, constraints);
  }

  /**
   * Listens to the MAC_LEOPARD_STYLE client property
   * changes and cofigures the divider accordingly.
   * @param evt The property change event.
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if((MAC_LEOPARD_STYLE.equals(evt.getPropertyName()) && Aqua.isAqua())
       || (AERO_BORDERED_DIVIDER.equals(evt.getPropertyName()) && Aero.isAero()))
    {
      decorateDivider(myDivider);
    }
  }

  /**
   * Platform- and property-specific divider decoration.
   * @param divider The divider.
   */
  private void decorateDivider(BasicSplitPaneDivider divider) {
    if(Boolean.TRUE.equals(getClientProperty(AERO_BORDERED_DIVIDER)) && Aero.isAero()) {
      divider.setBorder(
        getOrientation() == HORIZONTAL_SPLIT
          ? Aero.getAeroBorderWestEast() : Aero.getAeroBorderNorthSouth());
      setDividerSize(7);
      divider.setDividerSize(7);
    } else if(Boolean.TRUE.equals(getClientProperty(MAC_LEOPARD_STYLE)) && Aqua.isAqua()) {
      if(getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
        divider.setDividerSize(1);
        divider.setBorder(Aqua.MAC_BORDER_WEST);
      } else {
        divider.setBorder(Aqua.MAC_BORDER_NORTH_SOUTH);
      }
    } else {
      final Border border = divider.getBorder();
      final Insets insets = border != null ? border.getBorderInsets(divider) : new Insets(0, 0, 0, 0);
      divider.setBorder(new EmptyBorder(insets));
    }
  }

  public boolean isValidateRoot() {
    return myValidateRoot;
  }

  public void setValidateRoot(boolean validateRoot) {
    myValidateRoot = validateRoot;
  }

  public void hideDivider() {
    if(myHiddenDividerSize == DIVIDER_SHOWN) {
      myHiddenDividerSize = myDivider.getDividerSize();
      myDivider.setDividerSize(0);
      myHiddenDividerBorder = myDivider.getBorder();
      myDivider.setBorder(null);
    }
  }

  public void showDivider() {
    if(myHiddenDividerSize != DIVIDER_SHOWN) {
      myDivider.setDividerSize(myHiddenDividerSize);
      myHiddenDividerSize = DIVIDER_SHOWN;
      myDivider.setBorder(myHiddenDividerBorder);
      myHiddenDividerBorder = null;
    }
  }
}
