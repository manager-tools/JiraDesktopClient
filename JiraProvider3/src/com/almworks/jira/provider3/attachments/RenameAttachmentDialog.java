package com.almworks.jira.provider3.attachments;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.DataVerification;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.DataVerificationFailure;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class RenameAttachmentDialog {
  private static final TypedKey<DefaultEditModel.Root> ATTACHMENT_PROTO_MODEL = TypedKey.create("attachment/protoModel");
  private static final AttachmentNameTopEditor EDITOR = AttachmentNameTopEditor.INSTANCE;

  public static void prepareModel(VersionSource source, EditItemModel issueModel, EditPrepare editPrepare) {
    DefaultEditModel.Root proto = DefaultEditModel.Root.editItems(LongList.EMPTY);
    EDITOR.prepareModel(source, proto, editPrepare);
    issueModel.putHint(ATTACHMENT_PROTO_MODEL, proto);
  }

  public static boolean checkModel(EditItemModel model) {
    return model.getValue(ATTACHMENT_PROTO_MODEL) != null;
  }

  public static String show(ActionContext context, EditItemModel issueModel, String thisKey, String oldName) throws CantPerformException {
    final DefaultEditModel.Root model = createAttachmentModel(issueModel, oldName);
    DetachComposite life = new DetachComposite();

    final JTextField textField = new JTextField();
    EDITOR.attachComponent(life, model, textField);
    final List<ComponentControl> components = Collections15.arrayList();
    FieldEditorUtil.createComponents(life, model, model.getAllEditors(), components, null);

    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(VerticalLinePlacement.buildComponent(life, model, components), BorderLayout.CENTER);
    panel.add(DataVerificationFailure.install(life, model), BorderLayout.SOUTH);
    new DocumentFormAugmentor(UIManager.getColor("Panel.background")).augmentForm(life, panel, false);

    DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createBuilder("JIRA.Attachments.editorRenameAttachment");
    builder.setModal(true);
    builder.setTitle("Rename Attachment of " + Util.NN(thisKey, "New Issue"));
    builder.setContent(panel);
    builder.setInitialFocusOwner(textField);
    builder.setEmptyCancelAction();
    final boolean[] add = {false};
    builder.setOkAction(new SimpleAction("OK") {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(textField.getDocument());
        CantPerformException.ensure(model.hasDataToCommit() && !model.verifyData(DataVerification.Purpose.ANY_ERROR).hasErrors());
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        add[0] = true;
        CantPerformExceptionExplained explained = context.getSourceObject(WindowController.ROLE).close();
        if (explained != null) throw explained;
      }
    });
    builder.showWindow(life);
    if (!add[0]) return null;
    return EDITOR.getCurrentTextValue(model);
  }

  private static DefaultEditModel.Root createAttachmentModel(EditItemModel issueModel, String oldName) throws CantPerformException {
    DefaultEditModel.Root proto = CantPerformException.ensureNotNull(issueModel.getValue(ATTACHMENT_PROTO_MODEL));
    DefaultEditModel.Root model = proto.copyState();
    EDITOR.setValue(model, oldName);
    model.saveInitialValues();
    return model;
  }
}
