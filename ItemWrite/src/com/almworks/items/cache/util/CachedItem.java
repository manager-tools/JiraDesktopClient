package com.almworks.items.cache.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;

import java.util.Comparator;

public interface CachedItem {
  <T> T getValue(DataLoader<T> loader);

  <T> T getValue(DBAttribute<T> attribute);

  long getItem();

  DBImage getImage();

  class ByAttrComparator implements Comparator<CachedItem> {
    private final DataLoader<? extends Comparable> myAttribute;

    public ByAttrComparator(DataLoader<? extends Comparable> attribute) {
      myAttribute = attribute;
    }

    @Override
    public int compare(CachedItem o1, CachedItem o2) {
      Comparable c1 = getValue(o1);
      Comparable c2 = getValue(o2);
      if (c1 == c2) return 0;
      if (c1 == null || c2 == null) return c1 == null ? -1 : 1;
      return c1.compareTo(c2);
    }

    private Comparable getValue(CachedItem item) {
      return item != null ? item.getValue(myAttribute) : null;
    }
  }

  class SingleValueRenderer implements CanvasRenderer<CachedItem> {
    private final DBAttribute<?> myAttribute;
    private final String myNullText;

    public SingleValueRenderer(DBAttribute<?> attribute, String nullText) {
      myAttribute = attribute;
      myNullText = nullText;
    }

    @Override
    public void renderStateOn(CellState state, Canvas canvas, CachedItem item) {
      Object value = item != null ? item.getValue(myAttribute) : null;
      canvas.appendText(value != null ? String.valueOf(value) : myNullText);
    }
  }

  public static class ValueGetter<T> extends Convertor<CachedItem, T> {
    private final DataLoader<T> myLoader;

    public ValueGetter(DataLoader<T> loader) {
      myLoader = loader;
    }

    @Override
    public T convert(CachedItem value) {
      return value != null ? value.getValue(myLoader) : null;
    }

    public static <T> Convertor<CachedItem, T> create(DBAttribute<T> attribute) {
      return create(AttributeLoader.create(attribute));
    }

    public static <T> Convertor<CachedItem, T> create(DataLoader<T> loader) {
      return new ValueGetter<T>(loader);
    }
  }
}
