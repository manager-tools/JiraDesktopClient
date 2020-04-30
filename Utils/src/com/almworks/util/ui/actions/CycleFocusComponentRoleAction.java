package com.almworks.util.ui.actions;

import com.almworks.util.DECL;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author dyoma
 */
public class CycleFocusComponentRoleAction extends SimpleAction {
  private final List<? extends TypedKey<? extends JComponent>> myComponents;

  public CycleFocusComponentRoleAction(TypedKey<? extends JComponent> ... components) {
    myComponents = Collections15.arrayList(components);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    for (TypedKey<? extends JComponent> key : myComponents)
      context.watchRole(key);
    context.setEnabled(EnableState.DISABLED);
    for (TypedKey<? extends JComponent> key : myComponents) {
      try {
        context.getSourceObject(key);
        context.setEnabled(EnableState.ENABLED);
        return;
      } catch (CantPerformException e) {
        DECL.ignoreException();
      }
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    int focusedIndex = getNowFocusedIndex(context);
    if (focusedIndex == -1) {
      for (TypedKey<? extends JComponent> key : myComponents)
        try {
        if (FocusComponentRoleAction.focusComponentByRole(context, key))
          break;
        } catch(CantPerformException e) {
          DECL.ignoreException();
        }
      return;
    }
    for (int i = 1; i < myComponents.size(); i++) {
      try {
        TypedKey<? extends JComponent> role = myComponents.get((focusedIndex + i) % myComponents.size());
        if (FocusComponentRoleAction.focusComponentByRole(context, role))
          break;
      } catch (CantPerformException e) {
        DECL.ignoreException();
      }
    }
  }

  public AnAction backwardAction() {
    TypedKey<JComponent>[] keys = new TypedKey[myComponents.size()];
    myComponents.toArray(keys);
    ArrayUtil.reverse(keys, 0, keys.length);
    return new CycleFocusComponentRoleAction(keys);
  }

  private int getNowFocusedIndex(ActionContext context) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    int focusedIndex = -1;
    for (int i = 0; i < myComponents.size(); i++) {
      TypedKey<? extends JComponent> key = myComponents.get(i);
      try {
        JComponent component = context.getSourceObject(key);
        if (component.isDisplayable() && (component == focusOwner || SwingTreeUtil.isAncestor(component, focusOwner))) {
          focusedIndex = i;
          break;
        }
      } catch (CantPerformException e) {
        DECL.ignoreException();
      }
    }
    return focusedIndex;
  }
}
