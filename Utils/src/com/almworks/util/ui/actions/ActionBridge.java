package com.almworks.util.ui.actions;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.BottleneckJobs;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class ActionBridge implements UpdateService {
  private final NextUpdateManager myNextUpdate;
  private DetachComposite myDetach;
  private boolean myUptodate = false;
  private final JComponent myContextComponent;
  private final Action myPresentation = new FastAction(this);
  private final AnAction myAction;
  private final Map<String, PresentationMapping<?>> myMapping = Collections15.hashMap();

  public ActionBridge(AnAction action, JComponent contextComponent) {
    assert contextComponent != null;
    myAction = action;
    myContextComponent = contextComponent;
    myNextUpdate = new NextUpdateManager(this);
    PresentationMapping.setupDefaultMapping(myMapping);
  }

  public void setPresentationMapping(@NotNull String swingValue, @NotNull PresentationMapping<?> mapping) {
    myMapping.put(swingValue, mapping);
  }

  public void overridePresentation(Map<String, PresentationMapping<?>> mapping) {
    for (Map.Entry<String, PresentationMapping<?>> entry : mapping.entrySet()) {
      setPresentationMapping(entry.getKey(), entry.getValue());
    }
  }

  public void startUpdate() {
    myUptodate = false;
    assert myDetach == null;
    myDetach = new DetachComposite();
    myNextUpdate.start(myDetach);
    if (ThreadGate.isRightNow(ThreadGate.AWT))
      updateNow();
    else
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          updateNow();
        }
      });
  }

  public void updateNow() {
    if (!isStarted()) {
      updateNow(DefaultUpdateContext.singleUpdate(myContextComponent));
      return;
    }
    UpdateContext context = myNextUpdate.prepareUpdateContext(myContextComponent);
    try {
      updateNow(context);
      myUptodate = true;
    } finally {
      myNextUpdate.updateComplete();
    }
  }

  public void performAction() {
    String actionName = (String) getPresentation().getValue(Action.NAME);
    assert isEnabled() : actionName;
    ActionUtil.performAction(myAction, myContextComponent);
  }

  public boolean isStarted() {
    return myNextUpdate.isStarted();
  }

  public DetachComposite getDetach() {
    return myDetach;
  }

  public void stopUpdate() {
    if (myDetach != null) {
      myDetach.detach();
      myDetach = null;
    }
  }

  public JComponent getContextComponent() {
    return myContextComponent;
  }

  public void addDetach(Detach detach) {
    myDetach.add(detach);
  }

  public boolean performIfEnabled() {
    try {
      startUpdate();
      if (isEnabled()) {
        performAction();
        return true;
      }
    } finally {
      stopUpdate();
    }
    return false;
  }

  public void requestUpdate() {
    if (!myUptodate)
      return;
    addToUpdate(this);
    myUptodate = false;
  }

  public boolean isUpToDate() {
    return myUptodate;
  }

  public void ensureUpToDate() {
    if (isUpToDate())
      return;
    updateNow();
  }

  private static final BottleneckJobs<ActionBridge> ourToUpdate = new BottleneckJobs<ActionBridge>(10, ThreadGate.AWT) {
    protected void execute(ActionBridge job) {
      try {
        job.ensureUpToDate();
      } catch (Exception e) {
        Log.error(e);
      }
    }
  };

  private static void addToUpdate(ActionBridge action) {
    ourToUpdate.addJobDelayed(action);
  }

  public static void updateActionsNow() {
    ourToUpdate.executeJobsNow();
  }

  public Action getPresentation() {
    return myPresentation;
  }

  public AnAction getAction() {
    return myAction;
  }

  public boolean isVisible() {
    Boolean value = (Boolean) myPresentation.getValue(PresentationKey.ACTION_KEY_VISIBLE);
    assert value != null : myPresentation.getValue(Action.NAME);
    return value;
  }

  public boolean isAvailable() {
    Boolean value = (Boolean) myPresentation.getValue(PresentationKey.ACTION_KEY_NOT_AVALIABLE);
    return value == null || !value;
  }

  public boolean isEnabled() {
    boolean enabled = myPresentation.isEnabled();
    assert !enabled || isVisible() : myPresentation.getValue(Action.NAME);
    return enabled;
  }

  private void updateNow(UpdateContext context) {
    ActionUtil.performUpdate(getAction(), context);
    flushUpdate(context);
    onUpdated();
  }

  protected void onUpdated() {
  }

  private void flushUpdate(UpdateContext context) {
    Map<PresentationKey<?>, Object> values = context.getAllValues();
    PresentationMapping<Boolean> enableMapping = (PresentationMapping<Boolean>) myMapping.get(PresentationKey.ACTION_KEY_ENABLE);
    if (enableMapping != null) {
      Boolean value = enableMapping.getSwingValue(PresentationKey.ACTION_KEY_ENABLE, values);
      myPresentation.setEnabled(value != null && value);
    } else {
      assert false : myAction;
    }
    for (Map.Entry<String, PresentationMapping<?>> entry : myMapping.entrySet()) {
      PresentationMapping<?> mapping = entry.getValue();
      String swingKey = entry.getKey();
      Object swingValue = mapping.getSwingValue(swingKey, values);
      myPresentation.putValue(swingKey, swingValue);
    }
  }

  public void setAllMappings(Map<String, PresentationMapping<?>> mappings) {
    myMapping.clear();
    myMapping.putAll(mappings);
  }

  public String toString() {
    return "AB:" + myAction;
  }

  private static class FastAction implements Action {
    private final ActionBridge myBridge;
    private final Map<Object, Object> myValues = Collections15.hashMap();
    private final SwingPropertyChangeSupport myListeners = new SwingPropertyChangeSupport(this);
    private boolean myEnabled = false;

    public FastAction(ActionBridge bridge) {
      myBridge = bridge;
    }

    public boolean isEnabled() {
      return myEnabled;
    }

    public void setEnabled(boolean b) {
      if (myEnabled == b)
        return;
      boolean oldEnabled = myEnabled;
      myEnabled = b;
      fireChanged("enabled", Boolean.valueOf(oldEnabled), Boolean.valueOf(myEnabled));
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
      myListeners.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
      myListeners.removePropertyChangeListener(listener);
    }

    public Object getValue(String key) {
      return myValues.get(key);
    }

    public void putValue(String key, Object value) {
      Object oldValue = myValues.get(key);
      if (Util.equals(oldValue, value))
        return;
      if (value == null)
        myValues.remove(key);
      else
        myValues.put(key, value);
      fireChanged(key, oldValue, value);
    }

    public void actionPerformed(ActionEvent event) {
      myBridge.performAction();
    }

    private void fireChanged(String name, Object old, Object newValue) {
      myListeners.firePropertyChange(name, old, newValue);
    }
  }
}
