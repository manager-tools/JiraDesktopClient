package com.almworks.util.i18n.text.util;

import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.I18NAccessor;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class TranslateFormProcedure extends LocalizedUIUpdate.Visitor {
  private final I18NAccessor myAccessor;
  private final String myLocalizationPrefix;
  private final Map<String, String> mySpecial = Collections15.hashMap();

  public TranslateFormProcedure(I18NAccessor accessor, String localizationPrefix) {
    myAccessor = accessor;
    myLocalizationPrefix = localizationPrefix;
  }

  public TranslateFormProcedure addSpecial(String name, String textId) {
    mySpecial.put(name, textId);
    return this;
  }

  @Override
  protected void updateComponent(Component component) {
    CurrentLocale accessor = new CurrentLocale(myAccessor);
    String name = component.getName();
    if (name == null) return;
    String id = mySpecial.get(name);
    if (id == null) {
      if (!name.startsWith(myLocalizationPrefix)) return;
      id = name.substring(myLocalizationPrefix.length());
    }
    String localizedText = accessor.getString(id);
    if (localizedText.isEmpty()) {
      LogHelper.error("Cannot translate", name, id, component.toString(), component);
      return;
    }

    if (component instanceof JLabel) {
      NameMnemonic.parseString(localizedText).setToLabel((JLabel) component);
    } else if (component instanceof AbstractButton) {
      NameMnemonic.parseString(localizedText).setToButton((AbstractButton) component);
    }
  }

  public void install(JComponent root) {
    LocalizedUIUpdate.UPDATE_LOCALE.putClientValue(root, this);
    invoke(root);
  }
}
