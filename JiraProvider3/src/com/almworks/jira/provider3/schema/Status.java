package com.almworks.jira.provider3.schema;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.enums.IconLoader;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerStatus;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.ui.ColorUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Component;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Status {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerStatus.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerStatus.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerStatus.NAME);
  public static final DBAttribute<Set<Long>> ONLY_IN_PROJECTS = ServerJira.toLinkSetAttribute(
    ServerStatus.ONLY_IN_PROJECTS);
  public static final DBAttribute<String> DESCRIPTION = ServerJira.toScalarAttribute(ServerStatus.DESCRIPTION);
  public static final DBAttribute<Long> CATEGORY = ServerJira.toLinkAttribute(ServerStatus.CATEGORY);

  private static final DBIdentity FEATURE_ICON_CATEGORY_COLOR = Jira.feature("status.categoryColorIcon");
  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .narrowByAttribute(Issue.PROJECT, ONLY_IN_PROJECTS)
    .renderFirstNotNull(NAME, ID)
    .setIconLoader(new ScalarSequence.Builder().append(FEATURE_ICON_CATEGORY_COLOR).appendSubsequence(EnumTypeBuilder.ICON_BY_URL).create())
    .create();

  public static void registerFeature(FeatureRegistry registry) {
    registry.register(FEATURE_ICON_CATEGORY_COLOR, CategoryIcon.LOADER);
  }

  private static class CategoryIcon implements IconLoader {
    public static final SerializableFeature<IconLoader> LOADER = new SerializableFeature<IconLoader>() {
      @Override
      public IconLoader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        IconLoader background = SerializedObjectAttribute.restore(reader, stream.nextSubstream(), getValueClass(), invalidate);
        return new CategoryIcon(background);
      }

      @Override
      public Class<IconLoader> getValueClass() {
        return IconLoader.class;
      }
    };
    private IconLoader myBackground;
    private static final Map<String, Color> COLOR_NAMES;
    static {
      HashMap<String, Color> map = Collections15.hashMap();
      map.put("green", new Color(20, 137, 44));
      map.put("yellow", new Color(246, 195, 66));
      map.put("blue-gray", new Color(74, 103, 133));
      COLOR_NAMES = Collections.unmodifiableMap(map);
    }

    public CategoryIcon(IconLoader background) {
      myBackground = background;
    }

    @Override
    public Icon loadIcon(DBReader reader, long item) {
      Long category = CATEGORY.getValue(item, reader);
      String colorName = category != null && category > 0 ? StatusCategory.COLOR_NAME.getValue(category, reader) : null;
      Icon bgIcon = myBackground != null ? myBackground.loadIcon(reader, item) : null;
      return colorName != null ? iconByColorName(colorName, bgIcon) : bgIcon;
    }

//    @Contract("_,!null->!null")
    private Icon iconByColorName(String colorName, Icon bgIcon) {
      Color color = COLOR_NAMES.get(colorName);
      if (color == null) {
        LogHelper.warning("Unknown status category name:", colorName);
        return bgIcon;
      }
      if (bgIcon == null) return createDotIcon(color);
      return new ColorOverlayIcon(bgIcon, color);
    }

    private ImageIcon createDotIcon(Color color) {
      BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      g.setColor(color);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.fillOval(2, 3, 11, 10);
      return new ImageIcon(image);
    }
  }

  private static class ColorOverlayIcon implements Icon {
    @NotNull
    private final Icon myBackground;
    private final Color myColor;
    @Nullable
    private volatile Icon myActual;

    public ColorOverlayIcon(@NotNull Icon background, @NotNull Color color) {
      myBackground = background;
      myColor = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      ensureHasActual(c).paintIcon(c, g, x, y);
    }

    @NotNull
    private Icon ensureHasActual(Component c) {
      Icon actual = myActual;
      if (actual != null) return actual;
      int width = myBackground.getIconWidth();
      int height = myBackground.getIconHeight();
      if (width <= 0 || height <= 0) return myBackground;
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      myBackground.paintIcon(c, g, 0, 0);
      Color srcColor = ColorUtil.adjustHSB(myColor, -1, 1, -1);
      g.setColor(new Color(srcColor.getRed(), srcColor.getGreen(), srcColor.getBlue(), 70));
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int r = (int) (Math.round(Math.sqrt(width*width + height*height) / 2) - 3);
      g.fillOval(width/2 - r, height/2 -r, 2*r, 2*r);
      ImageIcon icon = new ImageIcon(image);
      actual = myActual;
      if (actual != null) return actual;
      myActual = icon;
      return icon;
    }

    @Override
    public int getIconWidth() {
      return myBackground.getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return myBackground.getIconHeight();
    }
  }
}
