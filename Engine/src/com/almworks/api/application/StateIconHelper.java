package com.almworks.api.application;

import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;

public class StateIconHelper {
  private static final TypedKey STATE_ICONS = TypedKey.create("##stateIcons");

  private static final WeakHashMap<Icon, StateIcon> myCachedStateIcons = new WeakHashMap<Icon, StateIcon>();

  public static void addStateIcon(PropertyMap values, StateIcon icon) {
    if (icon == null) return;
    Object obj = values.get(STATE_ICONS);
    if (obj == null) {
      values.put(STATE_ICONS, icon);
    } else if (obj instanceof StateIcon) {
      if (obj.equals(icon)) 
        return;
      values.put(STATE_ICONS, new StateIcon[] {(StateIcon) obj, icon});
    } else if (obj instanceof StateIcon[]) {
      StateIcon[] oldArray = (StateIcon[]) obj;
      int len = oldArray.length;
      for (StateIcon i : oldArray) {
        if (icon.equals(i)) return;
      }
      StateIcon[] newArray = new StateIcon[len + 1];
      System.arraycopy(oldArray, 0, newArray, 0, len);
      newArray[len] = icon;
      values.put(STATE_ICONS, newArray);
    } else {
      assert false : obj;
    }
  }

  public static void removeStateIcons(PropertyMap values, Collection<StateIcon> icons) {
    if (icons == null || icons.isEmpty()) return;
    Object obj = values.get(STATE_ICONS);
    if (obj == null) return;
    if (obj instanceof StateIcon) {
      if (!icons.contains(obj))
        return;
      values.remove(STATE_ICONS);
    } else if (obj instanceof StateIcon[]) {
      List<StateIcon> value = Collections15.arrayList((StateIcon[]) obj);
      int oldSize = value.size();
      value.removeAll(icons);
      int newSize = value.size();
      if (newSize == oldSize) return;
      if (value.isEmpty()) {
        values.remove(STATE_ICONS);
      } else if (newSize == 1) {
        values.put(STATE_ICONS, value.get(0));
      } else {
        values.put(STATE_ICONS, value.toArray(new StateIcon[newSize]));
      }
    } else {
      assert false : obj;
    }
  }

  @Nullable
  public static StateIcon[] getStateIcons(PropertyMap values) {
    Object obj = values.get(STATE_ICONS);
    if (obj == null) {
      return null;
    } else if (obj instanceof StateIcon) {
      return new StateIcon[] {(StateIcon) obj};
    } else if (obj instanceof StateIcon[]) {
      StateIcon[] array = (StateIcon[]) obj;
      Arrays.sort(array, StateIcon.ORDER_BY_PRIORITY);
      return array;
    } else {
      assert false : obj;
      return null;
    }
  }

  private StateIconHelper() {
  }

  public static synchronized StateIcon getCachedIcon(Icon icon) {
    return myCachedStateIcons.get(icon);
  }

  public static void putCachedIcon(Icon icon, StateIcon stateIcon) {
    myCachedStateIcons.put(icon, stateIcon);
  }
}
