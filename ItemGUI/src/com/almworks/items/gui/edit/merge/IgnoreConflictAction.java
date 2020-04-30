package com.almworks.items.gui.edit.merge;

import com.almworks.util.L;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;

import java.util.List;

class IgnoreConflictAction extends SimpleAction {
  public static final AnAction INSTANCE = new IgnoreConflictAction();

  private IgnoreConflictAction() {
    super(L.actionName("&Ignore Conflict"), null);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<MergeValue> changed = ApplyVersionAction.getChanged(context);
    for (MergeValue value : changed) CantPerformException.cast(MergeValue.Simple.class, value);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    List<MergeValue> values = ApplyVersionAction.getChanged(context);
    for (MergeValue value : values) {
      MergeValue.Simple simple = Util.castNullable(MergeValue.Simple.class, value);
      if (simple != null) simple.markResolved();
    }
  }
}
