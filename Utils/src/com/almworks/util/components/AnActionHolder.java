package com.almworks.util.components;

import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.ActionBridge;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.IdActionProxy;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * @author dyoma
 */
public class AnActionHolder {
  private final Lifecycle myLife = new Lifecycle(false);
  private final AbstractButton myActionComponent;
  private final Map<String, PresentationMapping<?>> myMappings = Collections15.hashMap();
  @Nullable
  private JComponent myContextComponent;
  @Nullable
  private ActionBridge myBridge;

  public AnActionHolder(AbstractButton actionComponent) {
    assert actionComponent != null;
    myActionComponent = actionComponent;
    PresentationMapping.setupDefaultMapping(myMappings);
  }

  public void ensureUpToDate() {
    if (myBridge != null)
      myBridge.ensureUpToDate();
  }

  public void startUpdate() {
    if (myLife.cycleStart() && myBridge != null)
      myBridge.startUpdate();
  }

  public void stopUpdate() {
    boolean wasStarted = myLife.cycleEnd();
    if (!wasStarted || myBridge == null)
      return;
    myBridge.stopUpdate();
    unregisterKeyStroke();
  }

  public void updateNow() {
    if (myBridge != null)
      myBridge.updateNow();
  }

  public void setContextComponent(@Nullable JComponent component) {
    myContextComponent = component;
    if (myBridge != null)
      setAnAction(myBridge.getAction());
  }

  @NotNull
  public JComponent getContentComponent() {
    return myContextComponent != null ? myContextComponent : myActionComponent;
  }

  @NotNull
  public AbstractButton getActionComponent() {
    return myActionComponent;
  }

  public void performAction() {
    ActionBridge bridge = myBridge;
    if (bridge != null) {
      bridge.updateNow();
      if (bridge.isEnabled())
        bridge.performAction();
    }
  }

  @NotNull
  public Detach setAnAction(@NotNull AnAction action) {
    boolean started = myLife.isCycleStarted();
    if (myBridge != null && started)
      myBridge.stopUpdate();
    ActionBridge newBridge = new ActionBridge(action, getContentComponent());
    myBridge = newBridge;
    newBridge.setAllMappings(myMappings);
    if (started)
      newBridge.startUpdate();
    myActionComponent.setAction(newBridge.getPresentation());
    return new Detach() {
      protected void doDetach() {
        setAnAction(AnAction.DEAF);
      }
    };
  }

  public Detach setActionById(String actionId) {
    return setAnAction(new IdActionProxy(actionId));
  }

  private static final ComponentProperty<KeyStroke> KEY_STROKE = ComponentProperty.createProperty("keyStroke");
  private static final Object BUTTON_ACTION_NAME = "$$$buttonAction$$$";

  public void registerKeyStroke(KeyStroke stroke) {
    AbstractButton button = myActionComponent;
    InputMap inputMap = button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final KeyStroke prevStroke = KEY_STROKE.getClientValue(button);
    if (prevStroke != null)
      inputMap.remove(prevStroke);
    KEY_STROKE.putClientValue(button, stroke);
    if (stroke != null)
      inputMap.put(stroke, BUTTON_ACTION_NAME);
  }

  public void setSwingAction(Action action) {
    myActionComponent.getActionMap().put(BUTTON_ACTION_NAME, action);
  }

  private void unregisterKeyStroke() {
    registerKeyStroke(null);
  }

  public KeyStroke[] getRegisteredKeyStrokes(KeyStroke[] registered) {
    KeyStroke stroke = KEY_STROKE.getClientValue(myActionComponent);
    if (stroke == null)
      return registered;
    int index = CollectionUtil.indexOf(registered, stroke);
    KeyStroke[] result;
    if (index == -1) {
      result = new KeyStroke[registered.length + 1];
      System.arraycopy(registered, 0, result, 1, registered.length);
    } else {
      System.arraycopy(registered, 0, registered, 1, index);
      result = registered;
    }
    result[0] = stroke;
    return result;
  }

  public int getConditionForKeyStroke(KeyStroke aKeyStroke, int defaultResult) {
    if (Util.equals(aKeyStroke, KEY_STROKE.getClientValue(myActionComponent)))
      return JComponent.WHEN_IN_FOCUSED_WINDOW;
    return defaultResult;
  }

  public void setPresentationMapping(String swingKey, PresentationMapping<?> mapping) {
    myMappings.put(swingKey, mapping);
    ActionBridge bridge = myBridge;
    if (bridge != null)
      bridge.setPresentationMapping(swingKey, mapping);
  }

  public void overridePresentation(Map<String, PresentationMapping> mapping) {
    for (Map.Entry<String, PresentationMapping> entry : mapping.entrySet()) {
      setPresentationMapping(entry.getKey(), entry.getValue());
    }
  }
}
