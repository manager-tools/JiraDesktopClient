package com.almworks.export;

import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.UIComponentWrapper2;
import org.jetbrains.annotations.NotNull;

public interface ExporterUI extends UIComponentWrapper2 {
  void addParametersTo(PropertyMap parameters);

  /**
   * @return model with error string, null value means no errors, export enabled
   */
  @NotNull ScalarModel<String> getFormErrorModel();
}
