package com.almworks.actions.console.actionsource;

import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface ActionGroup {
  public static final PresentationKey<List<AnAction>> ACTIONS = new PresentationKey<List<AnAction>>("actionGroup.actions");

  public void update(UpdateContext context) throws CantPerformException;

  public class Simple implements ActionGroup {
    private final String myName;
    private final Set<String> myActionIds;

    public Simple(String name, String ... actionIds) {
      myName = name;
      myActionIds = Collections15.hashSet(actionIds);
    }

    @Override
    public final void update(UpdateContext context) throws CantPerformException {
      context.putPresentationProperty(PresentationKey.NAME, myName);
      context.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
      doUpdate(context);
      if (context.isDisabled()) return;
      ActionRegistry registry = context.getSourceObject(ActionRegistry.ROLE);
      ArrayList<AnAction> actions = Collections15.arrayList();
      for (String actionId : myActionIds) {
        AnAction action = registry.getAction(actionId);
        if (action != null) actions.add(action);
      }
      context.putPresentationProperty(ACTIONS, actions);
    }

    protected void doUpdate(UpdateContext context) throws CantPerformException {}
  }

  public class InContext extends Simple {
    private final TypedKey<?> myRole;

    public InContext(String name, TypedKey<?> role, String... actionIds) {
      super(name, actionIds);
      myRole = role;
    }

    @Override
    protected void doUpdate(UpdateContext context) throws CantPerformException {
      checkContext(context, myRole);
    }

    public static void checkContext(ActionContext context, TypedKey<?> role) throws CantPerformException {
      ComponentContext<JComponent> componentContext = context.getComponentContext(JComponent.class, role);
      CantPerformException.ensure(SwingTreeUtil.isAncestor(componentContext.getComponent(), context.getComponent()));
    }
  }
}
