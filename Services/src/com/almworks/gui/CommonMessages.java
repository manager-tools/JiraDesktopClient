package com.almworks.gui;

import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;

public class CommonMessages {
  private static final LocalizedAccessor I18N  = CurrentLocale.createAccessor(CommonMessages.class.getClassLoader(), "com/almworks/gui/message");

  public static final LocalizedAccessor.Value OPEN_IN_BROWSER = I18N.getFactory("action.openInBrowser.name");
}
