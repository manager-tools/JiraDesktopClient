package com.almworks.util.ui.swing;

import com.almworks.util.Env;
import org.almworks.util.Collections15;
import org.almworks.util.StringUtil;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Map;

/**
 * Contains some predefined keyboard shortcuts as public constants.
 * Also contains utility fields and methods for constructing 
 * platform-dependent string representations of keyboard shortcuts.
 * E.g. "Ctrl+C" on Windows becomes "\u2318C" on the Mac.
 * @author dyoma
 * @author pzvyagin
 */
public class Shortcuts {
  public static final int MENU_MASK = Env.isMac() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;

  // todo #794 - see usages
  public static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
  // todo #793 - see usages
  public static final KeyStroke CTRL_ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK);
  public static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
  // todo #793 - see usages
  public static final KeyStroke F2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
  public static final KeyStroke ALT_INSERT = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_MASK);
  public static final KeyStroke DELETE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);

  public static final KeyStroke CTRL_Z = ksMenu(KeyEvent.VK_Z);
  public static final KeyStroke CTRL_Y = ksMenu(KeyEvent.VK_Y);
  public static final KeyStroke CTRL_SHIFT_Z = ksShiftMenu(KeyEvent.VK_Z);

  public static final KeyStroke UNDO = CTRL_Z;
  public static final KeyStroke REDO = Env.isMac() ? CTRL_SHIFT_Z : CTRL_Y;

  /** String representing the Control modifier key. */
  public static final String CONTROL = Env.isMac() ? "\u2303" : "Ctrl";

  /** String representing the Alt/Option modifier key. */
  public static final String ALT = Env.isMac() ? "\u2325" : "Alt";

  /** String representing the Shift modifier key. */
  public static final String SHIFT = Env.isMac() ? "\u21e7" : "Shift";

  /** String representing the Meta/Command modifier key. */
  public static final String META = Env.isMac() ? "\u2318" : "Meta";

  /** String representing the menu modifier key, either Control or Meta. */
  public static final String MENU = Env.isMac() ? META : CONTROL;

  /** String that separates keys in a shortcut sequence. */
  public static final String SEPARATOR = Env.isMac() ? "" : "+";

  /** String representing the Delete key. */
  public static final String DEL = Env.isMac() ? "\u2326" : "Del";

  /**
   * The map from modifier bitmasks to string representation.
   * The order is significant.
   */
  private static final Map<Integer, String> MODIFIERS = Collections15.linkedHashMap();
  public static final int KEYBOARD_MODIFIERS =
    InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK |
      InputEvent.ALT_GRAPH_DOWN_MASK;

  static {
    MODIFIERS.put(InputEvent.CTRL_MASK, CONTROL);
    MODIFIERS.put(InputEvent.ALT_MASK, ALT);
    MODIFIERS.put(InputEvent.SHIFT_MASK, SHIFT);
    MODIFIERS.put(InputEvent.META_MASK, META);
  }

  /**
   * Returns the string representation of a given combination
   * of modifier keys.
   * @param modifiers An int representing the combination of modifier keys.
   * @return The string representation.
   */
  public static String getModifiersText(int modifiers) {
    final StringBuilder sb = new StringBuilder();

    for(final Map.Entry<Integer, String> e : MODIFIERS.entrySet()) {
      if((modifiers & e.getKey().intValue()) != 0) {
        sb.append(e.getValue()).append(SEPARATOR);
      }
    }

    return sb.toString();
  }

  /**
   * Returns the string representation of a KeyStroke.
   * @param ks The KeyStroke.
   * @return The string representation.
   */
  public static String getKeyStrokeText(KeyStroke ks) {
    final StringBuilder sb = new StringBuilder();

    sb.append(getModifiersText(ks.getModifiers()));
    sb.append(KeyEvent.getKeyText(ks.getKeyCode()));

    return sb.toString();
  }

  /**
   * Returns the string representation of a key stroke
   * by joining the given parts with the SEPARATOR.
   * @param components The component strings of a key stroke,
   * e.g. (MENU, "C") for a typical "copy" key stroke.
   * @return The string representation of a key stroke.
   */
  public static String getKeyStrokeText(String... components) {
    return StringUtil.implode(Arrays.asList(components), SEPARATOR);
  }

  /**
   * Returns the string representation of a
   * Control-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String control(String key) {
    return getKeyStrokeText(CONTROL, key);
  }

  /**
   * Returns the string representation of an
   * Alt-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String alt(String key) {
    return getKeyStrokeText(ALT, key);
  }

  /**
   * Returns the string representation of a
   * Shift-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String shift(String key) {
    return getKeyStrokeText(SHIFT, key);
  }

  /**
   * Returns the string representation of a
   * Meta-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String meta(String key) {
    return getKeyStrokeText(META, key);
  }

  /**
   * Returns the string representation of a
   * Menu-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String menu(String key) {
    return getKeyStrokeText(MENU, key);
  }

  /**
   * Returns the string representation of a
   * Control-Alt-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String controlAlt(String key) {
    return getKeyStrokeText(CONTROL, ALT, key);
  }

  /**
   * Returns the string representation of a
   * Control-Shift-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String controlShift(String key) {
    return getKeyStrokeText(CONTROL, ALT, key);
  }

  /**
   * Returns the string representation of an
   * Alt-Shift-Key shortcut.
   * @param key The key.
   * @return The string representation.
   */
  public static String altShift(String key) {
    return getKeyStrokeText(CONTROL, ALT, key);
  }

  public static KeyStroke ksPlain(int keyCode) {
    return KeyStroke.getKeyStroke(keyCode, 0);
  }

  public static KeyStroke ksMenu(int keyCode) {
    return KeyStroke.getKeyStroke(keyCode, MENU_MASK);
  }

  public static KeyStroke ksShiftMenu(int keyCode) {
    return KeyStroke.getKeyStroke(keyCode, MENU_MASK | KeyEvent.SHIFT_DOWN_MASK);
  }
}