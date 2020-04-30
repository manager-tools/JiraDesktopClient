package com.almworks.util.models;

import com.almworks.util.collections.ReadAccessor;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

/**
 * @author dyoma
 */
public interface TableColumnAccessor<E, V> extends ColumnAccessor<E>, ReadAccessor<E, V> {
  Comparator<TableColumnAccessor> NAME_ORDER = new Comparator<TableColumnAccessor>() {
    public int compare(TableColumnAccessor o1, TableColumnAccessor o2) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  };

  /** The hint to draw a thin line along this column's right boder. */
  TypedKey<Boolean> LINE_EAST_HINT = TypedKey.create(Boolean.class);

  /** The hint to paint a background color for this column. */
  TypedKey<Color> BACKGROUND_COLOR_HINT = TypedKey.create(Color.class);

  /** The hint for setting background color transparency for this column. */
  TypedKey<Float> BACKGROUND_ALPHA_HINT = TypedKey.create(Float.class);
  
  CanvasRenderer<TableColumnAccessor<?,?>> NAME_RENDERER = new CanvasRenderer<TableColumnAccessor<?, ?>>() {
    public void renderStateOn(CellState state, Canvas canvas, TableColumnAccessor<?, ?> item) {
      canvas.appendText(item.getName());
    }
  };

  /**
   * Externalizable column id.
   */
  String getId();

  /**
   * Displayable name of the column that is used to identify it for the user.
   */
  String getName();

  /**
   * This value is rendered in the column header.
   */
  String getColumnHeaderText();

  @Nullable
  CollectionEditor<E> getDataEditor();

  Comparator<E> getComparator();

  int getPreferredWidth(JTable table, ATableModel<E> tableModel, ColumnAccessor<E> renderingAccessor, int columnIndex);

  ColumnSizePolicy getSizePolicy();

  String getHeaderTooltip();

  /**
   * If false, column will not be draggable
   */
  boolean isOrderChangeAllowed();

  boolean isSortingAllowed();

  @Nullable
  ColumnTooltipProvider<E> getTooltipProvider();

  public <T> T getHint(@NotNull TypedKey<T> key);

  abstract class DelegatingColumn<E, V> implements TableColumnAccessor<E, V> {
    @NotNull
    private TableColumnAccessor<? super E, ? extends V> myColumn;

    public DelegatingColumn(@NotNull TableColumnAccessor<? super E, ? extends V> column) {
      //noinspection ConstantConditions
      assert column != null;
      myColumn = column;
    }

    public void setColumn(@NotNull TableColumnAccessor<? super E, ? extends V> column) {
      myColumn = column;
    }

    protected TableColumnAccessor<? super E, ? extends V> getDelegate() {
      return myColumn;
    }

    public String getName() {
      return myColumn.getName();
    }

    public String getId() {
      return myColumn.getId();
    }

    public String getColumnHeaderText() {
      return myColumn.getColumnHeaderText();
    }

    public CollectionEditor<E> getDataEditor() {
      return (CollectionEditor<E>) myColumn.getDataEditor();
    }

    public Comparator<E> getComparator() {
      return (Comparator<E>) myColumn.getComparator();
    }

    public ColumnSizePolicy getSizePolicy() {
      return myColumn.getSizePolicy();
    }

    public String getHeaderTooltip() {
      return myColumn.getHeaderTooltip();
    }

    public boolean isOrderChangeAllowed() {
      return myColumn.isOrderChangeAllowed();
    }

    public boolean isSortingAllowed() {
      return myColumn.isSortingAllowed();
    }

    public V getValue(E object) {
      return myColumn.getValue(object);
    }

    public int hashCode() {
      return myColumn.hashCode();
    }

    public ColumnTooltipProvider<E> getTooltipProvider() {
      return (ColumnTooltipProvider<E>) myColumn.getTooltipProvider();
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof DelegatingColumn<?, ?>))
        return false;
      return myColumn.equals(((DelegatingColumn<?, ?>) obj).myColumn);
    }

    public <T> T getHint(TypedKey<T> key) {
      return myColumn.getHint(key);
    }

    @Override
    public String toString() {
      return getClass().getName() + "(" + myColumn + ")";
    }
  }
}
