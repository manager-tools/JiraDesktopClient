package com.almworks.util.ui.actions;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class FocusComponentRoleAction extends SimpleAction {
  private final TypedKey<? extends JComponent> myComponentRole;

  public FocusComponentRoleAction(TypedKey<? extends JComponent> componentRole) {
    myComponentRole = componentRole;
  }

  public FocusComponentRoleAction(@Nullable String name, TypedKey<? extends JComponent> componentRole) {
    super(name);
    myComponentRole = componentRole;
  }

  public FocusComponentRoleAction(@Nullable String name, @Nullable Icon icon, TypedKey<? extends JComponent> componentRole) {
    super(name, icon);
    myComponentRole = componentRole;
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.watchRole(myComponentRole);
    context.getSourceObject(myComponentRole);
    context.setEnabled(EnableState.ENABLED);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    focusComponentByRole(context, myComponentRole);
  }

  private static Component findComponentToFocusByRole(ActionContext context, TypedKey<? extends JComponent> role) throws
    CantPerformException
  {
    JComponent component = context.getSourceObject(role);
    if (!component.isDisplayable())
      return null;
    return component.getFocusCycleRootAncestor().getFocusTraversalPolicy().getDefaultComponent(component);
  }

  public static boolean focusComponentByRole(ActionContext context, TypedKey<? extends JComponent> role) throws CantPerformException {
    Component toFocus = findComponentToFocusByRole(context, role);
    if (toFocus == null)
      return false;
    toFocus.requestFocusInWindow();
    return true;
  }
}
