package com.almworks.actions.order;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.order.Order;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowManager;
import com.almworks.edit.EditLifecycleImpl;
import com.almworks.gui.ArtifactTableColumns;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.config.Configuration;

import java.util.Collection;

/**
 * @author dyoma
 */
class ItemsLoader {
  private ItemsLoader() {}

  public static void showReorderWindow(ComponentContainer container, Collection<? extends LoadedItem> items,
    AListModel<? extends Order> orders, String connectionID, ArtifactTableColumns<LoadedItem> columns)
  {
    if (orders.getSize() == 0) {
      showErrorMessage(container, "There are no fields usable for reordering");
    } else {
      WindowManager windowManager = container.getActor(WindowManager.ROLE);
      assert windowManager != null;
      FrameBuilder builder = windowManager.createFrame("reorderAction");
      Configuration config = builder.getConfiguration()
        .getOrCreateSubset("reorderWindow")
        .getOrCreateSubset("Connection:" + connectionID);
      OrderSelector selector = new OrderSelector(config, orders);
      if (!selector.ensureOrderSelected(getDialogManager(container)))
        return;
      EditLifecycleImpl lifecycle = EditLifecycleImpl.install(builder, null);
      final ReorderWindow reorderWindow =
        new ReorderWindow(items, selector, columns, config, builder.getWindowContainer(), lifecycle);
      builder.setContent(reorderWindow);
      builder.setTitle("Reorder by Field");
      lifecycle.setDiscardConfirmation(reorderWindow.createCloseConfirmation());
      builder.showWindow();
    }
  }

  private static void showErrorMessage(ComponentContainer container, String message) {
    getDialogManager(container).showErrorMessage("Cannot Reorder", message);
  }

  private static DialogManager getDialogManager(ComponentContainer container) {
    DialogManager dialogManager = container.getActor(DialogManager.ROLE);
    assert dialogManager != null;
    return dialogManager;
  }
}
