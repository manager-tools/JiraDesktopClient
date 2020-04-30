package com.almworks.api.application;

import com.almworks.api.application.field.RightViewerFields;
import com.almworks.engine.gui.ItemTableBuilder;
import com.almworks.util.components.layout.WidthDrivenColumn;
import com.almworks.util.config.Configuration;
import org.almworks.util.detach.Lifespan;

/** Transitional interface to call ItemGUI from Engine. */
public interface ViewerFieldsManager {
  void addLeftFields(ItemTableBuilder builder);

  void addRightFields(RightViewerFields host, WidthDrivenColumn hostComponent, Lifespan life, ModelMap model, Configuration settings);
}
