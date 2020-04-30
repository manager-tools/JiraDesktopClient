package com.almworks.util.ui.actions;

import com.almworks.util.components.AActionButton;
import com.almworks.util.components.AToolbar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class ActionToolbarEntry implements ToolbarEntry {
  private final JComponent myContextComponent;
  private final AnAction myAction;
  @Nullable
  private final Map<String, PresentationMapping<?>> myOverriddenPresentation;


  public ActionToolbarEntry(@NotNull AnAction action) {
    this(action, null, null);
  }

  public ActionToolbarEntry(@NotNull AnAction action, @Nullable JComponent contextComponent,
    @Nullable Map<String, PresentationMapping<?>> overriddenPresentation)
  {
    myAction = action;
    myContextComponent = contextComponent;
    myOverriddenPresentation = overriddenPresentation;
  }

  public static ToolbarEntry create(String actionId) {
    return create(actionId, null, null);
  }

  public static ToolbarEntry create(String actionId, @Nullable Map<String, PresentationMapping<?>> overriddenPresentation) {
    return new ActionToolbarEntry(new IdActionProxy(actionId), null, overriddenPresentation);
  }

  public static ToolbarEntry create(String actionId, @Nullable JComponent contextComponent,
    @Nullable Map<String, PresentationMapping<?>> overriddenPresentation) {
    return new ActionToolbarEntry(new IdActionProxy(actionId), contextComponent, overriddenPresentation);
  }

  public void addToToolbar(AToolbar toolbar) {
    AActionButton button =
      myContextComponent == null ? toolbar.addAction(myAction) : toolbar.addAction(myAction, myContextComponent);
    setupButton(button);
  }

  public void addToPanel(JPanel panel, JComponent contextComponent) {
    AActionButton button =
      AToolbar.createActionButton(myAction, myContextComponent != null ? myContextComponent : contextComponent);
    setupButton(button);
    panel.add(button);
  }

  private void setupButton(AActionButton button) {
    Map<String, PresentationMapping<?>> overridenPresentation = myOverriddenPresentation;
    if (overridenPresentation != null) {
      for (Map.Entry<String, PresentationMapping<?>> entry : overridenPresentation.entrySet()) {
        button.setPresentationMapping(entry.getKey(), entry.getValue());
      }
      if (button.isDisplayable()) {
        button.updateNow();
      }
    }
  }
}
