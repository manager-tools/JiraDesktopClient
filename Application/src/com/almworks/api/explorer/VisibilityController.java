package com.almworks.api.explorer;

import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.DefaultUIController;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;

public class VisibilityController implements UIController<JComponent> {
  private final Convertor<ModelMap, Boolean> myConvertor;

  public VisibilityController(Convertor<ModelMap, Boolean> convertor) {
    myConvertor = convertor;
  }

  public void connectUI(final Lifespan lifespan, final ModelMap model, final JComponent component) {
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        if (!lifespan.isEnded()) {
          Boolean visible = myConvertor.convert(model);
          component.setVisible(visible != null && visible);
        }
      }
    };
    model.addAWTChangeListener(lifespan, listener);

    // process child components
    if (component.getComponentCount() > 0) {
      DefaultUIController.ROOT.connectUI(lifespan, model, component);
    }

    listener.onChange();
  }
}
