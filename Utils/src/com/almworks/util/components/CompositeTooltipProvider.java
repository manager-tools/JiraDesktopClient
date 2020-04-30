package com.almworks.util.components;


import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class CompositeTooltipProvider implements TableTooltipProvider {
  private List<TableTooltipProvider> myProviders;

  @Nullable
  public String getTooltip(int row, int column, Point tablePoint) {
    List<TableTooltipProvider> providers = myProviders;
    if (providers == null)
      return null;
    for (TableTooltipProvider provider : providers) {
      String tip = provider.getTooltip(row, column, tablePoint);
      if (tip != null) {
        return tip;
      }
    }
    return null;
  }

  public void addProvider(TableTooltipProvider provider) {
    if (myProviders == null) {
      myProviders = Collections15.arrayList();
    }
    myProviders.add(provider);
  }

  public void removeProvider(TableTooltipProvider provider) {
    List<TableTooltipProvider> providers = myProviders;
    if (providers != null) {
      providers.remove(provider);
    }
  }
}
