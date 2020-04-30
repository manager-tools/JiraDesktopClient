package com.almworks.store.sqlite;

import com.almworks.api.gui.DialogManager;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Log;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Random;

public class CrashSQLiteAction extends SimpleAction {
  static volatile SQLiteConnection ourConnection;

  private final Random myRandom = new Random();

  public CrashSQLiteAction() {
    super("Crash SQLite");
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(
      ourConnection != null
        && getHandle(ourConnection) != null
        && getCache(ourConnection) != null);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final Rectangle rect = garblePointers();
    final String message = String.format(
      "Garbled pointers in %d of %d connections, %d of %d statements",
      rect.y, rect.x, rect.height, rect.width);
    Log.warn(message);
    context.getSourceObject(DialogManager.ROLE).showMessage("Crash SQLite", message, JOptionPane.INFORMATION_MESSAGE);
  }

  private Rectangle garblePointers() {
    final Rectangle rect = new Rectangle(0, 0, 0, 0);
    if(ourConnection != null) {
      garbleConnection(rect);
      garbleStatements(rect);
    }
    return rect;
  }

  private void garbleConnection(Rectangle rect) {
    rect.x++;
    final Object handle = getHandle(ourConnection);
    if(handle != null) {
      if(garblePointer(handle)) {
        rect.y++;
      }
    }
  }

  private Object getHandle(SQLiteConnection conn) {
    return getFieldValue(conn, "myHandle");
  }

  private void garbleStatements(Rectangle rect) {
    final Object cache = getCache(ourConnection);
    if(cache != null) {
      final Object values = callMethod(cache, "values");
      if(values != null) {
        final Object oit = callMethod(values, "iterator");
        if(oit instanceof Iterator) {
          final Iterator it = (Iterator)oit;
          while(it.hasNext()) {
            rect.width++;
            if(garblePointer(it.next())) {
              rect.height++;
            }
          }
        }
      }
    }
  }

  private Object getCache(SQLiteConnection conn) {
    return getFieldValue(conn, "myStatementCache");
  }

  private Object getFieldValue(Object obj, String name) {
    final Field field = getField(obj, name);
    if(field != null) {
      try {
        return field.get(obj);
      } catch (IllegalAccessException e) {
        Log.warn(e);
      }
    }
    return null;
  }

  private Field getField(Object obj, String name) {
    try {
      final Field field = obj.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch(NoSuchFieldException e) {
      Log.warn(e);
    }
    return null;
  }

  private Object callMethod(Object obj, String name) {
    final Method method = getMethod(obj, name);
    if(method != null) {
      try {
        return method.invoke(obj);
      } catch (IllegalAccessException e) {
        Log.warn(e);
      } catch (InvocationTargetException e) {
        Log.warn(e);
      }
    }
    return null;
  }

  private Method getMethod(Object obj, String name) {
    try {
      final Method method = obj.getClass().getMethod(name);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      Log.warn(e);
    }
    return null;
  }

  private boolean garblePointer(Object handle) {
    try {
      final Field field = getField(handle, "swigCPtr");
      if(field != null) {
        final long oldVal = field.getLong(handle);
        while(true) {
          final long newVal = makeGarbage(oldVal);
          if(newVal != oldVal) {
            field.setLong(handle, newVal);
            return true;
          }
        }
      }
    } catch(IllegalAccessException e) {
      Log.warn(e);
    }
    return false;
  }

  private long makeGarbage(long oldValue) {
    if(oldValue > 0) {
      return myRandom.nextLong() & oneMask(oldValue);
    }
    return myRandom.nextLong() & oldValue;
  }

  private long oneMask(long val) {
    long mask = 0L;
    long b = Long.highestOneBit(val);
    while(b != 0) {
      mask |= b;
      b >>= 1;
    }
    return mask;
  }
}
