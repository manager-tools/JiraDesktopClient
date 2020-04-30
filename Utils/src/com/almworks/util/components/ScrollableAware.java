package com.almworks.util.components;

import com.almworks.util.ui.ComponentProperty;

import java.awt.*;

public interface ScrollableAware {
  ComponentProperty<ScrollableAware> COMPONENT_PROPERTY = ComponentProperty.createProperty("ScrollableAware");

  boolean wantFillViewportHeight(Dimension viewport, Dimension preferred);

  boolean wantFillViewportWidth(Dimension viewport, Dimension preferred);

  ScrollableAware FILL_HEIGHT = new ScrollableAware() {
    @Override
    public boolean wantFillViewportHeight(Dimension viewport, Dimension preferred) {
      return viewport.height > preferred.height;
    }
    @Override
    public boolean wantFillViewportWidth(Dimension viewport, Dimension preferred) {
      return true;
    }
  };
}
