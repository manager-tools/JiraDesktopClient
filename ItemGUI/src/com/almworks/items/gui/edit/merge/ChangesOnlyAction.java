package com.almworks.items.gui.edit.merge;

import com.almworks.util.L;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

class ChangesOnlyAction extends SimpleAction {
  public static final AnAction INSTANCE = new ChangesOnlyAction();

  public ChangesOnlyAction() {
    super(L.actionName("Changes Only"), Icons.ACTION_MERGE_HIDE_SAME);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Show only changed fields"));
    watchModifiableRole(MergeControl.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.INVISIBLE);
    MergeControl control = context.getSourceObject(MergeControl.ROLE);
    context.setEnabled(EnableState.ENABLED);
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, control.isFiltered());
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    MergeControl control = context.getSourceObject(MergeControl.ROLE);
    control.setFiltered(!control.isFiltered());
  }
}
