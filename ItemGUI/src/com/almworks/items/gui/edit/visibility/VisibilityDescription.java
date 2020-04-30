package com.almworks.items.gui.edit.visibility;

import com.almworks.integers.IntArray;
import com.almworks.util.LogHelper;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VisibilityDescription {
  private final int[] myLevels;
  private final String[] myName;
  private final String[] myTooltips;
  private final int myDefaultLevel;

  private VisibilityDescription(int[] levels, String[] name, String[] tooltips, int defaultLevel) {
    myLevels = levels;
    myName = name;
    myTooltips = tooltips;
    myDefaultLevel = defaultLevel;
  }

  public int getDefault() {
    return myDefaultLevel;
  }

  @Nullable
  public String getLevelName(int level) {
    return getValue(level, myName);
  }

  @Nullable
  public String getTooltip(int level) {
    return getValue(level, myTooltips);
  }

  private String getValue(int level, String[] values) {
    int index = ArrayUtil.indexOf(myLevels, level);
    return index < 0 ? null : values[index];
  }

  public int getNextLevel(int level) {
    int index = ArrayUtil.indexOf(myLevels, level);
    if (index < 0) return myDefaultLevel;
    return index < myLevels.length - 1 ? myLevels[index + 1] : myLevels[0];
  }

  public static class Builder {
    public static final VisibilityDescription EMPTY_DESCRIPTION = new VisibilityDescription(new int[] {0}, new String[] {null}, new String[] {null}, 0);
    private final IntArray myLevels = new IntArray();
    private final List<String> myNames = Collections15.arrayList();
    private final List<String> myTooltips = Collections15.arrayList();
    private int myDefaultLevel = 0;
    
    public Builder addLevel(int level, String name, String tooltip) {
      if (myLevels.contains(level)) LogHelper.error("Duplicated level", level, name, tooltip);
      else if (name == null) LogHelper.error("Missing name", level, tooltip);
      else {
        if (myLevels.isEmpty()) myDefaultLevel = level;
        myLevels.add(level);
        myNames.add(name);
        myTooltips.add(tooltip);
      }
      return this;
    }

    public Builder setDefault(int level) {
      if (myLevels.contains(level)) myDefaultLevel = level;
      else LogHelper.error("Unknown default level", level, myNames);
      return this;
    }

    public VisibilityDescription create() {
      if (myLevels.isEmpty()) {
        LogHelper.error("Not configured");
        return EMPTY_DESCRIPTION;
      }
      return new VisibilityDescription(myLevels.toNativeArray(), myNames.toArray(new String[myNames.size()]), myTooltips.toArray(new String[myTooltips.size()]), myDefaultLevel);
    }
  }
}
