package com.almworks.actions.console.actionsource;

import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationKey;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class UpdatedAction {
  private final Map<PresentationKey<?>, Object> myPresentation;

  public UpdatedAction(Map<PresentationKey<?>, Object> presentation) {
    myPresentation = presentation;
  }

  public <T> T getValue(PresentationKey<T> key) {
    return key.getFrom(myPresentation);
  }

  public String getDescription() {
    return getValue(PresentationKey.SHORT_DESCRIPTION);
  }

  public Icon getIcon() {
    return getValue(PresentationKey.SMALL_ICON);
  }

  @NotNull
  public String getNameText() {
    String string = getValue(PresentationKey.NAME);
    if (string == null) return "";
    NameMnemonic nameMnemonic = NameMnemonic.parseString(string);
    return nameMnemonic.getText().trim();
  }

  public static class Action extends UpdatedAction {
    private final AnAction myAction;

    public Action(AnAction action, Map<PresentationKey<?>, Object> presentation) {
      super(presentation);
      myAction = action;
    }

    public AnAction getAction() {
      return myAction;
    }
  }

  public static class Group extends UpdatedAction {
    private final ActionGroup myGroup;
    private final List<Action> myActions;

    public Group(ActionGroup group, Map<PresentationKey<?>, Object> presentation, List<Action> actions) {
      super(presentation);
      myGroup = group;
      myActions = actions;
    }

    public List<Action> getActions() {
      return myActions;
    }
  }
}
