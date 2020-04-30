package com.almworks.jira.provider3.issue.features.move;

import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.LongArray;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.ComponentHolder;
import com.almworks.items.gui.edit.editors.HtmlMessage;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.gui.edit.EditMetaSchema;
import com.almworks.jira.provider3.gui.edit.EditorsScheme;
import com.almworks.jira.provider3.gui.edit.editors.move.MoveController;
import com.almworks.jira.provider3.issue.editor.ScreenIssueEditor;
import com.almworks.jira.provider3.issue.editor.ScreenSet;
import com.almworks.jira.provider3.issue.features.BaseEditIssueFeature;
import com.almworks.jira.provider3.issue.features.DescriptorWindowTitle;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.jira.provider3.issue.util.ScreenBuilder;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.jetbrains.annotations.NotNull;

public class MoveIssueFeature extends BaseEditIssueFeature {
  private static final ScreenIssueEditor EDITOR;

  static {
    String hexColor = ColorUtil.formatColor(
      ColorUtil.between(UIUtil.getEditorForeground(), DocumentFormAugmentor.backgroundColor(), 0.5f));
    FieldEditor hintMessage = HtmlMessage.wideLine("<html>Hints <font color=" + hexColor + ">(not used unless required by Jira)</font></html>", true);
    EditorsScheme scheme = new EditorsScheme(BaseEditIssueFeature.SCHEME)
      .addEditor(ServerFields.STATUS, EditMetaSchema.STATUS)
      .addEditor(ServerFields.ISSUE_TYPE, MoveController.MOVE_ISSUE_TYPE)
      .fix();
    ScreenBuilder builder = new ScreenBuilder(scheme);
    builder.addField(ServerFields.PROJECT);
    builder.addField(ServerFields.ISSUE_TYPE);
    builder.addMockEditor(ComponentHolder.createVerticalSpacer(0));
    builder.addMockEditor(hintMessage);
    builder.addMockEditor(ComponentHolder.createVerticalSpacer(0));
    builder.addField(ServerFields.STATUS);
    builder.addField(ServerFields.AFFECT_VERSIONS);
    builder.addField(ServerFields.FIX_VERSIONS);
    builder.addField(ServerFields.COMPONENTS);
    builder.addMockEditor(ComponentHolder.createVerticalSpacer(5));
    builder.addField(ServerFields.COMMENTS);
    ScreenSet screenSet = builder.buildScreenSet();
    EDITOR = new ScreenIssueEditor(false, screenSet);
  }

  private static final DescriptorWindowTitle TITLE = DescriptorWindowTitle.create("edit.screens.window.move.title.single", "edit.screens.window.move.title.multi");
  public static final EditFeature INSTANCE = new MoveIssueFeature();

  public MoveIssueFeature() {
    super(EDITOR);
  }

  @NotNull
  @Override
  protected EditDescriptor.Impl doCheckContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchModifiableRole(SyncManager.MODIFIABLE);
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    MoveKind kind = MoveKind.chooseMove(context);
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("move.", TITLE.createTitle(kind.getIssues()), null);
    descriptor.putPresentationProperty(PresentationKey.ENABLE, EnableState.INVISIBLE);
    descriptor.setDescriptionStrings(
      EditIssueFeature.I18N.getString("edit.screens.window.move.notUploaded.title"),
      EditIssueFeature.I18N.getString("edit.screens.window.move.notUploaded.message"),
      EditIssueFeature.I18N.getString("edit.screens.window.move.saveDescription"),
      EditIssueFeature.I18N.getString("edit.screens.window.move.uploadDescription"));
    if (!kind.getConnection().isUploadAllowed()) return descriptor;
    descriptor.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
    descriptor.putPresentationProperty(PresentationKey.NAME, kind.getActionName());
    descriptor.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, kind.getActionDescription());
    commonDescriptorSetup(descriptor);
    return descriptor;
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    MoveKind kind = MoveKind.chooseMove(context);
    LongArray issueItems = kind.getIssueItems();
    if (issueItems.isEmpty()) throw new CantPerformException();
    return setupEditIssues(itemsToLock, kind.getConnection(), issueItems);
  }
}
