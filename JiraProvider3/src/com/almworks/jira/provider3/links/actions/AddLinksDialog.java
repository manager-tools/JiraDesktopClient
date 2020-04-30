package com.almworks.jira.provider3.links.actions;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.DataVerification;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.DataVerificationFailure;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

class AddLinksDialog {
  private static final AddLinksEditor EDITOR = AddLinksEditor.linkIssues(NameMnemonic.rawText("T&argets"), NameMnemonic.rawText("Link &Type"));
  private static final TypedKey<DefaultEditModel.Root> ADD_LINK_PROTO_MODEL = TypedKey.create("addLink/protoModel");

  public static void prepareModel(VersionSource source, EditItemModel issueModel, EditPrepare editPrepare) {
    DefaultEditModel.Root proto = DefaultEditModel.Root.editItems(LongList.EMPTY);
    JiraConnection3 connection = EngineConsts.getConnection(JiraConnection3.class, issueModel);
    EngineConsts.setupConnection(proto, connection);
    EDITOR.prepareModel(source, proto, editPrepare);
    issueModel.putHint(ADD_LINK_PROTO_MODEL, proto);
  }

  public static boolean checkModel(EditItemModel model) {
    return model.getValue(ADD_LINK_PROTO_MODEL) != null;
  }

  public static Pair<DirectionalLinkType, List<String>> show(ActionContext context, EditItemModel issueModel, String thisKey) throws CantPerformException {
    final DefaultEditModel.Root model = createAddLinksModel(issueModel);
    final CreateLinksOutboundForm form = new CreateLinksOutboundForm(context.getSourceObject(GuiFeaturesManager.ROLE));
    form.setSourceKeys(Collections.singletonList(thisKey));
    DetachComposite life = new DetachComposite();
    EDITOR.attach(life, model, form.getLinkType(), form.getOppositeIssues());
    DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createBuilder("JIRA.Links.addLinksToEditor");
    builder.setModal(true);
    builder.setTitle("Add Links to " + thisKey);
    builder.setEmptyCancelAction();
    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(form.getWholePanel(), BorderLayout.CENTER);
    panel.add(DataVerificationFailure.install(life, model), BorderLayout.SOUTH);
    builder.setContent(panel);
    builder.setInitialFocusOwner(form.getLinkType());
    final boolean[] add = {false};
    builder.setOkAction(new SimpleAction("OK") {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(form.getOppositeIssues().getDocument());
        context.updateOnChange(form.getLinkType().getModel());
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
    return Pair.create(EDITOR.getLinkType(model), EDITOR.getIssueKeys(model));
  }

  private static DefaultEditModel.Root createAddLinksModel(EditItemModel issueModel) throws CantPerformException {
    DefaultEditModel.Root proto = CantPerformException.ensureNotNull(issueModel.getValue(ADD_LINK_PROTO_MODEL));
    return proto.copyState();
  }

  public static void updateLastLink(EditItemModel issueModel, DirectionalLinkType linkType) {
    DefaultEditModel.Root addLinkModel = issueModel.getValue(ADD_LINK_PROTO_MODEL);
    if (addLinkModel == null || linkType == null) return;
    EDITOR.setLinkType(addLinkModel, linkType);
  }
}
