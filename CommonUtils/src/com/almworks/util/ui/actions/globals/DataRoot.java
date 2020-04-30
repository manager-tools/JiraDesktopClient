package com.almworks.util.ui.actions.globals;

import com.almworks.util.ui.ComponentProperty;

import javax.swing.*;

/**
 * @author dyoma
 */
public interface DataRoot {
  ComponentProperty<DataRoot> KEY = ComponentProperty.createProperty("dataRoot");
  
  DataRoot TERMINATOR = new DataRoot() {
    public void onGlobalsChanged(JComponent component) {
    }
  };

  void onGlobalsChanged(JComponent component);
}
