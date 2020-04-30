package com.almworks.util;

import com.almworks.util.i18n.LText;
import com.almworks.util.i18n.LText1;
import com.almworks.util.i18n.LText2;
import com.almworks.util.i18n.PropertyFileBasedTextBook;

import javax.swing.*;

public class AppBook extends PropertyFileBasedTextBook {
  private static final AppBook BOOK = new AppBook();

  private AppBook() {
    super("com.almworks.rc.Application");
  }

  public static void replaceText(String prefix, JComponent root) {
    BOOK.doReplaceText(prefix, root);
  }

  public static LText text(String key, String defaultValue) {
    return BOOK.createText(key, defaultValue);
  }

  public static <T1> LText1<T1> text(String key, String defaultValue, T1 sample1) {
    return BOOK.<T1>text1(key, defaultValue);
  }

  public static <T1, T2> LText2<T1, T2> text(String key, String defaultValue, T1 sample1, T2 sample2) {
    return BOOK.<T1, T2>text2(key, defaultValue);
  }
}
