package com.almworks.util.i18n;

import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class PropertyFileBasedTextBook {
  private static final Object[][] EMPTY = {};
  private final String myBundleName;
  private ResourceBundle myBundle = null;

  public PropertyFileBasedTextBook(String bundleName) {
    myBundleName = bundleName;
  }

  private ResourceBundle getBundle() {
    if (myBundle != null)
      return myBundle;
    synchronized (this) {
      if (myBundle == null) {
        try {
          myBundle = ResourceBundle.getBundle(myBundleName, LTextUtils.SINGLE_LOCALE);
        } catch (MissingResourceException e) {
          Log.warn("cannot find " + myBundleName, e);
          myBundle = new ListResourceBundle() {
            protected Object[][] getContents() {
              return EMPTY;
            }
          };
        }
      }
      return myBundle;       
    }
  }

  protected LText createText(String key, String defaultValue) {
    return new LText(key, defaultValue) {
      public ResourceBundle getBundle(String key) {
        return PropertyFileBasedTextBook.this.getBundle();
      }
    };
  }

  protected <T1> LText1<T1> text1(String key, String defaultValue) {
    return new LText1<T1>(key, defaultValue) {
      public ResourceBundle getBundle(String key) {
        return PropertyFileBasedTextBook.this.getBundle();
      }
    };
  }

  protected <T1, T2> LText2<T1, T2> text2(String key, String defaultValue) {
    return new LText2<T1, T2>(key, defaultValue) {
      public ResourceBundle getBundle(String key) {
        return PropertyFileBasedTextBook.this.getBundle();
      }
    };
  }

  protected void doReplaceText(String prefix, JComponent root) {
    final String finalPrefix;
    if (prefix == null || prefix.length() == 0)
      finalPrefix = "";
    else
      finalPrefix = prefix.charAt(prefix.length() - 1) == '.' ? prefix : prefix + '.';
    UIUtil.visitComponents(root, Component.class, new ElementVisitor<Component>() {
      public boolean visit(Component component) {
        String text = getText(component);
        if (text != null) {
          String replaced = findReplacement(finalPrefix, text);
          if (replaced != null) {
            setText(component, replaced);
            // update tooltip
            if (component instanceof JComponent) {
              String tooltipText = ((JComponent) component).getToolTipText();
              if (tooltipText == null)
                tooltipText = text + "$tooltip";
              String replacedTooltip = findReplacement(finalPrefix, tooltipText);
              if (replacedTooltip != null) {
                ((JComponent) component).setToolTipText(replacedTooltip);
              }
            }
          }
        }

        if (component instanceof JComponent) {
          Border border = ((JComponent) component).getBorder();
          if (border instanceof TitledBorder) {
            String title = ((TitledBorder) border).getTitle();
            if (title != null) {
              String replacedTitle = findReplacement(finalPrefix, title);
              if (replacedTitle != null) {
                ((TitledBorder) border).setTitle(replacedTitle);
              }
            }
          }
        }
        return true;
      }
    });
  }

  private String getText(Component component) {
    if (component instanceof JTextComponent) {
      return ((JTextComponent) component).getText();
    } else if (component instanceof JLabel) {
      return ((JLabel) component).getText();
    } else if (component instanceof AbstractButton) {
      return ((AbstractButton)component).getText();
    } else {
      return null;
    }
  }

  private void setText(@NotNull Component component, @NotNull String text) {
    if (component instanceof JTextComponent) {
      ((JTextComponent) component).setText(text);
    } else if (component instanceof JLabel) {
      NameMnemonic.parseString(text).setToLabel((JLabel) component);
    } else if (component instanceof AbstractButton) {
      NameMnemonic.parseString(text).setToButton((AbstractButton) component);
    } else {
      assert false : component;
    }
  }

  private String findReplacement(String prefix, String text) {
    if (text == null || text.length() == 0)
      return null;
    if (text.charAt(0) != ':')
      return null;
    String key = text.substring(1);
    if (key.length() == 0)
      return null;
    ResourceBundle bundle = getBundle();
    try {
      text = bundle.getString(prefix + key);
      return text;
    } catch (MissingResourceException e) {
      // ignore
      return null;
    }
  }
}
