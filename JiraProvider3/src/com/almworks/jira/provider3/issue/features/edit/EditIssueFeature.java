package com.almworks.jira.provider3.issue.features.edit;

import com.almworks.api.application.ItemWrapper;
import com.almworks.integers.WritableLongList;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.issue.editor.ScreenIssueEditor;
import com.almworks.jira.provider3.issue.features.BaseEditIssueFeature;
import com.almworks.jira.provider3.issue.features.DescriptorWindowTitle;
import com.almworks.jira.provider3.issue.features.edit.screens.ScreenChooser;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.UpdateRequest;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class EditIssueFeature extends BaseEditIssueFeature {
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(EditIssueFeature.class.getClassLoader(), "com/almworks/jira/provider3/issue/features/message");

  private static final ScreenIssueEditor EDITOR = new ScreenIssueEditor(false, new ScreenChooser(BaseEditIssueFeature.SCHEME, false));
  private static final DescriptorWindowTitle TITLE = DescriptorWindowTitle.create("edit.screens.window.edit.title.single", "edit.screens.window.edit.title.multi");

  public static final EditFeature EDIT = new EditIssueFeature();
  public static final EditFeature CREATE_HERE = new NewIssueHereFeature();
  public static final EditFeature CREATE_GENERIC = new CreateIssueFeature(false);
  public static final EditFeature CREATE_SUBTASK = new CreateIssueFeature(true);
  public static final EditFeature MERGE = new MergeFeature();

  public EditIssueFeature() {
    super(EDITOR);
  }

  @NotNull
  @Override
  public EditDescriptor.Impl doCheckContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(ItemWrapper.ITEM_WRAPPER);
    Pair<JiraConnection3, List<ItemWrapper>> pair = getContextIssues(context);
    List<ItemWrapper> issues = pair.getSecond();
    int issueCount = issues.size();
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame("edit.", TITLE.createTitle(issues), getWindowPrefSize());
    if (issueCount > 1) {
      descriptor.putPresentationProperty(PresentationKey.NAME, I18N.messageInt("edit.screens.action.edit.name.multi").formatMessage(issueCount));
      descriptor.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, I18N.getString("edit.screens.action.edit.description.multi"));
    }
    descriptor.setActionScope("ArtifactEditor");
    descriptor.setDescriptionStrings(
      I18N.messageInt("edit.screens.window.edit.notUploaded.title").formatMessage(issueCount),
      I18N.messageInt("edit.screens.window.edit.notUploaded.message").formatMessage(issueCount),
      I18N.messageInt("edit.screens.window.edit.saveDescription").formatMessage(issueCount),
      I18N.messageInt("edit.screens.window.edit.uploadDescription").formatMessage(issueCount));
    commonDescriptorSetup(descriptor);
    return descriptor;
  }

  static Dimension getWindowPrefSize() {
    return new Dimension(500, 400);
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    Pair<JiraConnection3, List<ItemWrapper>> pair = getContextIssues(context);
    return setupEditIssues(itemsToLock, pair.getFirst(), pair.getSecond());
  }
}
