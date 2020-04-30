package com.almworks.extservice;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.tracker.alpha.AlphaProtocol;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import com.almworks.util.xmlrpc.SimpleOutgoingMessage;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Vector;

class FindExternallyAction extends EapiClientAction {
  public static final String DESCRIPTION = "Locate references to selected " + Terms.ref_artifacts + " in external tools";

  public FindExternallyAction(OrderListModel<EapiClient> clientsModel) {
    super("External &Search", Icons.SEARCH_IN_IDE, clientsModel);
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, DESCRIPTION);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.ALT_DOWN_MASK));
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
  }                                     

  protected void updateUI(UpdateContext context) {
    super.updateUI(context);
    context.putPresentationProperty(PresentationKey.NAME, adjusted("Search in $", "External Search"));
    context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION,
      adjusted(Local.parse("Locate references to selected " + Terms.ref_artifacts + " in $"), Local.parse(DESCRIPTION)));
  }

  protected void customUpdate2(UpdateContext context) throws CantPerformException {
    ItemActionUtils.basicUpdate(context, false);
    context.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<ItemWrapper> list = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    Vector vector = new Vector();
    for (ItemWrapper wrapper : list) {
      String url = wrapper.getItemUrl();
      if (url != null)
        vector.add(url);
    }
    EapiClient client = selectClient(context);
    if (client != null)
      client.send(new SimpleOutgoingMessage(AlphaProtocol.Messages.ToClient.FIND_ARTIFACTS, vector));
  }
}
