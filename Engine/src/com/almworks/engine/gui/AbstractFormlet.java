package com.almworks.engine.gui;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.ToolbarEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractFormlet implements Formlet {
  private static final String COLLAPSED_SETTING = "formletCollapsed";
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private final Configuration myConfiguration;
  private boolean myCollapsed;

  protected AbstractFormlet(Configuration config, boolean initiallyCollapsed) {
    myConfiguration = config;
    myCollapsed = config != null ? config.getBooleanSetting(COLLAPSED_SETTING, initiallyCollapsed) : initiallyCollapsed;
  }

  protected AbstractFormlet(Configuration config) {
    this(config, false);
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public boolean isCollapsible() {
    return true;
  }

  public boolean isCollapsed() {
    return myCollapsed;
  }

  protected void fireFormletChanged() {
    myModifiable.fireChanged();
  }

  private void setCollapsed(boolean collapsed) {
    if (collapsed != myCollapsed) {
      myCollapsed = collapsed;
      if (myConfiguration != null) {
        myConfiguration.setSetting(COLLAPSED_SETTING, myCollapsed);
      }
      fireFormletChanged();
      WidthDrivenComponent content = getContent();
      content.getComponent().revalidate();
    }
  }

  @Nullable
  public List<? extends ToolbarEntry> getActions() {
    return null;
  }

  public void toggleExpand() {
    setCollapsed(!isCollapsed());
  }

  public void expand() {
    setCollapsed(false);
  }

  @Nullable
  public String getCaption() {
    return null;
  }
}
