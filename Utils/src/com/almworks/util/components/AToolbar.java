package com.almworks.util.components;

import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.IdActionProxy;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * @author : Dyoma
 */
public class AToolbar extends JToolBar {
  public static final int MINIMUM_SEPARATOR_WIDTH = 10;
  private final JComponent myContextComponent;

  public AToolbar() {
    this(null);
  }

  public AToolbar(JComponent component) {
    this(component, InlineLayout.HORISONTAL);
  }

  public AToolbar(JComponent component, InlineLayout.Orientation orientation) {
    this(component, new InlineLayout(orientation, 0, false), orientation.isVertical() ? JToolBar.VERTICAL : JToolBar.HORIZONTAL);
  }

  public AToolbar(JComponent component, LayoutManager layout, int orientation) {
    super(orientation);
    myContextComponent = component;
    setLayout(layout);
    super.setRollover(true);
    super.setFloatable(false);
  }

  public AActionButton addAction(AnAction action) {
    return addAction(action, myContextComponent);
  }

  public DropDownButton addDropDownButton(JComponent contextComponent, String unselectedName) {
    DropDownButton button = createDropDownButton(contextComponent != null ? contextComponent : myContextComponent, unselectedName);
    add(button);
    return button;
  }

  public static DropDownButton createDropDownButton(JComponent contextComponent, String unselectedName) {
    AActionButton primaryButton = new AToolbarButton();
    primaryButton.setMargin(new Insets(0, 3, 0, 3));
    DropDownButton button = new DropDownButton(primaryButton, unselectedName);
    button.setContextComponent(contextComponent);
    return button;
  }

  public AActionButton addAction(String actionId) {
    return addAction(new IdActionProxy(actionId));
  }

  public AActionButton addAction(AnAction action, JComponent contextComponent) {
    AActionButton button = createActionButton(action, contextComponent);
    add(button);
    return button;
  }

  public static AToolbarButton createActionButton(AnAction action, JComponent contextComponent) {
    AToolbarButton button = new AToolbarButton();
    button.setContextComponent(contextComponent);
    button.setAnAction(action);
    return button;
  }

  public Detach addAllActions(List<? extends AnAction> actions) {
    DetachComposite detach = new DetachComposite();
    for (AnAction action : actions) {
      final AActionButton button = addAction(action);
      detach.add(new Detach() {
        protected void doDetach() {
          remove(button);
        }
      });
    }
    return detach;
  }

  public void addToWest(JPanel panel) {
    assert panel.getLayout() instanceof BorderLayout : String.valueOf(panel.getLayout());
    setLayout(InlineLayout.vertical(0));
    panel.add(this, BorderLayout.WEST);
  }

  public void addToNorth(JPanel panel) {
    assert panel.getLayout() instanceof BorderLayout : String.valueOf(panel.getLayout());
    setLayout(InlineLayout.horizontal(0));
    panel.add(this, BorderLayout.NORTH);
  }

  public void addSeparator() {
    JSeparator separator = new JSeparator(JSeparator.VERTICAL);
    Dimension size = separator.getPreferredSize();
    if (size != null && size.width < MINIMUM_SEPARATOR_WIDTH) {
      JPanel panel = new JPanel(new SingleChildLayout(SingleChildLayout.PREFERRED, SingleChildLayout.CONTAINER));
      panel.add(separator);
      int add = (MINIMUM_SEPARATOR_WIDTH - size.width + 1) / 2;
      Border additional = new EmptyBorder(0, add, 0, add);
      panel.setBorder(additional);
      add(panel);
    } else {
      add(separator);
    }
  }
}
