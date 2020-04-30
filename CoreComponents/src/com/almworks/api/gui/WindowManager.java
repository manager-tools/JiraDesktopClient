package com.almworks.api.gui;

import com.almworks.util.properties.Role;

/**
 * @author : Dyoma
 */
public interface WindowManager {
  Role<WindowManager> ROLE = Role.role("windowManager");
  FrameBuilder createFrame(String windowId);
}
