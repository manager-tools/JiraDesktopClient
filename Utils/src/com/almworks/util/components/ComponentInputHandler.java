package com.almworks.util.components;

import org.almworks.util.detach.Lifespan;

import javax.swing.*;

/**
 * @author dyoma
 */
public interface ComponentInputHandler {
  ComponentInputHandler DEAF = new ComponentInputHandler() {
    public void subscribe(Lifespan lifespan, JComponent host, InputEventController inputEventController) {
    }
  };

  void subscribe(Lifespan lifespan, JComponent host, InputEventController inputEventController);
}
