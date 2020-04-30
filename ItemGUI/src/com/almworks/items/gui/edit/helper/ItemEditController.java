package com.almworks.items.gui.edit.helper;

import com.almworks.items.sync.EditCommit;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DataRole;

public interface ItemEditController {
  DataRole<ItemEditController> ROLE = DataRole.createRole(ItemEditController.class);

  void commit(ActionContext context, EditCommit commit) throws CantPerformException;
}
