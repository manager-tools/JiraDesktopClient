package com.almworks.api.application;

import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Function;
import com.almworks.util.i18n.Local;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;

public class StateIcon {
  public static final Comparator<StateIcon> ORDER_BY_PRIORITY = new Comparator<StateIcon>() {
    public int compare(StateIcon o1, StateIcon o2) {
      return Containers.compareInts(o1.getPriority(), o2.getPriority());
    }
  };

  @NotNull
  private final Icon myIcon;
  @Nullable
  private final String myTooltipPattern;
  @Nullable
  private final Function<LoadedItem, String> myTooltipGetter;
  private final int myPriority;

  public StateIcon(Icon icon, int priority, String tooltipPattern,Function<LoadedItem, String> tooltipGetter) {
    myIcon = icon;
    myPriority = priority;
    myTooltipPattern = tooltipPattern;
    myTooltipGetter = tooltipGetter;
  }

  public StateIcon(Icon icon, int priority) {
    this(icon, priority, null);
  }

  public StateIcon(Icon icon, int priority, String tooltipPattern) {
    this(icon, priority, tooltipPattern, null);
  }

  public Icon getIcon() {
    return myIcon;
  }

  @Nullable
  public String getTooltip(@Nullable LoadedItem loadedItem) {
    StringBuilder result = new StringBuilder();
    if (myTooltipPattern != null)
      result.append(Local.parse(myTooltipPattern));
    if (loadedItem != null && myTooltipGetter != null) {
      if (result.length() > 0)
        result.append(": ");
      result.append(myTooltipGetter.invoke(loadedItem));
    }
    return result.toString();
  }

  public int getPriority() {
    return myPriority;
  }
}
