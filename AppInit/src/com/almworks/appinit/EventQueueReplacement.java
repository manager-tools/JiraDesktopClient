package com.almworks.appinit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dyoma
 */
public class EventQueueReplacement extends EventQueue {
  //  @Nullable
  private static EventQueueReplacement INSTANCE;
  private final java.util.List<AWTEventPreprocessor> myPreprocessors = new ArrayList();
  private AWTEventPreprocessor[] myArray;

  private EventQueueReplacement() {
  }

  //  @ThreadSafe
  public void addPreprocessor(final AWTEventPreprocessor preprocessor) {
    synchronized (myPreprocessors) {
      myPreprocessors.add(preprocessor);
      resetArray();
    }
  }

  private void resetArray() {
    int size = myPreprocessors.size();
    myArray = size == 0 ? null : myPreprocessors.toArray(new AWTEventPreprocessor[size]);
  }

  //  @ThreadSafe
  public boolean removePreprocessor(AWTEventPreprocessor preprocessor) {
    synchronized (myPreprocessors) {
      boolean result = myPreprocessors.remove(preprocessor);
      resetArray();
      return result;
    }
  }

  protected void dispatchEvent(AWTEvent event) {
    boolean consumed = false;
    AWTEventPreprocessor[] array = myArray;
    if (array != null) {
      for (AWTEventPreprocessor preprocessor : array) {
        consumed = preprocessor.preprocess(event, consumed);
        if (consumed) {
          break;
        }
      }
    }
    if (!consumed) {
      super.dispatchEvent(event);
    }
    consumed = consumed || ((event instanceof InputEvent) && ((InputEvent) event).isConsumed());
    if (array != null && !consumed) {
      for (AWTEventPreprocessor processor : array) {
        consumed = processor.postProcess(event, consumed);
        if (consumed) break;
      }
    }
  }

  //  @NotNull
  public static EventQueueReplacement ensureInstalled() {
    synchronized (EventQueueReplacement.class) {
      if (INSTANCE != null) return INSTANCE;
    }
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        synchronized (EventQueueReplacement.class) {
          if (INSTANCE == null) {
            EventQueueReplacement queue = new EventQueueReplacement();
            Toolkit.getDefaultToolkit().getSystemEventQueue().push(queue);
            INSTANCE = queue;
          }
        }
      }
    };
    if (EventQueue.isDispatchThread()) {
      // Install event queue in separate invocation event. Otherwise previous AWT thread become not-AWT, so AWT confined object may be accessed from two threads (old AWT and new AWT).
      // Code in both threads may check the thread and get the confirmation.
      // To avoid such problems:
      // 1. Install event queue from current AWT thread. This ensures that no other code is executed in AWT thread, no AWT confined objects can be accessed right during event queue
      // replacement.
      // 2. Install event queue in separate invocation event. Avoid any activity before and especially after event queue replacement. Since this thread is not AWT thread any more.
      // This check logs error if it can not control right event queue replacement.
      Logger.getLogger("").log(Level.WARNING, "Install event queue from not dispatch thread", new Throwable());
      runnable.run();
    }
    else try {
      SwingUtilities.invokeAndWait(runnable);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    synchronized (EventQueueReplacement.class) {
      return INSTANCE;
    }
  }

  public static void detachPreprocessor(AWTEventPreprocessor preprocessor) {
    EventQueueReplacement instance;
    synchronized (EventQueueReplacement.class) {
      instance = INSTANCE;
      if (instance == null)
        return;
    }
    instance.removePreprocessor(preprocessor);
  }
}
