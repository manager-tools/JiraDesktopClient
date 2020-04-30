package com.almworks.util.i18n;

import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.ComponentVisitor;

import javax.swing.*;

import static com.almworks.util.i18n.ComponentLocalizer.Kind.TEXT;
import static com.almworks.util.i18n.ComponentLocalizer.Kind.TOOLTIP;

public class ComponentLocalizerVisitor extends ComponentVisitor {
  private final ComponentLocalizer myLocalizer;

  public ComponentLocalizerVisitor(ComponentLocalizer extractor) {
    myLocalizer = extractor;
  }

  protected void visitJLabel(JLabel c) {
    String text = c.getText();
    String localized = myLocalizer.forJLabel(c, TEXT, text);
    if (localized != null)
      NameMnemonic.parseString(localized).setToLabel(c);
    super.visitJLabel(c);
  }

  protected void visitJComponent(JComponent c) {
    String text = c.getToolTipText();
    String localized = myLocalizer.forJComponent(c, TOOLTIP, text);
    if (localized != null)
      c.setToolTipText(localized);
    super.visitJComponent(c);
  }
}
