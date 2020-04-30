package com.almworks.timetrack.gui;

import com.almworks.util.Env;
import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.DropDownListener;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * The base class for adjustment fly-out panels used by the Time Tracker.
 */
public abstract class BaseAdjustmentForm<T> {
  public static <V> JDialog setupFormDialog(BaseAdjustmentForm<V> form, Component parentComponent) {
    if (!parentComponent.isDisplayable()) {
      return null;
    }

    final JPanel formPanel = form.getComponent();
    formPanel.setBorder(
      new CompoundBorder(
        new LineBorder(ColorUtil.between(AwtUtil.getPanelBackground(), Color.BLACK, 0.25F), 1),
        UIUtil.BORDER_5));
    final Dimension sz = formPanel.getPreferredSize();

    final Window parentWindow = SwingUtilities.getWindowAncestor(parentComponent);
    if (parentWindow == null) {
      return null;
    }

    final GraphicsConfiguration gc = parentWindow.getGraphicsConfiguration();
    final Rectangle db = gc.getBounds();
    final Rectangle pb = parentWindow.getBounds();
    final Point pp = SwingUtilities.convertPoint(parentComponent, new Point(0, 0), parentWindow);

    final Point location = new Point();
    if (pb.x + pb.width + sz.width <= db.x + db.width) {
      location.x = pb.x + pb.width;
    } else {
      location.x = pb.x - sz.width;
    }
    if (pb.y + pp.y + sz.height <= db.y + db.height) {
      location.y = pb.y + pp.y;
    } else {
      location.y = db.y + db.height - sz.height;
    }

    final JDialog dialog = new JDialog((JDialog) parentWindow);
    dialog.setContentPane(formPanel);
    dialog.setUndecorated(true);
    dialog.setModal(false);
    dialog.setLocation(location);
    dialog.setAlwaysOnTop(true);
    dialog.setResizable(false);

    return dialog;
  }

  public void attach(final Procedure<T> proc) {
    final DetachComposite life = new DetachComposite();

    final class MyAdapter extends FocusAdapter implements PropertyChangeListener, WindowFocusListener {
      @Override
      public void focusGained(FocusEvent e) {
        UIUtil.addGlobalFocusOwnerListener(life, this);
        UIUtil.addWindowFocusListener(life, SwingTreeUtil.getOwningWindow(getMainPanel()), this);
      }

      public void propertyChange(PropertyChangeEvent evt) {
        final Component v = (Component) evt.getNewValue();
        if (v != null
          && !SwingUtilities.isDescendingFrom(v, getMainPanel())
          && !DropDownListener.isDropDownComponent(v))
        {
          life.detach();
          doCancel(proc);
        }
      }

      public void windowGainedFocus(WindowEvent e) {}

      public void windowLostFocus(WindowEvent e) {
        if(e.getOppositeWindow() == null) {
          life.detach();
          doCancel(proc);
        }
      }
    }

    life.add(UIUtil.addFocusListener(getFocusGrabber(), new MyAdapter()));

    final AbstractAction set = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        life.detach();
        doOk(proc);
      }
    };

    final AbstractAction cancel = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        life.detach();
        doCancel(proc);
      }
    };

    final Pair<JButton, JButton> buttons = getButtons();
    final JButton okButton;
    final JButton cancelButton;

    if(Env.isMac()) {
      okButton = buttons.getSecond();
      cancelButton = buttons.getFirst();
    } else {
      okButton = buttons.getFirst();
      cancelButton = buttons.getSecond();
    }

    okButton.setText("Set");
    okButton.addActionListener(set);
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(cancel);

    final InputMap imap = getMainPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "set");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "set");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

    final ActionMap amap = getMainPanel().getActionMap();
    amap.put("set", set);
    amap.put("cancel", cancel);
  }

  protected abstract Pair<JButton, JButton> getButtons();

  protected abstract JPanel getMainPanel();

  protected abstract JComponent getFocusGrabber();

  protected abstract void doOk(Procedure<T> proc);

  protected abstract void doCancel(Procedure<T> proc);

  public JPanel getComponent() {
    return getMainPanel();
  }
}
