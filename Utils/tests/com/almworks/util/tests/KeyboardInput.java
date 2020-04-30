package com.almworks.util.tests;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

/**
 * @author dyoma
 */
public class KeyboardInput {
  private final JComponent myComponent;

  public KeyboardInput(JComponent component) {
    myComponent = component;
  }

  public void sendKey(final int code) throws InvocationTargetException, InterruptedException {
    sendKey(code, 0);
  }

  public void sendKey(final int code, int modifiers) throws InvocationTargetException, InterruptedException {
    sendEvents(createPressEvent(code, modifiers), createReleaseEvent(code, modifiers));
    GUITestCase.flushAWTQueue();
  }

  public void typeKey(int code, char aChar) throws InvocationTargetException, InterruptedException {
    sendEvents(createPressEvent(code, 0), createReleaseEvent(code, 0), createTypeEvent(aChar));
    GUITestCase.flushAWTQueue();
  }

  private KeyEvent createReleaseEvent(int code, int modifiers) {
    return createEvent(KeyEvent.KEY_RELEASED, code, modifiers);
  }

  private KeyEvent createPressEvent(int code, int modifiers) {
    return createEvent(KeyEvent.KEY_PRESSED, code, modifiers);
  }

  private KeyEvent createTypeEvent(char aChar) {
    return createEvent2(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, aChar, 0);
  }

  private void sendEvents(final KeyEvent ... events)
    throws InterruptedException, InvocationTargetException
  {
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        for (KeyEvent event : events)
          manager.dispatchKeyEvent(event);
      }
    });
  }

  public void sendKey_Down() throws InterruptedException, InvocationTargetException {
    sendKey(KeyEvent.VK_DOWN);
  }

  public void sendKey_Up() throws InterruptedException, InvocationTargetException {
    sendKey(KeyEvent.VK_UP);
  }

  public void sendKey_Esc() throws InterruptedException, InvocationTargetException {
    sendKey(KeyEvent.VK_ESCAPE);
  }

  public void sendKey_Enter() throws InterruptedException, InvocationTargetException {
    sendKey(KeyEvent.VK_ENTER);
  }

  private KeyEvent createEvent(int id, int code, int modifiers) {
    return createEvent2(id, code, (char)0, modifiers);
  }

  private KeyEvent createEvent2(int id, int code, char keyChar, int modifiers) {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    return new KeyEvent(manager.getFocusOwner(), id, System.currentTimeMillis(), modifiers, code, keyChar);
  }
}
