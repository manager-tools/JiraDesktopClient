package com.almworks.tools.tagexporter;

import com.almworks.util.collections.MultiMap;

public class TagInfo {
  public final String name;
  public final String iconPath;
  /** Connection ID => [item key] */
  public final MultiMap<String, String> items;
  public final boolean isJira;

  public TagInfo(String name, String iconPath, MultiMap<String, String> items, boolean isJira) {
    this.name = name;
    this.iconPath = iconPath;
    this.items = items;
    this.isJira = isJira;
  }
}
