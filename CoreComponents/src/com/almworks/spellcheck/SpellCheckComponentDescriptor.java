package com.almworks.spellcheck;

import com.almworks.api.container.RootContainer;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.platform.RegisterActions;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;

public class SpellCheckComponentDescriptor implements ComponentDescriptor {
  @Override
  public void registerActors(RootContainer container) {
    container.registerActorClass(SpellCheckManager.ROLE, SpellCheckManager.class);
    RegisterActions.registerAction(container, MainMenu.Tools.SPELL_CHECKER_SETTINGS, new SimpleAction(SpellCheckManager.I18N.getFactory("spellcheck.actions.settings.name"), null) {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        SpellCheckSettings.showDialog(context.getSourceObject(DialogManager.ROLE), context.getSourceObject(SpellCheckManager.ROLE).getConfig());
      }
    });
  }
}
