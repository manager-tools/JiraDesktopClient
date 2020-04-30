package com.almworks.engine.gui;

import com.almworks.api.application.*;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;

class DownloadStageMessage implements ChangeListener {
  private static final TypedKey<Object> MESSAGE_KEY = TypedKey.create("downloadStage");
  private final ModelMap myModel;
  private final ItemMessages myMessages;

  public DownloadStageMessage(ModelMap model, ItemMessages messages) {
    myModel = model;
    myMessages = messages;
  }

  public void attach(Lifespan life) {
    myModel.addAWTChangeListener(life, this);
    updateMessage();
  }

  public void onChange() {
    updateMessage();
  }

  private void updateMessage() {
    ItemDownloadStage stage = ItemDownloadStageKey.retrieveValue(myModel);
    if (stage != ItemDownloadStage.QUICK) myMessages.setMessage(MESSAGE_KEY, null);
    else {
      LoadedItemServices lis = LoadedItemServices.VALUE_KEY.getValue(myModel);
      String shortDescription;
      String longDescription;
      AnAction[] actions;
      if (lis != null) {
        MetaInfo metaInfo = lis.getMetaInfo();
        longDescription = metaInfo.getPartialDownloadHtml();
        shortDescription = metaInfo.getPartialDownloadShort();
        actions = new AnAction[]{lis.getActor(ActionRegistry.ROLE).getAction(MainMenu.Edit.DOWNLOAD)};
      } else {
        LogHelper.warning("Missing LoadedItemServices");
        return;
      }
      myMessages.setMessage(MESSAGE_KEY, ItemMessage.information(Icons.ATTENTION, shortDescription, longDescription, actions));
    }
  }
}
