package com.almworks.api.application.util;

import com.almworks.util.commons.Function;
import com.almworks.util.components.CollectionRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.ShadingComponent;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.ColorUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.almworks.util.collections.Containers.constF;

public class DummyShadingRenderer implements CollectionRenderer<PropertyMap> {
  @NotNull
  private final CollectionRenderer <PropertyMap> myBaseRenderer;
  @NotNull
  private final Function<PropertyMap, Boolean> myIsDummy;
  private final JComponent myNoDummyValue = new ShadingComponent();

  public DummyShadingRenderer(
    @NotNull CollectionRenderer<PropertyMap> baseRenderer,
    @Nullable Function<PropertyMap, Boolean> isDummy)
  {
    myBaseRenderer = baseRenderer;
    myIsDummy = Util.NN(isDummy, constF(false).<PropertyMap>f());
  }

  public JComponent getRendererComponent(CellState state, PropertyMap values) {
    if (!myIsDummy.invoke(values)) {
      return myBaseRenderer.getRendererComponent(state, values);
    } else {
      myNoDummyValue.setForeground(ColorUtil.between(state.getForeground(), state.getOpaqueBackground(), 0.75f));
      state.setBackgroundTo(myNoDummyValue, true);
      myNoDummyValue.setBorder(state.getBorder());
      return myNoDummyValue;
    }
  }
}
