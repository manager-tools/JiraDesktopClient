package com.almworks.tags;

import com.almworks.api.misc.FileCollectionBasedIcon;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.enums.IconLoader;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.PresentationMapping;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

public class TagIcons extends IconLoader.ByAttribute {
  static final SerializableFeature<IconLoader> LOADER = new AttributeLoader() {
    @Override
    protected IconLoader createIconLoader(DBAttribute<String> attribute) {
      return new TagIcons(attribute);
    }
  };
  public static final String NO_ICON = ":none:";
  public static final String TAG_ICONS_COLLECTION = "tagicons";
  public static final String FAVORITES_ICONPATH = ":favorites:";
  public static final String UNREAD_ICONPATH = ":unread:";

  TagIcons(DBAttribute<String> resourcePath) {
    super(resourcePath);
  }

  @Nullable
  public static Icon getTagIcon(String iconPath, boolean fixSize) {
    if (iconPath == null || "".equals(iconPath)) {
      return Icons.TAG_DEFAULT;
    } else if (FAVORITES_ICONPATH.equals(iconPath)) {
      return Icons.TAG_FAVORITES;
    } else if (UNREAD_ICONPATH.equals(iconPath)) {
      return Icons.TAG_UNREAD;
    } else if (NO_ICON.equals(iconPath) || isPathInvalid(iconPath)) {
      return PresentationMapping.EMPTY_ICON;
    } else {
      return new FileCollectionBasedIcon(TAG_ICONS_COLLECTION, iconPath, fixSize);
    }
  }

  public static boolean isPathInvalid(String iconPath) {
    return iconPath.indexOf(File.separatorChar) >= 0;
  }

  @Override
  protected Icon loadIcon(DBReader reader, long item, String iconPath) {
    if (iconPath == null) return null;
    return getTagIcon(iconPath, false);
  }
}
