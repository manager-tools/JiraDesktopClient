package com.almworks.util.components.plaf.patches;

import com.almworks.util.DECL;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MacComboBoxPatch {
  private static final Class<?> ourAquaComboBoxUI;
  private static final Method ourGetPopup;

  static {
    Class<?> aClass = null;
    Method method = null;
    try {
      aClass = MacComboBoxPatch.class.getClassLoader().loadClass("apple.laf.AquaComboBoxUI");
      method = aClass.getMethod("getPopup");
    } catch (ClassNotFoundException e) {
      DECL.ignoreException();
    } catch (NoSuchMethodException e) {
      DECL.ignoreException();
    }
    if (aClass == null || method == null) {
      aClass = null;
      method = null;
    }
    ourAquaComboBoxUI = aClass;
    ourGetPopup = method;
  }

  public static void patch(final JComboBox cb) {
    if (ourAquaComboBoxUI == null)
      return;
    ComboBoxUI ui = cb.getUI();
    if (!ourAquaComboBoxUI.isInstance(ui))
      return;
    ComboPopup popup;
    try {
      Object obj = ourGetPopup.invoke(ui);
      if (!(obj instanceof ComboPopup))
        return;
      popup = (ComboPopup) obj;
    } catch (IllegalAccessException e) {
      assert false;
      return;
    } catch (InvocationTargetException e) {
      assert false;
      return;
    }
    final JList list = popup.getList();
    if (list == null)
      return;
    list.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!cb.isPopupVisible())
          return;
        int index = list.getSelectedIndex();
        if (index < 0 || index >= list.getModel().getSize())
          return;
        cb.setSelectedIndex(index);
      }
    });
  }
}
