package com.almworks.util.components;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface TableTooltipProvider {
  @Nullable
  String getTooltip(int row, int column, Point tablePoint);
}
