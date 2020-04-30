package com.almworks.util.components.plaf.macosx.combobox;

import com.almworks.util.ReflectionUtil;
import com.almworks.util.components.plaf.macosx.Aqua;
import org.almworks.util.Log;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;

public class MacComboBoxEditor extends BasicComboBoxEditor {
  private static final Border EDITOR_BORDER = new MacComboBoxEditorBorder(false);
  private static final Border DISABLED_EDITOR_BORDER = new MacComboBoxEditorBorder(true);
  
  private static final Action TRIGGER_SELECTION = getTriggerSelectionAction();
  public static final boolean IS_AVAILABLE = TRIGGER_SELECTION != null;

  private static final Color GRAY_150 = gray(150);
  private static final Color GRAY_175 = gray(175);
  private static final Color GRAY_200 = gray(200);
  private static final Color GRAY_205 = gray(205);
  private static final Color GRAY_220 = gray(220);
  private static final Color GRAY_230 = gray(230);
  private static final Color GRAY_250 = gray(250);

  private MacComboBoxEditor() {
    super();
  }

  public static void install(JComboBox combo) {
    assert Aqua.isAqua();
    if (IS_AVAILABLE) {
      combo.setEditor(new MacComboBoxEditor());
    }
  }

  private static Color gray(int value) {
    return new Color(value, value, value);
  }

  private static Action getTriggerSelectionAction() {
    if (Aqua.isAqua()) {
      try {
        Class<?> clazz = Class.forName("com.apple.laf.AquaComboBoxUI");
          Field field = clazz.getDeclaredField("triggerSelectionAction");
        if (field == null || !ReflectionUtil.isStatic(field)) return null; // todo remove - this field is not-static in Java8
        if (Action.class.isAssignableFrom(field.getType())) {
          field.setAccessible(true);
          return (Action) field.get(null);
        }
      } catch (Exception e) {
        Log.warn(e);
      }
    }
    return null;
  }

  @Override
  protected JTextField createEditorComponent() {
    return new MacComboBoxTextField();
  }

  protected JTextField getField() {
    return editor;
  }

  private class MacComboBoxTextField extends JTextField implements FocusListener {
    private MacComboBoxTextField() {
      setBorder(isEnabled() ? EDITOR_BORDER : DISABLED_EDITOR_BORDER);

      final InputMap inputMap = getInputMap();
      inputMap.put(KeyStroke.getKeyStroke("DOWN"), "aquaSelectNext");
      inputMap.put(KeyStroke.getKeyStroke("KP_DOWN"), "aquaSelectNext");
      inputMap.put(KeyStroke.getKeyStroke("UP"), "aquaSelectPrevious");
      inputMap.put(KeyStroke.getKeyStroke("KP_UP"), "aquaSelectPrevious");
      inputMap.put(KeyStroke.getKeyStroke("HOME"), "aquaSelectHome");
      inputMap.put(KeyStroke.getKeyStroke("END"), "aquaSelectEnd");
      inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "aquaSelectPageUp");
      inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "aquaSelectPageDown");
      inputMap.put(KeyStroke.getKeyStroke("SPACE"), "aquaSpacePressed");
      inputMap.put(KeyStroke.getKeyStroke("ENTER"), "alm:aquaEnterPressed");

