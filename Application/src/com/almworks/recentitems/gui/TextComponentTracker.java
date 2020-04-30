package com.almworks.recentitems.gui;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TextComponentTracker implements PropertyChangeListener {
  public static final DataRole<TextComponentTracker> TEXT_COMPONENT_TRACKER =
    DataRole.createRole(TextComponentTracker.class);

  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private Window myToolWindow;
  private JTextComponent myComponent;

  public TextComponentTracker(@NotNull Lifespan life, @Nullable Window toolWindow, @Nullable Component initial) {
    myToolWindow = toolWindow;
    onFocusOwnerChanged(initial);
    UIUtil.addGlobalFocusOwnerListener(life, this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final Component newOwner = (Component)evt.getNewValue();
    onFocusOwnerChanged(newOwner);
  }

  private void onFocusOwnerChanged(Component newOwner) {
    if(newOwner == myComponent) {
      return;
    }

    if(newOwner != null && myToolWindow != null
        && SwingTreeUtil.getOwningWindow(newOwner) == myToolWindow) {
      return;
    }

    if(newOwner instanceof JTextComponent) {
      onNewTextComponent((JTextComponent)newOwner);
    } else {
      onNewOtherComponent(newOwner);
    }
  }

  private void onNewTextComponent(@NotNull JTextComponent newOwner) {
    if(newOwner.isEditable()) {
      myComponent = newOwner;
      myModifiable.fireChanged();
    } else if(myComponent != null) {
      myComponent = null;
      myModifiable.fireChanged();
    }
  }

  private void onNewOtherComponent(@Nullable Component newOwner) {
    if(myComponent != null && newOwner != null) {
      myComponent = null;
      myModifiable.fireChanged();
    }
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public JTextComponent getTextComponent() {
    return myComponent;
  }

  public void setToolWindow(@NotNull Window toolWindow) {
    assert myToolWindow == null;
    myToolWindow = toolWindow;
    final Component component = myComponent;
    myComponent = null;
    onFocusOwnerChanged(component);
  }
}
