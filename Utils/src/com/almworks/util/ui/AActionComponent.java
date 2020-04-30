package com.almworks.util.ui;

import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import java.util.Map;

/**
 * @author dyoma
 */
public interface AActionComponent<C extends JComponent> {
  C toComponent();

  Detach setAnAction(AnAction action);

  void setActionById(String actionId);

  void updateNow();

  void setContextComponent(JComponent component);

  void setPresentationMapping(String swingKey, PresentationMapping<?> mapping);

  void overridePresentation(Map<String, PresentationMapping> mapping);
}
