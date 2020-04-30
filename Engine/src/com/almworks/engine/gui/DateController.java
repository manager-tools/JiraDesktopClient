package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.components.ADateField;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DateController implements UIController<ADateField> {
  private final ModelKey<Date> myKey;

  public DateController(ModelKey<Date> key) {
    myKey = key;
  }

  public void connectUI(@NotNull Lifespan lifespan, @NotNull final ModelMap model,
    @NotNull final ADateField component)
  {
    JointChangeListener listener = new JointChangeListener() {
      protected void processChange() {
        Date date = myKey.getValue(model);
        component.setDate(date);
      }
    };
    model.addAWTChangeListener(lifespan, listener);
    listener.onChange();
    final ValueModel<Date> dateModel = component.getDateModel();
    dateModel.addAWTChangeListener(lifespan, new JointChangeListener(listener.getUpdateFlag()) {
      protected void processChange() {
        Date date = dateModel.getValue();
        PropertyMap props = new PropertyMap();
        myKey.setValue(props, date);
        myKey.copyValue(model, props);
        model.valueChanged(myKey);
      }
    });
  }

  public static DateController install(ADateField component, final ModelKey<Date> key) {
    DateController controller = new DateController(key);
    CONTROLLER.putClientValue(component, controller);
    return controller;
  }
}
