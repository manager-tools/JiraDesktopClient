package com.almworks.engine.gui.attachments;

import com.almworks.util.images.Icons;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.*;

import javax.swing.*;

class SelectAttachmentViewModeAction extends SimpleAction {
  private static final ComponentProperty<AttachmentsViewMode> VIEW_MODE = ComponentProperty.createProperty("VIEW_MODE");
  private static AnAction[] ourActions = {new SelectAttachmentViewModeAction(true), new SelectAttachmentViewModeAction(false)};

  private final boolean myThumbnails;

  private SelectAttachmentViewModeAction(boolean thumbnails) {
    super(thumbnails ? "View Thumbnails" : "View Details", thumbnails ? Icons.FILE_VIEW_THUMBNAILS : Icons.FILE_VIEW_DETAILS);
    myThumbnails = thumbnails;
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, thumbnails ?
    "Switch to attachment thumbnails view" : "Switch to attachment list view");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    AttachmentsViewMode mode = context.getSourceObject(VIEW_MODE);
    context.updateOnChange(mode.getModifiable());
    boolean toggled = !(myThumbnails ^ mode.isThumbnailsMode());
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, toggled);
    context.setEnabled(true);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    AttachmentsViewMode mode = context.getSourceObject(VIEW_MODE);
    if (myThumbnails) {
      mode.setThumbnailsMode();
    } else {
      mode.setTableMode();
    }
  }

  public static AnAction[] setup(JComponent context, AttachmentsViewMode viewMode) {
    DataProvider.DATA_PROVIDER.putClientValue(context, ConstProvider.singleData(VIEW_MODE, viewMode));
    return ourActions;
  }
}
