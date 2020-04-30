package com.almworks.util.i18n;

import com.almworks.util.components.Link;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.util.Set;

public class NameBasedComponentLocalizer implements ComponentLocalizer {
  private final Set<Class> myClasses;
  private final String myPrefix;

  public NameBasedComponentLocalizer(String prefix, Class ... classes) {
    assert classes.length > 0;
    myPrefix = prefix;
    myClasses = Collections15.hashSet(classes);
  }

  public String forJComponent(JComponent c, Kind kind, String value) {
    if (!myClasses.contains(c.getClass()))
      return null;
    assert kind == Kind.TOOLTIP;
    String name = c.getName();
    if (name != null) {
      if (myPrefix != null)
        name = myPrefix + name;
      name = name + "$tooltip";
      return Local.textOrNull(name);
    }
    return null;
  }

  public String forJLabel(JLabel c, Kind kind, String value) {
    if (!myClasses.contains(JLabel.class))
      return null;
    assert kind == Kind.TEXT;
    String name = c.getName();
    String oldText = c.getText();
    int index = c.getDisplayedMnemonicIndex();
    return forText(name, oldText, index);
  }

  public String forText(String name, String oldText, int oldIndex) {
    if (name != null) {
      if (myPrefix != null)
        name = myPrefix + name;
      char oldMnemonic = (oldIndex >= 0 && oldIndex < oldText.length()) ? oldText.charAt(oldIndex) : 0;
      String replacement = Local.textOrNull(name);
      if (replacement != null) {
        boolean hasAmpersand = replacement.indexOf('&') >= 0;
        if (!hasAmpersand) {
          int mnemonicIndex = replacement.indexOf(oldMnemonic);
          if (mnemonicIndex >= 0) {
            replacement = replacement.substring(0, mnemonicIndex) + '&' + replacement.substring(mnemonicIndex);
          }
        }
        return replacement;
      }
    }
    return null;
  }

  public void localizeLink(Link link) {
    String replacement = forText("url", link.getText(), link.getDisplayedMnemonicIndex());
    if (replacement != null) {
      NameMnemonic.parseString(replacement).setToButton(link);
    }
  }
}
