package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.util.List;

public interface ThumbnailViewUI<T> {
  ThumbnailViewCellGeometry getCellGeometry(List<T> list, JComponent c);

  void paintItemImage(JComponent c, Graphics g, T item, Rectangle r);

  String getItemText(T item);

  String getTooltipText(T at);

  boolean isTransferSupported();

  Transferable createTransferable(List<T> items);
}
