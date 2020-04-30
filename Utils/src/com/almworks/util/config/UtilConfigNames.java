package com.almworks.util.config;

public class UtilConfigNames {
  public static final String SORTED_REVERSE = "reverseSort";
  public static final String COLUMN = "column";
  public static final String ID = "id";
  public static final String ORDER = "order";
  public static final String WIDTH = "width";
  public static final String WIDTH_KEY = "width";
  public static final String HEIGHT_KEY = "height";
  public static final String OLD_WIDTH_KEY = "oldwidth";
  public static final String OLD_HEIGHT_KEY = "oldheight";
  public static final String MAXIMIZED = "maximized";
  public static final String FULLSCREEN = "fullscreen";
  public static final String POSITION_KEY = "position";
  public static final String POSITION_KEY_MULTIDISPLAYED = "position.md";
  public static final String DIMENSION_KEY = "dimension";
  public static final String X_KEY = "x";
  public static final String Y_KEY = "y";
  public static final String OLD_X_KEY = "oldx";
  public static final String OLD_Y_KEY = "oldy";
  public static final String EXPANDED = "expanded";
  public static final String SELECTED = "selected";
  public static final String TREE_EXPANSION_KEY = "treeExpansion";

  public static void register() {
    MediumOptimization.addInternedNames(SORTED_REVERSE, COLUMN, ID, ORDER, WIDTH, WIDTH_KEY, HEIGHT_KEY, OLD_HEIGHT_KEY,
      OLD_WIDTH_KEY, MAXIMIZED, FULLSCREEN, POSITION_KEY, POSITION_KEY_MULTIDISPLAYED, DIMENSION_KEY, X_KEY, Y_KEY, OLD_X_KEY,
      OLD_Y_KEY, EXPANDED, SELECTED, TREE_EXPANSION_KEY);
  }
}
