package com.almworks.jira.provider3.attachments;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.text.FileNameEditor;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.util.TopEditor;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.schema.Attachment;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AttachmentNameTopEditor extends TopEditor {
  public static final AttachmentNameTopEditor INSTANCE = new AttachmentNameTopEditor();
  private static final FileNameEditor EDITOR = new FileNameEditor(NameMnemonic.rawText("Name"), Attachment.ATTACHMENT_NAME, null);

  private AttachmentNameTopEditor() {
    super(NameMnemonic.rawText("Rename Attachment"));
  }

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(AttachmentImpl.ROLE);
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.notModal("renameAttachment.", "Rename Attachment", new Dimension(300, 95));
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.setContextKey(JiraEditUtils.getContextKey(this, context));
    JiraEditUtils.checkAnyConnectionAllowsEdit(context, descriptor);
    Connection conn = context.getSourceObject(ItemWrapper.ITEM_WRAPPER).getConnection();
    JiraConnection3 conn3 = CantPerformException.cast(JiraConnection3.class, conn);
    CantPerformException.ensure(conn3.isUploadAllowed());
    AttachmentImpl att = context.getSourceObject(AttachmentImpl.ROLE);
    CantPerformException.ensure(att.isLocal());
    long attItem = att.getItem();
    if (context.getSourceObject(SyncManager.ROLE).findLock(attItem) != null) throw new CantPerformException();
    descriptor.setDescriptionStrings(
      "Rename attachment",
      "New attachment name was saved in the local database",
      "Save new attachment name in the local database without uploading to server",
      "Save new attachment name and upload all issue changes to server");
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    AttachmentImpl att = context.getSourceObject(AttachmentImpl.ROLE);
    long attItem = att.getItem();
    DefaultEditModel.Root model = DefaultEditModel.Root.editItems(LongArray.create(attItem));
    itemsToLock.add(attItem);
    return model;
  }

  @Override
  protected JComponent doEditModel(Lifespan life, EditItemModel model, Configuration config) {
    return VerticalLinePlacement.buildTopComponent(life, model, model.getAllEditors());
  }

  @Override
  protected void doCommit(CommitContext childContext) throws CancelCommitException {
    if (childContext.getManager().isDuringUpload(childContext.getReader(), childContext.getItem())) return;
    if (childContext.readTrunk().getValue(Attachment.FILE_URL) != null) return;
    childContext.commitEditors(null);
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    return createDefaultNestedModel(parent, EDITOR);
  }

  public void attachComponent(DetachComposite life, DefaultEditModel.Root model, JTextField field) {
    EDITOR.attachComponent(life, getNestedModel(model), field);
  }

  public String getCurrentTextValue(DefaultEditModel.Root model) {
    return EDITOR.getNameToCommit(getNestedModel(model));
  }

  public void setValue(DefaultEditModel.Root model, String value) {
    EDITOR.setValue(getNestedModel(model), value);
  }
}