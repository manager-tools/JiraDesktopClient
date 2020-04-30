package com.almworks.api.application;

import com.almworks.util.Env;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.NullTolerantComparator;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.CanvasSection;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

/**
 * @author : Dyoma
 */
public abstract class ItemKey implements CanvasRenderable, Comparable<ItemKey>, UiItem {
  public static final long NOT_RESOLVED_LONG = 0L;
  public static final ItemKey INVALID = new ItemKeyStub("_#_#_I#N#V#A#L#I#D_#_#_", "", ItemOrder.NO_ORDER);
  public static final boolean HACK_HTML_OPTION_VALUES = Env.getBoolean("hack.html.option.values");

  public static final Convertor<ItemKey, String> DISPLAY_NAME = new Convertor<ItemKey, String>() {
    public String convert(ItemKey value) {
      return value != null ? value.getDisplayName() : "";
    }
  };
  public static final Convertor<ItemKey, String> GET_ID = new Convertor<ItemKey, String>() {
    public String convert(ItemKey value) {
      return value != null ? value.getId() : "";
    }
  };
  public static final Comparator<ItemKey> COMPARATOR = NullTolerantComparator.nullsFirst(new MyComparator());

  public static final Comparator<ItemKey> DISPLAY_NAME_ORDER = Containers.convertingComparator(DISPLAY_NAME, String.CASE_INSENSITIVE_ORDER);

  public static final DataRole<ItemKey> ITEM_KEY_ROLE = DataRole.createRole(ItemKey.class, "ITEM_KEY");

  public static final CanvasRenderer<ItemKey> ICON_NAME_RENDERER = new CanvasRenderer<ItemKey>() {
    public void renderStateOn(CellState state, Canvas canvas, ItemKey item) {
      if (item != null) {
        canvas.setIcon(item.getIcon());
        canvas.appendText(item.getDisplayName());
      }
    }
  };
  public static final CanvasRenderer<ItemKey> NAME_ID_RENDERER = new CanvasRenderer<ItemKey>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, ItemKey item) {
      if (item == null) return;
      String id = item.getId();
      String name = item.getDisplayName();
      if (Util.equals(id, name)) id = null;
      if (id == null) canvas.appendText(name);
      else {
        canvas.emptySection().appendText(name);
        CanvasSection section = canvas.emptySection();
        section.appendText(" [").appendText(id).appendText("]");
        section.setFontStyle(Font.ITALIC);
        if (!state.isSelected()) section.setForeground(ColorUtil.between(state.getOpaqueBackground(), state.getForeground(), 0.5f));
      }
    }
  };

  @NotNull
  public abstract String getId();

  @NotNull
  public abstract ItemOrder getOrder();

  public void renderOn(Canvas canvas, CellState state) {
    canvas.appendText(getDisplayName());
  }

  @NotNull
  public String getDisplayName() {
    return getId();
  }

  public static String hackFixHtmlValueName(String displayName) {
    if (!HACK_HTML_OPTION_VALUES) return displayName;
    if (displayName == null || displayName.length() < 8) return displayName;
    if (displayName.charAt(0) != '<') return displayName;
    StringBuilder b = new StringBuilder(displayName.length());
    boolean tag = false;
    for (int i = 0; i < displayName.length(); i++) {
      char c = displayName.charAt(i);
      if (tag && c == '>') {
        tag = false;
      } else if (!tag && c == '<') {
        tag = true;
      } else if (!tag) {
        b.append(c);
      }
    }
    String s = b.toString().trim();
    return s.length() == 0 ? displayName : s;
  }

  @Nullable
  public Icon getIcon() {
    return null;
  }

  /**
   * "Resolved" ItemKey is backed by a real DB item (long). Some ItemKeys are not resolved: their backing item is unknown. In this case, {@link #NOT_RESOLVED_LONG} is returned.
   * @return
   */
  public long getResolvedItem() {
    return NOT_RESOLVED_LONG;
  }

  /**
   * display name should come first for quick navigation in lists
   */
  public String toString() {
    return getDisplayName() + " [" + getId() + "]";
  }

  public int hashCode() {
    String id = getId();
    //noinspection ConstantConditions
    if (id == null) {
      assert false;
      return 0;
    }
    return id.hashCode();
  }

  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    return obj instanceof ItemKey && Util.equals(((ItemKey) obj).getId(), getId());
  }

  public int compareTo(ItemKey that) {
    int r = COMPARATOR.compare(this, that);
    assert verifyEquality(r, that);
    return r;
  }

  private boolean verifyEquality(int r, ItemKey that) {
    if (r == 0) {
      assert equals(that) : "[" + this + ":" + getOrder() + "] [" + that + ":" + that.getOrder() + "]";
    }
    return true;
  }

  public static <T extends ItemKey> Comparator<T> keyComparator() {
    return (Comparator<T>) COMPARATOR;
  }

  @Override
  public long getItem() {
    return getResolvedItem();
  }

  private static class MyComparator implements Comparator<ItemKey> {
    public int compare(ItemKey o1, ItemKey o2) {
      if (o1 == null || o2 == null) {
        assert false : o1 + " " + o2;
        return 0;
      }
      ItemOrder ord1 = o1.getOrder();
      ItemOrder ord2 = o2.getOrder();
      int r = ord1.compareTo(ord2);
      if (r != 0) return r;
      String id1 = o1.getId();
      String id2 = o2.getId();
      if (id1 == null || id2 == null) return NullTolerantComparator.compareWithNull(id1, id2, true);
      else return id1.compareTo(id2);
    }
  }
}
