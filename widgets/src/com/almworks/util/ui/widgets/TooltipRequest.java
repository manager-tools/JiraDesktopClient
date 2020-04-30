package com.almworks.util.ui.widgets;

import org.almworks.util.TypedKey;

public class TooltipRequest {
  public static final TypedKey<TooltipRequest> KEY = TypedKey.create("tooltip");
  private String myTooltip;

  public String getTooltip() {
    return myTooltip;
  }

  public void setTooltip(String tooltip) {
    myTooltip = tooltip;
  }
}
