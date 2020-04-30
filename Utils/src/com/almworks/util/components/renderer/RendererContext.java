package com.almworks.util.components.renderer;

import com.almworks.util.components.RendererActivityController;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * @author dyoma
 */
public class RendererContext {
  private final Map<TypedKey<?>, ?> myCache = Collections15.hashMap();
  private final Map<TypedKey<?>, ?> myValues = Collections15.hashMap();
  private final Map<Integer, FontMetrics> myFontMetrics = Collections15.hashMap();
  private final RendererHostComponent myHost;
  public static final DataRole<RendererContext> RENDERER_CONTEXT = DataRole.createRole(RendererContext.class);

  public RendererContext(RendererHostComponent host) {
    myHost = host;
    ConstProvider.addRoleValue(host.getComponent(), RENDERER_CONTEXT, this);
  }

  public int getFontHeight(int fontStyle) {
    return getFontMetrics(fontStyle).getHeight();
  }

  public int getStringWidth(String str, int fontStyle) {
    return getFontMetrics(fontStyle).stringWidth(str);
  }

  public int getFontBaseLine(int fontStyle) {
    return getFontMetrics(fontStyle).getAscent();
  }

  public <T> void putValue(TypedKey<? extends T> key, T value) {
    ((TypedKey<T>) key).putTo(myValues, value);
  }

  @Nullable
  public <T> T getValue(TypedKey<T> key) {
    return key.getFrom(myValues);
  }

  @NotNull
  public FontMetrics getFontMetrics(int fontStyle) {
    FontMetrics fontMetrics = myFontMetrics.get(fontStyle);
    if (fontMetrics == null) {
      fontMetrics = myHost.getFontMetrics(fontStyle);
      myFontMetrics.put(fontStyle, fontMetrics);
    }
    return fontMetrics;
  }

  @Nullable
  public <T> T getCachedValue(TypedKey<T> key) {
    return key.getFrom(myCache);
  }

  public void resetCaches() {
    myCache.clear();
  }

  public <T> void cacheValue(TypedKey<T> key, T value) {
    key.putTo(myCache, value);
  }

  public Font getFont(int fontStyle) {
    return getFontMetrics(fontStyle).getFont();
  }

  public int getWidth() {
    return myHost.getWidth();
  }

  public int getHeight() {
    return myHost.getHeight();
  }

  public Color getForeground() {
    return myHost.getForeground();
  }

  public Color getBackground() {
    return myHost.getBackground();
  }

  public JComponent getComponent() {
    return myHost;
  }

  public RendererActivityController getController() {
    return myHost.getActivityController();
  }
}
