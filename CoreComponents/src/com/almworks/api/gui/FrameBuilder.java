package com.almworks.api.gui;

import com.almworks.util.ui.UIComponentWrapper;

/**
 * @author : Dyoma
 */
public interface FrameBuilder extends BasicWindowBuilder<UIComponentWrapper> {

  /**
   * By default, resizable frame is created.
   * */
  void setResizable(boolean resizable);
}