      getActionMap().put("alm:aquaEnterPressed", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JComboBox combo = getCombo();
          if (combo == null) {
            return;
          }
          if (combo.isPopupVisible()) {
            Object source = e.getSource();
            e.setSource(combo);
            try {
              if (TRIGGER_SELECTION != null) TRIGGER_SELECTION.actionPerformed(e);
            } finally {
              e.setSource(source);
            }
            selectAll();
          } else {
            Action action = getActionMap().get("notify-field-accept");
            if (action != null) {
              action.actionPerformed(e);
            }
          }
        }
      });

      addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if ("enabled".equals(evt.getPropertyName())) {
            setBorder(Boolean.TRUE.equals(evt.getNewValue()) ? EDITOR_BORDER : DISABLED_EDITOR_BORDER);
            repaint();
          }
        }
      });

      addFocusListener(this);
    }
    
    private JComboBox getCombo() {
      Container ancestor = SwingUtilities.getAncestorOfClass(JComboBox.class, MacComboBoxTextField.this);
      if (ancestor instanceof JComboBox && ancestor.isVisible()) {
        return (JComboBox) ancestor;
      }
      return null;
    }

    @Override
    public boolean hasFocus() {
      if (!super.hasFocus()) {
        return false;
      }
      MacPrettyComboBox combo = Util.castNullable(MacPrettyComboBox.class, getCombo());
      if (combo != null && combo.isPaintingButton()) {
        return false; // avoiding double focus ring around the button
      }
      return true;
    }

    @Override
    public void focusGained(FocusEvent e) {
      repaintCombobox();
    }

    @Override
    public void focusLost(FocusEvent e) {
      repaintCombobox();
    }

    private void repaintCombobox() {
      final Container parent = getParent();
      if (parent == null) {
        return;
      }
      if (parent instanceof JComponent && Boolean.TRUE == ((JComponent)parent).getClientProperty("JComboBox.isTableCellEditor")) {
        return;
      }
      final Container grandParent = parent.getParent();
      if (grandParent != null) {
        grandParent.repaint();
      }
    }

    @Override
    public Dimension getMinimumSize() {
      final Dimension minimumSize = super.getMinimumSize();
      return new Dimension(minimumSize.width, minimumSize.height + 2);
    }

    @Override
    public Dimension getPreferredSize() {
      return getMinimumSize();
    }

    @Override
    public void setBounds(final int x, final int y, final int width, final int height) {
      // fix for too wide combobox editor, see AquaComboBoxUI.layoutContainer:
      // it adds +4 pixels to editor width. WTF?!
      reshape(x, y, width - 4, height - 1);
    }
  }

  public static class MacComboBoxEditorBorder implements Border {
    private boolean myDisabled;

    public MacComboBoxEditorBorder(final boolean disabled) {
      myDisabled = disabled;
    }

    @Override
    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
      Color topColor;
      Color secondTopColor;
      Color leftRightColor;
      Color bottomColor;

      if (myDisabled) {
        topColor = GRAY_200;
        secondTopColor = GRAY_250;
        leftRightColor = GRAY_205;
        bottomColor = GRAY_220;
      } else {
        topColor = GRAY_150;
        secondTopColor = GRAY_230;
        leftRightColor = GRAY_175;
        bottomColor = GRAY_200;
      }

      int _y = y + MacComboBoxFocusRing.MAC_COMBO_BORDER_V_OFFSET;

      g.setColor(topColor);
      g.drawLine(x + 3, _y + 3, x + width - 1, _y + 3);

      g.setColor(secondTopColor);
      g.drawLine(x + 3, _y + 4, x + width - 1, _y + 4);

      g.setColor(leftRightColor);
      g.drawLine(x + 3, _y + 4, x + 3, _y + height - 4);
      g.drawLine(x + width - 1, _y + 4, x + width - 1, _y + height - 4);

      g.setColor(bottomColor);
      g.drawLine(x + 4, _y + height - 4, x + width - 2, _y + height - 4);

      Container panel = SwingUtilities.getAncestorOfClass(JPanel.class, c);
      g.setColor(panel == null ? UIManager.getColor("Panel.background") : panel.getBackground());

      g.fillRect(x,  y, width, 3 + MacComboBoxFocusRing.MAC_COMBO_BORDER_V_OFFSET);
      g.fillRect(x, _y, 3, height);
      g.fillRect(x, _y + height - 3, width, 3);
    }

    @Override
    public Insets getBorderInsets(final Component c) {
      return new Insets(6, 6, 4, 3);
    }

    @Override
    public boolean isBorderOpaque() {
      return true;
    }
  }
}