package com.almworks.util.components;

import com.almworks.util.L;
import com.almworks.util.text.RODocument;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class ReadOnlyTextFields {
  public static final ComponentProperty<Integer> DEFAULT_COLUMNS = ComponentProperty.createProperty("DEFAULT_COLUMNS");
  public static final String CROP_MARK = " \u2026";

  public static void setText(final JTextComponent component, final String text) {
    RODocument.setComponentText(component, text);
    UIUtil.scrollToTop(component);

    VisibilityGroup group = VisibilityGroup.COMPONENT_PROPERTY.getClientValue(component);
    if (group != null)
      group.checkVisibility();
  }

  public static Detach setTextFieldText(final JTextComponent field, final String text) {
    String crippledText = text;
    Integer value = DEFAULT_COLUMNS.getClientValue(field);
    int columns = value == null ? 0 : value.intValue();
    if (columns > 0) {
      FontMetrics metrics = field.getFontMetrics(field.getFont());
      int maxWidth = metrics.charWidth('m') * columns;
      int width = metrics.stringWidth(text);

      if (width > maxWidth) {
        setColumns(field, columns);

        int length = 0;
        while (true) {
          length = crippledText.length();
          if (length == 0)
            break;
          int dec = Math.max(length >> 4, 1);
          length -= dec;
          crippledText = crippledText.substring(0, length);
          if (metrics.stringWidth(crippledText + CROP_MARK) <= maxWidth)
            break;
        }

        if (length > 0) {
          crippledText += CROP_MARK;
          field.setToolTipText(L.tooltip("Click on the cropped text to view and select full text"));
        }
      } else {
        setColumns(field, 0);
        if (field.getToolTipText() != null)
          field.setToolTipText(L.tooltip(field.getToolTipText()));
      }
    }

    setText(field, text);

    CommunalFocusListener<JTextComponent> listener = CommunalFocusListener.create(field);
    listener.addActivity(CommunalFocusListener.ChangeBorder.FOCUSED_COMPONENT);
    listener.addActivity(setTextActivity(text, crippledText));
    listener.addActivity(CommunalFocusListener.HANDLE_TEXT);
    return listener.attach();
  }

  public static void setupReadOnlyTextField(JTextComponent field) {
    basicReadonlySetup(field);

    // store values for ReadOnlyTextController
    int columns = getColumns(field);
    if (columns > 0) {
      DEFAULT_COLUMNS.putClientValue(field, columns);
      setColumns(field, 0);
    }

    CommunalFocusListener.ChangeBorder.prepare(field);
  }

  public static void basicReadonlySetup(JTextComponent field) {
    field.setMargin(new Insets(0, 0, 0, 0));
    field.setEditable(false);
    field.setOpaque(false);
  }

  /** @noinspection ChainOfInstanceofChecks*/
  private static int getColumns(JTextComponent component) {
    if (component instanceof JTextField)
      return ((JTextField) component).getColumns();
    else if (component instanceof JTextArea)
      return ((JTextArea) component).getColumns();
    return 0;
  }

  /** @noinspection ChainOfInstanceofChecks*/
  private static void setColumns(JTextComponent component, int columns) {
    if (component instanceof JTextField)
      ((JTextField) component).setColumns(columns);
    else if (component instanceof JTextArea)
      ((JTextArea) component).setColumns(columns);
    else
      assert false : "Unknown class: " + component.getClass();
  }

  public static CommunalFocusListener.Activity<JTextComponent> setTextActivity(final String focusedText, final String unfocusedText) {
    return new CommunalFocusListener.Activity<JTextComponent>() {
      public void onFocusGained(JTextComponent component) {
        RODocument.setComponentText(component, focusedText);
      }

      public void onFocusLost(JTextComponent component) {
        RODocument.setComponentText(component, unfocusedText);
      }
    };
  }
}
