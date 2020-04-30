package com.almworks.engine.gui.attachments;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.config.Configuration;

import java.awt.*;

public class AttachmentsViewMode {
  private static final String SETTING_NAME = "viewmode";

  private static final String THUMBNAILS = "THUMBNAILS";
  private static final String TABLE = "TABLE";

  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final Configuration myConfiguration;

  private String myMode;

  public AttachmentsViewMode(Configuration configuration) {
    myConfiguration = configuration;
    String setting = configuration.getSetting(SETTING_NAME, THUMBNAILS);
    myMode = TABLE.equals(setting) ? TABLE : THUMBNAILS;
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public void addThumbnailsCard(Container container, Component component) {
    assert container.getLayout() instanceof CardLayout;
    container.add(component, THUMBNAILS);
  }

  public void addTableCard(Container container, Component component) {
    assert container.getLayout() instanceof CardLayout;
    container.add(component, TABLE);
  }

  public void selectCard(Container container) {
    ((CardLayout) container.getLayout()).show(container, myMode);
  }

  public void setThumbnailsMode() {
    setMode(THUMBNAILS);
  }

  public void setTableMode() {
    setMode(TABLE);
  }

  private void setMode(String mode) {
    if (myMode != mode) {
      myMode = mode;
      myConfiguration.setSetting(SETTING_NAME, myMode);
      myModifiable.fireChanged();
    }
  }

  public boolean isThumbnailsMode() {
    return myMode == THUMBNAILS;
  }
}
