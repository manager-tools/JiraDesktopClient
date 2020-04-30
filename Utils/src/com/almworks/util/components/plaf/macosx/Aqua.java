package com.almworks.util.components.plaf.macosx;

import com.almworks.util.Env;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.ui.AdjustedSplitPane;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Aqua {
  public static final Color MAC_BORDER_COLOR = Color.GRAY;
  public static final Border MAC_BORDER_NORTH = createBorder(MAC_BORDER_COLOR, BrokenLineBorder.NORTH);
  public static final Border MAC_BORDER_SOUTH = createBorder(MAC_BORDER_COLOR, BrokenLineBorder.SOUTH);
  public static final Border MAC_BORDER_BOX = createBorder(MAC_BORDER_COLOR, BrokenLineBorder.EAST | BrokenLineBorder.SOUTH | BrokenLineBorder.WEST | BrokenLineBorder.NORTH);
  public static final Border MAC_BORDER_NORTH_SOUTH = createBorder(MAC_BORDER_COLOR, BrokenLineBorder.NORTH | BrokenLineBorder.SOUTH);
  public static final Border MAC_BORDER_WEST = createBorder(MAC_BORDER_COLOR, BrokenLineBorder.WEST);

  public static final Color MAC_LIGHT_BORDER_COLOR = Color.LIGHT_GRAY;
  public static final Border MAC_LIGHT_BORDER_NORTH = createBorder(MAC_LIGHT_BORDER_COLOR, BrokenLineBorder.NORTH);
  public static final Border MAC_LIGHT_BORDER_SOUTH = createBorder(MAC_LIGHT_BORDER_COLOR, BrokenLineBorder.SOUTH);

  private static Border createBorder(Color color, int sides) {
    return new BrokenLineBorder(color, 1, sides);
  }

  public static boolean isAqua() {
    return Env.isMac();
  }

  /**
   * Removes the default border from JScrollPane on the Mac
   * to give it a cleaner look.
   * @param scrollPane The component; a JScrollPane is expected,
   * others are silently ignored.
   */
  public static void cleanScrollPaneBorder(JComponent scrollPane) {
    if(isAqua() && scrollPane instanceof JScrollPane) {
      scrollPane.setBorder(null);
    }
  }

  /**
   * Patches a JScrollPane's layout on the Mac so that a lower right
   * corner is made free when only one scrollbar is shown. This is needed
   * for those scrollpanes that share its lower right corner with the
   * window.
   * @param scrollPane The component; a JScrollPane is expected,
   * others are silently ignored.
   */
  public static void cleanScrollPaneResizeCorner(JComponent scrollPane) {
    if(isAqua() && !Env.isMacLionOrNewer() && scrollPane instanceof JScrollPane) {
      scrollPane.setLayout(new MacCornerScrollPaneLayout());
    }
  }

  public static void makeLeopardStyleSplitPane(JComponent sp) {
    if(isAqua() && sp instanceof AdjustedSplitPane) {
      sp.putClientProperty(AdjustedSplitPane.MAC_LEOPARD_STYLE, true);
    }
  }

  public static void makeSmallComponent(JComponent c) {
    if(isAqua()) {
      c.putClientProperty("JComponent.sizeVariant", "small");
    }
  }

  public static void makeMiniComponent(JComponent c) {
    if(isAqua()) {
      c.putClientProperty("JComponent.sizeVariant", "mini");
    }
  }

  public static void makeSearchField(JTextField f) {
    if(isAqua()) {
      f.putClientProperty("JTextField.variant", "search");
    }
  }

  public static void makeSquareButton(AbstractButton b) {
    if(isAqua()) {
      b.putClientProperty("JButton.buttonType", "square");
    }
  }

  public static void makeBevelButton(AbstractButton b) {
    if(isAqua()) {
      b.putClientProperty("JButton.buttonType", "bevel");
    }
  }

  public static void makeBorderlessTabbedPane(JTabbedPane tp) {
    if(isAqua()) {
      tp.setBorder(new EmptyBorder(0, -11, -13, -10));
    }
  }

  public static void addSouthBorder(JComponent c) {
    if(isAqua()) {
      UIUtil.addOuterBorder(c, MAC_BORDER_SOUTH);
    }
  }

  public static void addNorthBorder(JComponent c) {
    if(isAqua()) {
      UIUtil.addOuterBorder(c, MAC_BORDER_NORTH);
    }
  }

  public static void setLightNorthBorder(JComponent c) {
    if(isAqua()) {
      c.setBorder(MAC_LIGHT_BORDER_NORTH);
    }
  }

  /**
   * On Mac OS unsets mnemonics for all {@link javax.swing.JLabel}s and
   * {@link javax.swing.AbstractButton}s contained by the given component.
   * Has no effect on other platforms.
   * @param parent The parent component.
   */
  public static void disableMnemonics(JComponent parent) {
    if(Env.isMac()) {
      UIUtil.visitComponents(parent, JComponent.class, new ElementVisitor<JComponent>() {
        public boolean visit(JComponent element) {
          if(element instanceof AbstractButton) {
            final AbstractButton btn = (AbstractButton) element;
            btn.setMnemonic(0);
            btn.setDisplayedMnemonicIndex(-1);
          } else if(element instanceof JLabel) {
            final JLabel lbl = (JLabel) element;
            lbl.setDisplayedMnemonic(0);
            lbl.setDisplayedMnemonic(-1);
          }
          return true;
        }
      });
    }
  }
}
