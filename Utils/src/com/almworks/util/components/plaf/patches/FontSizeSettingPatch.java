package com.almworks.util.components.plaf.patches;

import com.almworks.util.Env;
import com.almworks.util.components.plaf.LAFExtension;
import org.almworks.util.Collections15;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.List;

public class FontSizeSettingPatch extends LAFExtension {
  private static final List<String> FONT_KEYS = Collections15.unmodifiableListCopy(
    "Button.font",
    "CheckBox.font",
    "ComboBox.font",
    "EditorPane.font",
    "FormattedTextField.font",
    "Label.font",
    "List.font",
    "RadioButton.font",
    "Panel.font",
    "PasswordField.font",
    "ProgressBar.font",
    "ScrollPane.font",
    "Spinner.font",
    "TabbedPane.font",
    "Table.font",
    "TableHeader.font",
    "TextArea.font",
    "TextField.font",
    "TextPane.font",
    "TitledBorder.font",
    "ToggleButton.font",
    "Tree.font",
    "Viewport.font",
    "MenuBar.font",
    "MenuItem.acceleratorFont",
    "MenuItem.font",
    "Menu.font",
    "PopupMenu.font",
    "CheckBoxMenuItem.font",
    "Link.font",
    "ToolTip.font",
    "ToolBar.font",
    "OptionPane.font"
  );

  public void install(LookAndFeel laf) {
    int absoluteSize = getFontSizeAbs();
    int baseNewSize = getFontSize();
    assert baseNewSize != 0 || absoluteSize != 0;
    if (baseNewSize == 0 && absoluteSize == 0)
      return;
    double ratio = 0;
    if (baseNewSize != 0) {
      Font baseFont = defaults().getFont("Label.font");
      assert baseFont != null;
      if (baseFont == null) {
        return;
      }
      int baseOldSize = baseFont.getSize();
      assert baseOldSize != 0;
      if (baseOldSize == 0)
        return;
      ratio = ((double)baseNewSize) / baseOldSize;
    }
    for (String fontKey : FONT_KEYS) {
      patchFont(fontKey, absoluteSize, ratio);
    }
  }

  private void patchFont(String key, int absoluteSize, double ratio) {
    Font old = defaults().getFont(key);
    if (old == null)
      return;
    int oldSize = old.getSize();
    int size = absoluteSize != 0 ? absoluteSize : (int)(oldSize * ratio);
    if (size != oldSize) {
      save(key);
      FontUIResource newFont = new FontUIResource(old.getName(), old.getStyle(), size);
      defaults().put(key, newFont);
    }
  }

  public void uninstall(LookAndFeel laf) {
    for (String fontKey : FONT_KEYS) {
      restore(fontKey);
    }
  }

  public boolean isExtendingLookAndFeel(LookAndFeel laf) {
    return getFontSize() != 0 || getFontSizeAbs() != 0;
  }

  public static int getFontSize() {
    return getSetting("font.size");
  }

  public static int getFontSizeAbs() {
    return getSetting("font.size.abs");
  }

  public static int getOverrideFontSize() {
    int size = getFontSizeAbs();
    if (size > 0) return size;
    size = getFontSize();
    return size > 0 ? size : 0;
  }

  private static int getSetting(String name) {
    try {
      String setting = Env.getString(name);
      if (setting == null)
        return 0;
      int size = Integer.parseInt(setting);
      return (size < 5 || size > 72) ? 0 : size;
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
