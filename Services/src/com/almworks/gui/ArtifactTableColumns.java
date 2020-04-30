package com.almworks.gui;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.models.TableColumnAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Represents columns that can be shown in artifact table.
 */
public interface ArtifactTableColumns<T> {
  /**
   * @return all columns that can be shown in items table
   */
  @NotNull
  AListModel<? extends TableColumnAccessor<T, ?>> getAll();

  /**
   * @return only "main" columns: the notion of main is product-specific.
   * @see com.almworks.api.engine.Connection#getMainColumns
   */
  @NotNull
  ColumnsSet<T> getMain();

  /**
   * @return only "auxiliary" columns: the notion of auxiliary is product-specific.
   * If product does not specify auxiliary columns for items, returns null.
   * @see com.almworks.api.engine.Connection#getAuxiliaryColumns
   */
  @Nullable
  ColumnsSet<T> getAux();

  class ColumnsSet<T> {
    @NotNull
    public final AListModel<? extends TableColumnAccessor<T, ?>> model;
    public final Comparator<? super TableColumnAccessor<T, ?>> order;
    private static final ColumnsSet EMPTY = new ColumnsSet(AListModel.EMPTY, TableColumnAccessor.NAME_ORDER);

    public ColumnsSet(@NotNull AListModel<? extends TableColumnAccessor<T, ?>> model, Comparator<? super TableColumnAccessor<T, ?>> order) {
      this.model = model;
      this.order = order;
    }

    public static <T> ColumnsSet<T> empty() {
      return EMPTY;
    }
  }
}
