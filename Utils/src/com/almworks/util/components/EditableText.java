package com.almworks.util.components;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.config.Configuration;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@ThreadAWT
public class EditableText extends CanvasRenderable.TextWithIcon {
  private SimpleModifiable myModifiable;
  private String myText;

  public EditableText(String text, Icon openIcon, Icon closedIcon) {
    super(openIcon, closedIcon);
    myText = text;
  }

  public EditableText(String text, Icon icon) {
    this(text, icon, icon);
  }

  @NotNull
  public String getText() {
    return Util.NN(myText);
  }

  public boolean setText(String text) {
    if (Util.equals(text, myText))
      return false;
    myText = text;
    if (myModifiable != null)
      myModifiable.fireChanged();
    return true;
  }

  public void storeName(Lifespan life, final Configuration configuration, final String settingName) {
    getModifiable().addAWTChangeListener(life, new ChangeListener() {
      public void onChange() {
        configuration.setSetting(settingName, getText());
      }
    });
  }

  public Modifiable getModifiable() {
    if (myModifiable == null) {
      myModifiable = new SimpleModifiable();
    }
    return myModifiable;
  }

  public static CanvasRenderable folder(String text, Icon openIcon, Icon closedIcon) {
    return new EditableText(text, openIcon, closedIcon);
  }

  public boolean isDefaultContent() {
    return false;
  }

  public void setUserTyped(boolean userTyped) {
  }

  public void fireChanged() {
    ((SimpleModifiable)getModifiable()).fireChanged();
  }
}
