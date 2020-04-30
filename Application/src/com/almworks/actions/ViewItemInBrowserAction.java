package com.almworks.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.Connection;
import com.almworks.gui.CommonMessages;
import com.almworks.util.Env;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author dyoma
 */
public class ViewItemInBrowserAction extends SimpleAction {
  public ViewItemInBrowserAction() {
    super(CommonMessages.OPEN_IN_BROWSER, Icons.ACTION_OPEN_IN_BROWSER);
    KeyStroke key = Env.isMac() ? KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.META_DOWN_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
    setDefaultPresentation(PresentationKey.SHORTCUT, key);
    watchRole(LoadedItem.LOADED_ITEM);
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ItemWrapper wrapper = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    Connection connection = CantPerformException.ensureNotNull(wrapper.getConnection());
    if (connection.getState().getValue().isDegrading()) {
      context.setEnabled(false);
      return;
    }
    String url = wrapper.getItemUrl();
    context.setEnabled(url != null && connection.isItemUrl(url));
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    ItemWrapper wrapper = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    ExternalBrowser.openURL(wrapper.getItemUrl() + "#", false);
  }
}
