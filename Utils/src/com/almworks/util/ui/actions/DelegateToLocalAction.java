package com.almworks.util.ui.actions;

import org.almworks.util.TypedKey;

import javax.swing.*;

/**
 * An action that does not perform anything ifself but delegates to an action obtained from the context.<br>
 * This action may help to define global shortcut for action which cannot be registered globally (due to any reason).<br><br>
 * Usage:<br>
 * Assuming some keyboard shortcut is assigned to "My.action.id"<br>
 * <pre>
 *   // Create and register
 *   DelegateToLocalAction delegate = new DelegateToLocalAction("My.action.id");
 *   delegate.register(actionRegistry); //
 *
 *   // Use-case #1 make local action invokable with shortcut
 *   // The local action can be invoked with shortcut in globalized context of someComponent
 *   delegate.provideAction(someComponent, localAction);
 *
 *   // Use-case #2 assign shortcut to a button
 *   AnAction actualAction = ...;
 *   delegate.provideAction(button, actualAction);
 *   button.setAction(delegate.getProxy());
 * </pre>
 */
public class DelegateToLocalAction {
  private final TypedKey<AnAction> myLocalActionKey = TypedKey.create("Local action");
  private final IdActionProxy myProxy;

  public DelegateToLocalAction(String id) {
    myProxy = new IdActionProxy(id);
  }

  public IdActionProxy getProxy() {
    return myProxy;
  }

  public void provideAction(JComponent context, AnAction localAction) {
    ConstProvider.addGlobalValue(context, myLocalActionKey, localAction);
  }

  public void register(ActionRegistry registry) {
    registry.registerAction(myProxy.getId(), new SimpleAction() {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        AnAction action = null;
        try {
          action = context.getSourceObject(myLocalActionKey);
        } catch (CantPerformException e) {
          context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, true);
          return;
        }
        action.update(context);
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        context.getSourceObject(myLocalActionKey).perform(context);
      }
    });
  }
}
