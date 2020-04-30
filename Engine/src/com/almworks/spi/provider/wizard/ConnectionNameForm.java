package com.almworks.spi.provider.wizard;

import com.almworks.util.components.URLLink;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.ScrollBarPolicy;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * GUI form for the New Connection wizard's connection name page.
 */
public class ConnectionNameForm {
  JPanel myWholePanel;
  JTextField myNameField;
  URLLink myUrlLink;
  JLabel myUserTermLabel;
  JLabel myUserLabel;
  JLabel myUnitsLabel;
  JTextArea myUnitsArea;

  public ConnectionNameForm() {
    adjustToPlatform();
    adjustToTracker();
    configureUnitsArea();
    final Border textFieldBorder = createTextFieldBorder();
    configureLinkAndLabel(textFieldBorder);
    configureEnclosingScrollPane(textFieldBorder);
  }

  private void adjustToPlatform() {
    myUrlLink.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    myUserLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
  }

  private void adjustToTracker() {
    myUserTermLabel.setText(Local.text("app.term.Username") + ":");
  }

  private void configureUnitsArea() {
    myUnitsArea.setOpaque(false);
    myUnitsArea.setRows(10);
    myUnitsArea.setDragEnabled(false);
    myUnitsArea.setBorder(null);
  }

  private Border createTextFieldBorder() {
    final Insets insets = myNameField.getBorder().getBorderInsets(myNameField);
    return new EmptyBorder(0, insets.left, 0, insets.right);
  }

  private void configureLinkAndLabel(Border textFieldBorder) {
    myUrlLink.setBorder(textFieldBorder);
    myUserLabel.setBorder(textFieldBorder);
  }

  private void configureEnclosingScrollPane(Border textFieldBorder) {
    final JScrollPane jsp = SwingTreeUtil.findAncestorOfType(myUnitsArea, JScrollPane.class);
    if(jsp != null) {
      jsp.setBorder(textFieldBorder);
      jsp.getViewport().setOpaque(false);
      jsp.setOpaque(false);
      ScrollBarPolicy.AS_NEEDED.setBoth(jsp);
    }
  }
}
