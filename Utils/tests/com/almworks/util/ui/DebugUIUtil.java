package com.almworks.util.ui;

import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.ReadAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author dyoma
 */
public class DebugUIUtil {
  public static Convertor<Component, String> DEBUG_COMPONENT_TEXT = new Convertor<Component, String>() {
    public String convert(Component value) {
      return getDebugText(value);
    }
  };

  private static final Map<Class, ReadAccessor<Component, String>> DEBUG_TEXT_GETTERS;

  public static String getDebugText(Component component) {
    if (component == null)
      return "<null>";
    for (Iterator<Class> iterator = DEBUG_TEXT_GETTERS.keySet().iterator(); iterator.hasNext();) {
      Class aClass = iterator.next();
      Component casted = (Component) Util.castNullable(aClass, component);
      if (casted == null)
        continue;
      ReadAccessor<Component, String> getter = DEBUG_TEXT_GETTERS.get(aClass);
      return getter.getValue(casted);
    }
    return "Unsupported class: " + component.getClass();
  }

  private static <T extends Component>
  void registerDebugTextGetter(Map<Class, ReadAccessor<? extends Component, String>> registry, Class<T> aClass,
    ReadAccessor<T, String> getter) {
    registry.put(aClass, getter);
  }

  static {
    HashMap<Class, ReadAccessor<? extends Component, String>> map = Collections15.hashMap();
    registerDebugTextGetter(map, JLabel.class, new ReadAccessor<JLabel, String>() {
      public String getValue(JLabel object) {
        return object.getText();
      }
    });
    registerDebugTextGetter(map, AbstractButton.class, new ReadAccessor<AbstractButton, String>() {
      public String getValue(AbstractButton object) {
        return object.getText();
      }
    });
    registerDebugTextGetter(map, JTextComponent.class, new ReadAccessor<JTextComponent, String>() {
      public String getValue(JTextComponent object) {
        return object.getText();
      }
    });
    DEBUG_TEXT_GETTERS = (Map)map;
  }
}
