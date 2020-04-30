package com.almworks.util.ui;

import com.almworks.util.components.AActionButton;
import com.almworks.util.components.AMenuChild;
import com.almworks.util.components.AnActionHolder;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class AMenuItem extends JMenuItem implements AActionComponent<JMenuItem>, AMenuChild {
  private static final LinkedHashMap<Icon, Icon> ourFixedIconsCache = Collections15.linkedHashMap(120, 0.9F, true, 100);
  private static final int FIXED_WIDTH = 16;
  private static final int FIXED_HEIGHT = 16;

  private final AnActionHolder myActionHolder = new AnActionHolder(this);
  private final boolean myFixedIconSize;

  public AMenuItem(boolean fixedIconSize) {
    super("");
    myFixedIconSize = fixedIconSize;
    setVisible(false);
  }

  public AMenuItem(AnAction action, boolean fixedIconSize) {
    this(fixedIconSize);
    setAnAction(action);
  }

  protected void configurePropertiesFromAction(Action a) {
    super.configurePropertiesFromAction(a);
    AActionButton.updateVisibility(this, a);
  }

  protected PropertyChangeListener createActionPropertyChangeListener(final Action a) {
    final PropertyChangeListener superListener = super.createActionPropertyChangeListener(a);
    return new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        superListener.propertyChange(evt);
        configurePropertiesFromAction(a);
      }
    };
  }

  public Detach setAnAction(AnAction action) {
    assert !isDisplayable() || !isVisible() : action;
    return myActionHolder.setAnAction(action);
  }

  public void setActionById(String actionId) {
    myActionHolder.setActionById(actionId);
  }

  public void setContextComponent(JComponent component) {
    assert !isDisplayable() || !isVisible() : component;
    myActionHolder.setContextComponent(component);
  }

  public void updateNow() {
    myActionHolder.updateNow();
  }

  public JMenuItem toComponent() {
    return this;
  }

  public void parentStartsUpdate() {
    myActionHolder.startUpdate();
  }

  public void parentStopsUpdate() {
    myActionHolder.stopUpdate();
  }

  public void setPresentationMapping(String swingKey, PresentationMapping<?> mapping) {
    myActionHolder.setPresentationMapping(swingKey, mapping);
  }

  public void overridePresentation(Map mapping) {
    myActionHolder.overridePresentation(mapping);
  }

  public void setIcon(Icon icon) {
    if (myFixedIconSize)
      icon = alignIcon(icon);
    super.setIcon(icon);
  }

  private Icon alignIcon(Icon icon) {
    Threads.assertAWTThread();
    if (icon != null && icon.getIconHeight() == FIXED_HEIGHT && icon.getIconWidth() == FIXED_WIDTH)
      return icon;
    Icon fixed = ourFixedIconsCache.get(icon);
    if (fixed == null) {
      fixed = new IconSizeFixup(icon, FIXED_WIDTH, FIXED_HEIGHT);
      ourFixedIconsCache.put(icon, fixed);
    }
    return fixed;
  }
}
