package com.almworks.api.application.util;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.engine.Connection;
import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.properties.PropertyMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;

public abstract class ItemExport {
  public static final Convertor<ItemExport, String> GET_DISPLAY_NAME = new Convertor<ItemExport, String>() {
    @Override
    public String convert(ItemExport value) {
      return value != null ? value.getDisplayName() : "";
    }
  };
  public static final Comparator<ItemExport> DISPLAY_NAME_COMPARATOR = Containers.convertingComparator(GET_DISPLAY_NAME, String.CASE_INSENSITIVE_ORDER);
  public static final CanvasRenderer<? super ItemExport> DISPLAY_NAME_RENDERER = Renderers.convertingCanvasRenderer(Renderers.<String>defaultCanvasRenderer(), GET_DISPLAY_NAME);
  public static final Convertor<ItemExport, String> GET_ID = new Convertor<ItemExport, String>() {
    @Override
    public String convert(ItemExport value) {
      return value != null ? value.getId() : "";
    }
  };

  private final String myId;

  public ItemExport(String id) {
    myId = id;
  }

  @Nullable
  public abstract Pair<String, ExportValueType> formatForExport(PropertyMap values, ExportContext context);

  public abstract boolean isExportable(Collection<Connection> conns);

  public abstract String getDisplayName();

  public String getId() {
    return myId;
  }
}
