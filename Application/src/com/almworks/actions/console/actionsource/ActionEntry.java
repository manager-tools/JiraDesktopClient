package com.almworks.actions.console.actionsource;

import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Convertors;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.EmptyIcon;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class ActionEntry {
  private static final Icon EMPTY_ICON = new EmptyIcon(16, 16);

  public static final CanvasRenderer<ActionEntry> RENDERER = new CanvasRenderer<ActionEntry>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, ActionEntry item) {
      if (item == null) return;
      canvas.setFontStyle(item.myAction != null ? Font.PLAIN : Font.BOLD);
      canvas.setIcon(Util.NN(item.myIcon, EMPTY_ICON));
      canvas.appendText(item.myDisplayName);
      String description = item.getShortDescription();
      if (description != null) {
        CanvasSection section = canvas.emptySection();
        section.appendText(" ").appendText(description);
        if (!state.isSelected()) section.setForeground(ColorUtil.between(state.getForeground(), state.getBackground(), 0.5f));
        section.setFontStyle(Font.ITALIC);
      }
    }
  };

  public static final Condition<ActionEntry> IS_ACTION = new Condition<ActionEntry>() {
    @Override
    public boolean isAccepted(ActionEntry value) {
      return value != null && value.myAction != null;
    }
  };

  public static final ActionEntry PROTOTYPE = new ActionEntry(AnAction.DEAF, EMPTY_ICON, "Action Name", "Short action description here");

  public static final Convertor<ActionEntry, String> GET_NAME = new Convertor<ActionEntry, String>() {
    @Override
    public String convert(ActionEntry value) {
      if (value == null) return "";
      return Util.NN(value.myDisplayName);
    }
  };

  private static final Comparator<? super ActionEntry> ACTION_COMPARATOR = new Comparator<ActionEntry>() {
    @Override
    public int compare(ActionEntry o1, ActionEntry o2) {
      if (o1 == o2) return 0;
      if (o1 == null || o2 == null) return o1 == null ? -1 : 1;
      int order = o1.myDisplayName.compareToIgnoreCase(o2.myDisplayName);
      if (order != 0) return order;
      order = o1.myDisplayName.compareTo(o2.myDisplayName);
      if (order != 0) return order;
      return Util.NN(o1.myShortDescription).compareTo(Util.NN(o2.myShortDescription));
    }
  };

  private static final Comparator<Pair<ActionEntry, ?>> GROUP_COMPARATOR =
    Containers.convertingComparator(Convertors.superposition(Pair.<ActionEntry, Object>toFirst(), GET_NAME), String.CASE_INSENSITIVE_ORDER);

  private final AnAction myAction;

  private final Icon myIcon;
  private final String myDisplayName;
  private final String myShortDescription;
  ActionEntry(AnAction action, Icon icon, String displayName, String shortDescription) {
    myIcon = icon;
    myDisplayName = displayName;
    myAction = action;
    if (shortDescription != null) {
      shortDescription = shortDescription.trim();
      if (shortDescription.isEmpty()) shortDescription = null;
    }
    myShortDescription = shortDescription;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  @Nullable
  private String getShortDescription() {
    return myShortDescription;
  }

  @Nullable
  public AnAction getAction() {
    return myAction;
  }

  public boolean isGroup() {
    return myAction == null;
  }

  public static List<Pair<ActionEntry, List<ActionEntry>>> collectGroups(List<UpdatedAction.Group> groups) {
    ArrayList<Pair<ActionEntry, List<ActionEntry>>> result = Collections15.arrayList();
    List<ActionEntry> defaultGroup = Collections15.arrayList();
    for (UpdatedAction.Group group : groups) {
      String groupName = group.getNameText();
      List<ActionEntry> actionEntries;
      if (groupName.isEmpty()) actionEntries = defaultGroup;
      else actionEntries = Collections15.arrayList();
      ActionEntry groupEntry = new ActionEntry(null, null, groupName, null);
      for (UpdatedAction.Action action : group.getActions()) {
        String name = action.getNameText();
        if (name.isEmpty()) continue;
        actionEntries.add(new ActionEntry(action.getAction(), action.getIcon(), name, action.getDescription()));
      }
      if (actionEntries.isEmpty()) continue;
      if (actionEntries != defaultGroup) {
        Collections.sort(actionEntries, ACTION_COMPARATOR);
        result.add(Pair.create(groupEntry, actionEntries));
      }
    }
    if (!defaultGroup.isEmpty()) {
      Collections.sort(defaultGroup, ACTION_COMPARATOR);
      result.add(Pair.create(new ActionEntry(null, null, "", null), defaultGroup));
    }
    Collections.sort(result, GROUP_COMPARATOR);
    return result;
  }
}
