package com.almworks.api.explorer;

import com.almworks.util.components.PlaceHolder;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.UIComponentWrapper2;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface ApplicationToolbar extends UIComponentWrapper2 {
  public static final Role<ApplicationToolbar> ROLE = Role.role(ApplicationToolbar.class);

  void setSectionComponent(Section section, @Nullable JComponent component);

  PlaceHolder getSectionHolder(Section section);

  public enum Section {
    CREATE_ARTIFACT,
    SYNCHRONIZATION,
    NAVIGATION_NODE_ACTIONS,
    TOOLS,
    SEARCH,
    EXTERNAL_ARTIFACT_TOOLS,
    OTHER
  }
}
