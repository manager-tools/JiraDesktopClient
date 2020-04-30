package com.almworks.gui;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowManager;
import com.almworks.util.config.Configuration;

/**
 * @author : Dyoma
 */
public class DefaultWindowManager implements WindowManager {
  private final ComponentContainer myContainer;
  private final Configuration myConfiguration;

  public DefaultWindowManager(ComponentContainer container, Configuration configuration) {
    myContainer = container;
    myConfiguration = configuration;
  }

  public FrameBuilder createFrame(String windowId) {
    String selectionId = "window." + windowId;
    return new FrameBuilderImpl(myContainer.createSubcontainer(selectionId),
      myConfiguration.getOrCreateSubset(selectionId));
  }
}
