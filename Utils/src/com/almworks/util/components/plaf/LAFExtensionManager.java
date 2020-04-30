package com.almworks.util.components.plaf;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class LAFExtensionManager {
  private static final LAFExtensionManager INSTANCE = new LAFExtensionManager();

  private final List<LAFExtension> myExtensions = Collections15.arrayList();
  private PropertyChangeListener myListener;

  public static void installExtension(final LAFExtension extension) {
    assert extension != null;
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        INSTANCE.install(extension);
      }
    });
  }

  private void changeLAF(LAFExtension extension, LookAndFeel oldLAF, LookAndFeel newLAF) {
    if (oldLAF != null) {
      if (extension.isExtendingLookAndFeel(oldLAF))
        try {
          extension.uninstall(oldLAF);
        } catch (Exception e) {
          Log.error(e);
        }
    }
    if (newLAF != null) {
      if (extension.isExtendingLookAndFeel(newLAF))
        try {
          extension.install(newLAF);
        } catch (Exception e) {
          Log.error(e);
        }
    }
  }

  private void changeLAF(LookAndFeel oldLAF, LookAndFeel newLAF) {
    LAFExtension[] extensions = myExtensions.toArray(new LAFExtension[myExtensions.size()]);
    for (int i = 0; i < extensions.length; i++)
      changeLAF(extensions[i], oldLAF, newLAF);
  }

  private void install(LAFExtension extension) {
    if (extension == null)
      return;
    myExtensions.add(extension);
    changeLAF(extension, null, UIManager.getLookAndFeel());
    maybeSubscribe();
  }

  private void maybeSubscribe() {
    if (myListener == null) {
      myListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt != null) {
            if (evt.getPropertyName().equals("lookAndFeel")) {
              changeLAF((LookAndFeel)evt.getOldValue(), (LookAndFeel)evt.getNewValue());
            }
          }
        }
      };
      UIManager.addPropertyChangeListener(myListener);
    }
  }
}
