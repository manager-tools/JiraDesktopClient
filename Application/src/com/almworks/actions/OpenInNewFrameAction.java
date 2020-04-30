package com.almworks.actions;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.util.Terms;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author Stalex
 */
public class OpenInNewFrameAction extends SimpleAction {
  public OpenInNewFrameAction() {
    super("View " + Terms.ref_Artifact + " in a Separate Window", Icons.OPEN_ARTIFACT_IN_FRAME);
    setDefaultPresentation(PresentationKey.SHORTCUT, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    watchRole(LoadedItem.LOADED_ITEM);
    watchRole(ItemWrapper.ITEM_WRAPPER);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    context.setEnabled(true);
  }

  protected void doPerform(final ActionContext context) throws CantPerformException {
    final ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
    ItemWrapper wrapper = context.getSourceObject(LoadedItem.LOADED_ITEM);
    explorer.showItemInWindow(wrapper);
  }
}