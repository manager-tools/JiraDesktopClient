package com.almworks.jira.provider3.gui.actions;

import com.almworks.actions.order.BaseReorderAction;
import com.almworks.api.application.order.Order;
import com.almworks.api.engine.Connection;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.ui.actions.CantPerformException;

class JiraReorderAction extends BaseReorderAction {
  @Override
  protected AListModel<? extends Order> getOrders(Connection c) throws CantPerformException {
    JiraConnection3 connection = CantPerformException.cast(JiraConnection3.class, c);
    CantPerformException.ensure(connection.isUploadAllowed());
    return connection.getReordersModel();
  }
}
