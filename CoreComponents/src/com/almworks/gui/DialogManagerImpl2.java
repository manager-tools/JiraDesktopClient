package com.almworks.gui;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.WindowController;
import com.almworks.util.commons.Factory;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.UIUtil;

import java.awt.*;

/**
 * @author : Dyoma
 */
public class DialogManagerImpl2 extends BaseDialogManager {
  public DialogManagerImpl2(Configuration configuration, final WindowController ownerWindow, ComponentContainer container) {
    super(configuration, container, new Factory<Window>() {
      public Window create() {
        for (Window window = ownerWindow.getWindow(); window != null; window = window.getOwner()) {
          if (window.isDisplayable())
            return window;
        }
        return UIUtil.getDefaultDialogOwner();
      }
    });
  }
}
