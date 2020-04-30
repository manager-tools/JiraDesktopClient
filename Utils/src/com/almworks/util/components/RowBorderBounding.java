package com.almworks.util.components;

import javax.swing.*;
import java.awt.*;

public interface RowBorderBounding {
  /**
   * This interface must be implemented by a CollectionRenderer to give RowBorder a hint
   * about whether this column wants to adjust the start of the border.
   *
   * NB: called when painting
   *
   * @return
   *   &lt;0 if the column should not be bordered
   *   0 if the column should be bordered
   *   &gt;0 if the column should be partially painted, starting from pixel "X" within the column.
   *
   */
  int getRowBorderX(JTable table, Graphics g);
}
