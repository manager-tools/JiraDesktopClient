package com.almworks.sumtable;

import com.almworks.util.config.Configuration;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

class STFilterFormat {
  public static final int ICON_MODE_AUTO_COLOR = 1;
  public static final int ICON_MODE_SELECTED_COLOR = 2;
  public static final int ICON_MODE_BY_FILTER = 3;
  public static final int LABEL_MODE_AUTO = 1;
  public static final int LABEL_MODE_SPECIFIED = 2;

  public static final STFilterFormat DEFAULT =
    new STFilterFormat(true, ICON_MODE_AUTO_COLOR, null, false, LABEL_MODE_AUTO, null);

  private final boolean myUseIcon;
  private final int myIconMode;
  private final Color mySelectedColor;
  private final boolean myUseLabel;
  private final int myLabelMode;
  private final String mySpecifiedLabel;
  private static final String SETTING_USE_ICON = "useIcon";
  private static final String SETTING_ICON_MODE = "iconMode";
  private static final String SETTING_SELECTED_COLOR = "selectedColor";
  private static final String SETTING_USE_LABEL = "useLabel";
  private static final String SETTING_LABEL_MODE = "labelMode";
  private static final String SETTING_SPECIFIED_LABEL = "specifiedLabel";

  public STFilterFormat(boolean useIcon, int iconMode, Color selectedColor, boolean useLabel, int labelMode,
    String specifiedLabel)
  {
    myUseIcon = useIcon;
    myIconMode = iconMode;
    mySelectedColor = selectedColor;
    myUseLabel = useLabel;
    myLabelMode = labelMode;
    mySpecifiedLabel = specifiedLabel;
  }

  public boolean isUseIcon() {
    return myUseIcon;
  }

  public int getIconMode() {
    return myIconMode;
  }

  public Color getSelectedColor() {
    return mySelectedColor;
  }

  public boolean isUseLabel() {
    return myUseLabel;
  }

  public int getLabelMode() {
    return myLabelMode;
  }

  @NotNull
  public String getSpecifiedLabel() {
    return Util.NN(mySpecifiedLabel);
  }

  public void saveTo(Configuration config) {
    config.setSetting(SETTING_USE_ICON, myUseIcon);
    config.setSetting(SETTING_ICON_MODE, myIconMode);
    if (mySelectedColor != null) {
      config.setSetting(SETTING_SELECTED_COLOR, mySelectedColor.getRGB());
    }
    config.setSetting(SETTING_USE_LABEL, myUseLabel);
    config.setSetting(SETTING_LABEL_MODE, myLabelMode);
    if (mySpecifiedLabel != null) {
      config.setSetting(SETTING_SPECIFIED_LABEL, mySpecifiedLabel);
    }
  }

  public static STFilterFormat loadFrom(Configuration config) {
    boolean useIcon = config.getBooleanSetting(SETTING_USE_ICON, false);
    int iconMode = config.getIntegerSetting(SETTING_ICON_MODE, ICON_MODE_AUTO_COLOR);
    if (iconMode != ICON_MODE_AUTO_COLOR && iconMode != ICON_MODE_BY_FILTER && iconMode != ICON_MODE_SELECTED_COLOR) {
      iconMode = ICON_MODE_AUTO_COLOR;
    }
    int selectedColor = config.getIntegerSetting(SETTING_SELECTED_COLOR, -1);
    Color color = (selectedColor >= 0 && selectedColor <= 0xFFFFFF) ? new Color(selectedColor) :  null;
    boolean useLabel = config.getBooleanSetting(SETTING_USE_LABEL, false);
    int labelMode = config.getIntegerSetting(SETTING_LABEL_MODE, -1);
    if (labelMode != LABEL_MODE_AUTO && labelMode != LABEL_MODE_SPECIFIED) {
      labelMode = LABEL_MODE_AUTO;
    }
    String specifiedLabel = config.getSetting(SETTING_SPECIFIED_LABEL, null);
    return new STFilterFormat(useIcon, iconMode, color, useLabel, labelMode, specifiedLabel);
  }
}
