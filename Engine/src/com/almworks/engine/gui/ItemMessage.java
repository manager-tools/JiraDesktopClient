package com.almworks.engine.gui;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.util.images.IconHandle;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ItemMessage {
  private static final IconHandle SYNC_PROBLEM = Icons.ARTIFACT_STATE_HAS_SYNC_PROBLEM;
  private static final Icon FATAL = Icons.ACTION_GENERIC_CANCEL_OR_REMOVE;
  private final Icon myIcon;
  private final Color myColor;
  private final String myShortDescription;
  private final String myLongDescription;
  private final AnAction[] myActions;
  private static final Color COLOR_MAJOR = GlobalColors.ERROR_COLOR;//.darker().darker();
  private static final Color COLOR_INFO = Color.BLACK;

  private ItemMessage(Icon icon, String shortDescription, String longDescription, AnAction[] actions, Color color) {
    myIcon = icon;
    myShortDescription = shortDescription;
    myLongDescription = longDescription;
    myColor = color;
    myActions = actions == null || actions.length == 0 ? AnAction.EMPTY_ARRAY : actions;
  }

  public static ItemMessage synchProblem(String shortDescription, String longDescription, @Nullable AnAction[] actions) {
    return new ItemMessage(SYNC_PROBLEM, shortDescription, longDescription, actions, COLOR_MAJOR);
  }

  public static ItemMessage information(Icon icon, String shortDescription, String longDescription, AnAction... actions) {
    return new ItemMessage(icon, shortDescription, longDescription, actions, COLOR_INFO);
  }

  public static ItemMessage fatal(String shortDescription, String longDescription, AnAction... actions) {
    return new ItemMessage(FATAL, shortDescription, longDescription, actions, COLOR_MAJOR);
  }

  public Icon getIcon() {
    return myIcon;
  }

  public String getShortDescription() {
    return myShortDescription;
  }

  public String getLongDescription() {
    return myLongDescription;
  }

  @NotNull
  public AnAction[] getActions() {
    return myActions;
  }

  public boolean hasActions() {
    return myActions.length > 0;
  }

  public Color getColor() {
    return myColor;
  }

  @Nullable
  public static AnAction[] getActions(@Nullable LoadedItemServices itemServices, @Nullable String... ids) {
    if (itemServices == null || ids == null || ids.length == 0) return null;
    ActionRegistry registry = itemServices.getActor(ActionRegistry.ROLE);
    if (registry == null) return null;
    java.util.List<AnAction> actions = Collections15.arrayList();
    for (String id : ids) {
      AnAction action = registry.getAction(id);
      if (action != null) actions.add(action);
    }
    return actions.toArray(new AnAction[actions.size()]);
  }
}
