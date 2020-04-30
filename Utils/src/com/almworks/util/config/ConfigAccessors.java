package com.almworks.util.config;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public class ConfigAccessors {
  // todo refactor

  private final Configuration myConfiguration;
  private static final int DEFAULT_WIDTH = 100;
  private static final int DEFAULT_HEIGHT = 80;

  private static final int DEFAULT_X = 50;
  private static final int DEFAULT_Y = 50;

  public ConfigAccessors(Configuration configuration) {
    myConfiguration = configuration;
  }

  public static ConfigAccessors dimension(Configuration configuration) {
    return new ConfigAccessors(configuration.getOrCreateSubset(UtilConfigNames.DIMENSION_KEY));
  }

  /**
   * Returns config accessors for point configuration. Two types of configuration are kept - for
   * multi-display environment and for single-display environment. If the user changes environment,
   * positions will be kept separately.
   */
  public static ConfigAccessors position(Configuration configuration, boolean multiDisplay) {
    // key denotes subset that should contain position for the given environment (multiDisplay)
    // otherKey denotes subset for "other" environment
    String key = multiDisplay ? UtilConfigNames.POSITION_KEY_MULTIDISPLAYED : UtilConfigNames.POSITION_KEY;
    String otherKey = !multiDisplay ? UtilConfigNames.POSITION_KEY_MULTIDISPLAYED : UtilConfigNames.POSITION_KEY;

    // if there are no settings for the current environment, copy those from the other env.
    SubMedium<Medium> subsets = configuration.subsets();
    if (!subsets.isSet(key) && subsets.isSet(otherKey)) {
      ConfigurationUtil.copyTo(configuration.getSubset(otherKey), configuration.getOrCreateSubset(key));
    }

    return new ConfigAccessors(configuration.getOrCreateSubset(key));
  }

  @Nullable
  public static Icon icon(ReadonlyConfiguration configuration) {
    return null;
  }

  protected void setInt(String setting, int value) {
    myConfiguration.setSetting(setting, String.valueOf(value));
  }

  protected int getInt(String setting, int defaultValue) {
    String value = myConfiguration.getSetting(setting, "unknown");
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public void setDimension(Dimension dimension) {
    setInt(UtilConfigNames.WIDTH_KEY, dimension.width);
    setInt(UtilConfigNames.HEIGHT_KEY, dimension.height);
  }

  public Dimension getDimension() {
    return new Dimension(getWidth(), getHeight());
  }

  public int getWidth() {
    return getInt(UtilConfigNames.WIDTH_KEY, DEFAULT_WIDTH);
  }

  public int getHeight() {
    return getInt(UtilConfigNames.HEIGHT_KEY, DEFAULT_HEIGHT);
  }

  public void setOldDimension(Dimension dimension) {
    setInt(UtilConfigNames.OLD_WIDTH_KEY, dimension.width);
    setInt(UtilConfigNames.OLD_HEIGHT_KEY, dimension.height);
  }

  public Dimension getOldDimension() {
    return new Dimension(getInt(UtilConfigNames.OLD_WIDTH_KEY, DEFAULT_WIDTH), getInt(UtilConfigNames.OLD_HEIGHT_KEY, DEFAULT_HEIGHT));
  }

  public void setPoint(Point point) {
    setInt(UtilConfigNames.X_KEY, point.x);
    setInt(UtilConfigNames.Y_KEY, point.y);
  }

  public Point getPoint() {
    return new Point(getInt(UtilConfigNames.X_KEY, DEFAULT_X), getInt(UtilConfigNames.Y_KEY, DEFAULT_Y));
  }

  public void setOldPoint(Point point) {
    setInt(UtilConfigNames.OLD_X_KEY, point.x);
    setInt(UtilConfigNames.OLD_Y_KEY, point.y);
  }

  public Point getOldPoint() {
    return new Point(getInt(UtilConfigNames.OLD_X_KEY, DEFAULT_X), getInt(UtilConfigNames.OLD_Y_KEY, DEFAULT_Y));
  }


  public void setMaximized(boolean b) {
    myConfiguration.setSetting(UtilConfigNames.MAXIMIZED, String.valueOf(b));
  }

  public boolean isMaximized() {
    return Boolean.parseBoolean(myConfiguration.getSetting(UtilConfigNames.MAXIMIZED, "false"));
  }

  public void setFullScreen(boolean b) {
    myConfiguration.setSetting(UtilConfigNames.FULLSCREEN, String.valueOf(b));
  }

  public boolean isFullScreen() {
    return Boolean.parseBoolean(myConfiguration.getSetting(UtilConfigNames.FULLSCREEN, "false"));
  }

  public static Int integer(Configuration configuration, int defaultValue, String settingName) {
    return new Int(configuration, defaultValue, settingName);
  }

  public static Bool bool(Configuration configuration, String settingName, boolean defaultValue) {
    return new Bool(configuration, settingName, defaultValue);
  }

  public Dimension getDimensionOrNull() {
    int width = getInt(UtilConfigNames.WIDTH_KEY, 0);
    int height = getInt(UtilConfigNames.HEIGHT_KEY, 0);
    return (width <= 0 || height <= 0) ? null : new Dimension(width, height);
  }

  public Point getPointOrNull() {
    int x = getInt(UtilConfigNames.X_KEY, Short.MIN_VALUE);
    int y = getInt(UtilConfigNames.Y_KEY, Short.MIN_VALUE);
    return (x <= Short.MIN_VALUE || y <= Short.MIN_VALUE) ? null : new Point(x, y);
  }

  public static class Int extends ConfigAccessors {
    private final int myDefaultValue;
    private final String mySettingName;

    public Int(Configuration configuration, int defaultValue, String settingName) {
      super(configuration);
      assert settingName != null;
      myDefaultValue = defaultValue;
      mySettingName = settingName;
    }

    public int getInt() {
      return getInt(mySettingName, myDefaultValue);
    }

    public void setInt(int value) {
      setInt(mySettingName, value);
    }
  }

  public static class Bool extends ConfigAccessors {
    private final boolean myDefaultValue;
    private final String mySettingName;

    public Bool(Configuration configuration, String settingName, boolean defaultValue) {
      super(configuration);
      myDefaultValue = defaultValue;
      mySettingName = settingName;
    }

    public boolean getBool() {
      return getInt(mySettingName, myDefaultValue ? 1 : 0) == 1;
    }

    public void setBool(boolean bool) {
      setInt(mySettingName, bool ? 1 : 0);
    }
  }
}
