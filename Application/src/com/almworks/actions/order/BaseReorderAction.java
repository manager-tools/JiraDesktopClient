package com.almworks.actions.order;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.order.Order;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.TableController;
import com.almworks.explorer.ColumnsCollector;
import com.almworks.gui.ArtifactTableColumns;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.List;

public abstract class BaseReorderAction extends SimpleAction {
  protected BaseReorderAction() {
    super("Reorder by &Field\u2026", Icons.ACTION_REORDER);
    watchRole(TableController.DATA_ROLE);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.setEnabled(EnableState.INVISIBLE);
    TableController controller = context.getSourceObject(TableController.DATA_ROLE);
    AListModel<? extends LoadedItem> model = controller.getCollectionModel();
    context.updateOnChange(model);
    ItemCollectionContext collectionContext = CantPerformException.ensureNotNull(controller.getItemCollectionContext());
    CantPerformException.ensure(model.getSize() >= 2);
    Connection connection = CantPerformException.ensureNotNull(collectionContext.getSourceConnection());
    AListModel<? extends Order> orders = CantPerformException.ensureNotNull(getOrders(connection));
    context.updateOnChange(orders);
    context.setEnabled(EnableState.DISABLED);
    CantPerformException.ensure(orders.getSize() > 0);
    context.setEnabled(EnableState.ENABLED);
  }

  protected abstract AListModel<? extends Order> getOrders(Connection connection) throws CantPerformException;

  protected void doPerform(ActionContext context) throws CantPerformException {
    TableController controller = context.getSourceObject(TableController.DATA_ROLE);
    AListModel<? extends LoadedItem> artifactsModel = controller.getCollectionModel();
    List<LoadedItem> items = Collections15.arrayList(Collections15.linkedHashSet(artifactsModel.toList()));
    ItemCollectionContext contextInfo = CantPerformException.ensureNotNull(controller.getItemCollectionContext());
    Connection connection = CantPerformException.ensureNotNull(contextInfo.getSourceConnection());
    ArtifactTableColumns<LoadedItem> columns = context.getSourceObject(ColumnsCollector.ROLE).getColumns(connection);
    AListModel<? extends Order> ordersModel = getOrders(connection);
    ItemsLoader.showReorderWindow(context.getSourceObject(ComponentContainer.ROLE), items,
      ordersModel, connection.getConnectionID(), columns);
  }

}
