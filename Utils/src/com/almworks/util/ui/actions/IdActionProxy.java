package com.almworks.util.ui.actions;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author dyoma
 */
public class IdActionProxy implements AnAction {
  private final String myId;
  private Map<PresentationKey<?>, Object> myOverriddenPresentation = null;

  public IdActionProxy(String id) {
    this(id, false);
  }

  public IdActionProxy(String id, boolean hideIcon) {
    myId = id;
    if (hideIcon)
      setOverriddenPresentation(PresentationKey.SMALL_ICON, null);
  }

  public <T> void setOverriddenPresentation(PresentationKey<T> key, T value) {
    if (myOverriddenPresentation == null) 
      myOverriddenPresentation = Collections15.hashMap(4, 0.8f);
    key.putTo(myOverriddenPresentation, value);
  }

  public void update(final UpdateContext context) throws CantPerformException {
    AnAction action = getDelegateForUpdate(context, myId);
    if (action == null) return;
    context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, false);
    CantPerformException rethrow = null;
    try {
      action.update(context);
    } catch (CantPerformException e) {
      rethrow = e;
    }
    setShortcut(context, myId);
    if (myOverriddenPresentation != null) {
      for (Map.Entry<PresentationKey<?>,Object> entry : myOverriddenPresentation.entrySet()) {
        context.putPresentationProperty((PresentationKey<Object>) entry.getKey(), entry.getValue());
      }
    }
    if (rethrow != null)
      throw rethrow;
  }

  public static AnAction getDelegateForUpdate(UpdateContext context, String actionId) {
    ActionRegistry registry;
    try {
      registry = context.getSourceObject(ActionRegistry.ROLE);
    } catch (CantPerformException e) {
      context.watchRole(ActionRegistry.ROLE);
      return null;
    }
    AnAction action = registry.getAction(actionId);
    if (action != null) return action;
    context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, true);
    context.putPresentationProperty(PresentationKey.NAME, "");
    context.setEnabled(EnableState.INVISIBLE);
    final UpdateService updateService = context.getUpdateRequest().getUpdateService();
    registry.addListener(context.getUpdateRequest().getLifespan(), actionId, new ActionRegistry.Listener() {
      public void onActionRegister(String actionId, AnAction action) {
        updateService.requestUpdate();
      }
    });
    return null;
  }

  public void perform(ActionContext context) throws CantPerformException {
    AnAction action = getDelegate(context);
    assert action != null : myId;
    action.perform(context);
  }

  public String getId() {
    return myId;
  }

  public String toString() {
    return "IdActionProxy:" + myId;
  }

  @Nullable
  private AnAction getDelegate(ActionContext context) throws CantPerformException {
    return findActionById(context, myId);
  }

  public static void setShortcut(@NotNull UpdateContext context, @Nullable String actionId) {
    final ScopedKeyStroke stroke = getShortcut(context, actionId);
    if(stroke != null) {
      context.putPresentationProperty(PresentationKey.SHORTCUT, stroke.getKeyStroke());
    }
  }

  public static ScopedKeyStroke getShortcut(@NotNull ActionContext context, @Nullable String actionId) {
    if(actionId == null) {
      return null;
    }
    try {
      return context.getSourceObject(ActionRegistry.ROLE).getKeyStroke(actionId);
    } catch (CantPerformException e) {
      return null;
    }
  }

  @Nullable
  public static AnAction findActionById(ActionContext context, String actionId) throws CantPerformException {
    return context.getSourceObject(ActionRegistry.ROLE).getAction(actionId);
  }
}
