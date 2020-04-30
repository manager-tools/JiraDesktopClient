package com.almworks.exec;

import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.util.events.FireEventSupport;
import org.almworks.util.detach.Detach;
import org.picocontainer.Startable;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;

/**
 * @author : Dyoma
 */
public class ApplicationManagerImpl implements ApplicationManager, Startable {
  private final FireEventSupport<Listener> myListeners = FireEventSupport.createSynchronized(Listener.class);

  private final WorkArea myWorkArea;
  private int myFocusSwitchCount;

  public ApplicationManagerImpl(WorkArea workArea) {
    myWorkArea = workArea;
  }

  public boolean requestExit() {
    ExitRequest r = new ExitRequest();
    try {
      myListeners.getDispatcher().onExitRequested(r);
    } catch (Exception e) {
      r = new ExitRequest();
    }
    
    if(r.isCancelled()) {
      return false;
    }

    forceExit();
    return true;
  }

  @Override
  public void forceExit() {
    try {
      myListeners.getDispatcher().onBeforeExit();
    } catch (Exception e) {
      // ignore
    }
    try {
      myWorkArea.shutdown();
    } catch (Exception e) {
      // ignore
    }
    System.exit(0);
  }

  public Detach addListener(Listener listener) {
    return myListeners.addStraightListener(listener);
  }

  public void removeListener(Listener listener) {
    myListeners.removeListener(listener);
  }

  public void start() {
    Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
      public void eventDispatched(AWTEvent event) {
        if (event instanceof FocusEvent) {
          FocusEvent e = (FocusEvent) event;
          if (isApplicationSwitched(e)) {
            myFocusSwitchCount++;
          }
        }
      }
    }, AWTEvent.FOCUS_EVENT_MASK);
  }

  private boolean isApplicationSwitched(FocusEvent e) {
    return e.isTemporary() && e.getOppositeComponent() == null;
  }

  public int getFocusSwitchCount() {
    return myFocusSwitchCount;
  }

  public void stop() {
  }
}
