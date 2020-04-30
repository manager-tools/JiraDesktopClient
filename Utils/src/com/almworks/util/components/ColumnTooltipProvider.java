package com.almworks.util.components;

import com.almworks.util.collections.Convertor;
import com.almworks.util.components.renderer.CellState;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface ColumnTooltipProvider<T> {
  @Nullable
  String getTooltip(CellState cellState, T element, Point cellPoint, Rectangle cellRect);

  class Converting<D, R> implements ColumnTooltipProvider<D> {
    private final ColumnTooltipProvider<R> myProvider;
    private final Convertor<? super D, ? extends R> myConvertor;

    protected Converting(ColumnTooltipProvider<R> provider, Convertor<? super D, ? extends R> convertor) {
      myProvider = provider;
      myConvertor = convertor;
    }

    public static <D, R> ColumnTooltipProvider<D> create(ColumnTooltipProvider<R> provider, Convertor<? super D, ? extends R> convertor) {
      return new Converting<D, R>(provider, convertor);
    }

    @Override
    public String getTooltip(CellState cellState, D element, Point cellPoint, Rectangle cellRect) {
      return myProvider.getTooltip(cellState, myConvertor.convert(element), cellPoint, cellRect);
    }
  }
}
