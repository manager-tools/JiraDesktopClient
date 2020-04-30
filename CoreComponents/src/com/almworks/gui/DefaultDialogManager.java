package com.almworks.gui;

import com.almworks.api.container.ComponentContainer;
import com.almworks.util.commons.Factory;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.UIUtil;

import java.awt.*;

/**
 * @author : Dyoma
 */
public class DefaultDialogManager extends BaseDialogManager {
  public DefaultDialogManager(Configuration configuration, ComponentContainer container) {
    super(configuration, container, new Factory<Window>() {
      public Window create() {
        return UIUtil.getDefaultDialogOwner();
      }
    });
  }
}
