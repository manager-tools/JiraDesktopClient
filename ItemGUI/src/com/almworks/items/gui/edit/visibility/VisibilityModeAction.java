package com.almworks.items.gui.edit.visibility;

import com.almworks.util.images.AlphaIcon;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.*;

class VisibilityModeAction extends SimpleAction {
  public static final AnAction INSTANCE = new VisibilityModeAction();

  private VisibilityModeAction() {
    super((String)null, new AlphaIcon(Icons.SWITCH, 0.75f));
    watchModifiableRole(FieldVisibilityController.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    FieldVisibilityController controller = context.getSourceObject(FieldVisibilityController.ROLE);
    if(!controller.hasAnyField()) {
      context.putPresentationProperty(PresentationKey.NAME, "None");
      context.setEnabled(EnableState.INVISIBLE);
      return;
    }
    int level = controller.getVisibilityLevel();
    VisibilityDescription description = controller.getDescription();
    String name = description.getLevelName(level);
    String tooltip = description.getTooltip(level);
    if (name == null) {
      context.putPresentationProperty(PresentationKey.NAME, "None");
      context.setEnabled(EnableState.INVISIBLE);
      return;
    }
    context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, tooltip);
    context.putPresentationProperty(PresentationKey.NAME, name);
    if (level == description.getNextLevel(level)) {
      context.setEnabled(EnableState.DISABLED);
      return;
    }

    Component comp = context.getComponent();
    if(comp instanceof AbstractButton) {
      final AbstractButton button = (AbstractButton)comp;
      button.setHorizontalTextPosition(SwingConstants.LEFT);
      button.setIconTextGap(6);
    }
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    FieldVisibilityController controller = context.getSourceObject(FieldVisibilityController.ROLE);
    int level = controller.getVisibilityLevel();
    VisibilityDescription description = controller.getDescription();
    controller.setVisibilityLevel(description.getNextLevel(level));
  }
}
