package com.almworks.util.models;

import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.Renderers;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Comparator;
import java.util.Map;

// TODO[dyoma]


/**
 * @author : Dyoma
 */
public abstract class BaseTableColumnAccessor<E, V> implements TableColumnAccessor<E, V> {
  private final String myId;
  private final String myName;
  private final CollectionRenderer<? super E> myDataRenderer;
  private final Comparator<E> myComparator;
  private CollectionEditor<E> myDataEditor;
  private Map<TypedKey<?>, Object> myHintMap;

  protected BaseTableColumnAccessor(String name, CollectionRenderer<? super E> dataRenderer, Comparator<? super V> comparator) {
    this(name, name, dataRenderer, comparator);
  }

  protected BaseTableColumnAccessor(String id, String name, CollectionRenderer<? super E> dataRenderer,
    Comparator<? super V> comparator)
  {
    myId = id;
    myName = name;
    myDataRenderer = dataRenderer;
    myComparator = comparator != null ? (Comparator) Containers.convertingComparator(this, comparator) : null;
  }

  public BaseTableColumnAccessor<E, V> setDataEditor(CollectionEditor<E> dataEditor) {
    myDataEditor = dataEditor;
    return this;
  }

  protected BaseTableColumnAccessor(String name, CollectionRenderer<? super E> dataRenderer) {
    this(name, dataRenderer, null);
  }

  protected BaseTableColumnAccessor(String name) {
    this(name, null);
  }

  protected BaseTableColumnAccessor(String name, CanvasRenderer<? super V> renderer, Comparator<? super V> comparator) {
    myId = name;
    myName = name;
    myComparator = comparator != null ? (Comparator) Containers.convertingComparator(this, comparator) : null;
    myDataRenderer =
      new Renderers.DefaultCollectionRenderer<E>(Renderers.convertingCanvasRenderer(renderer, new Convertor<E, V>() {
        @Override
        public V convert(E value) {
          return getValue(value);
        }
      }));
  }

  public String getName() {
    return myName;
  }

  public String getId() {
    return myId;
  }

  public String getColumnHeaderText() {
    return getName();
  }

  public CollectionRenderer<? super E> getDataRenderer() {
    return myDataRenderer;
  }

  public CollectionEditor<E> getDataEditor() {
    return myDataEditor;
  }

  public Comparator<E> getComparator() {
    return myComparator;
  }

  public int getPreferredWidth(JTable table, ATableModel<E> tableModel, ColumnAccessor<E> renderingAccessor,
    int columnIndex)
  {
    return getSizePolicy().getPreferredWidth(table, tableModel, renderingAccessor, columnIndex);
  }

  public ColumnSizePolicy getSizePolicy() {
    return ColumnSizePolicy.FREE;
  }

  public String getHeaderTooltip() {
    return getName();
  }

  public boolean isOrderChangeAllowed() {
    return true;
  }

  public boolean isSortingAllowed() {
    return true;
  }


  public ColumnTooltipProvider<E> getTooltipProvider() {
    return null;
  }

  public static <T> BaseTableColumnAccessor<T, T> simple(String name, CollectionRenderer<T> renderer,
    Comparator<T> comparator)
  {
    return new SimpleColumnAccessor<T>(name, renderer, comparator);
  }

  public static <T> BaseTableColumnAccessor<T, T> simple(String name, CollectionRenderer<T> renderer) {
    return new SimpleColumnAccessor<T>(name, renderer, null);
  }

  public <T> T getHint(@NotNull TypedKey<T> key) {
    if(myHintMap != null) {
      return key.getFrom(myHintMap);
    }
    return null;
  }

  public <T> void putHint(@NotNull TypedKey<T> key, T hint) {
    if(hint == null) {
      if(myHintMap != null) {
        myHintMap.remove(key);
        if(myHintMap.isEmpty()) {
          myHintMap = null;
        }
      }
    } else {
      if(myHintMap == null) {
        myHintMap = Collections15.hashMap();
      }
      myHintMap.put(key, hint);
    }
  }

  @Override
  public String toString() {
    return myName;
  }
}
