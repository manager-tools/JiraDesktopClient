package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.issue.editor.IssueScreen;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.util.images.Icons;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

class RelevantScreen extends EditIssueScreen {
  private final RelevantFields myRelevantFields;

  private RelevantScreen(@NotNull RelevantFields relevantFields) {
    super(EditIssueFeature.I18N.getFactory("edit.screens.generated.relevant.name"), "relevant", Icons.JIRA_CLIENT_SMALL);
    myRelevantFields = relevantFields;
  }

  @Nullable
  public static EditIssueScreen choose(Iterable<? extends IssueScreen> screens) {
    AllFieldsScreen allFields = null;
    for (IssueScreen s : screens) {
      RelevantScreen relevant = Util.castNullable(RelevantScreen.class, s);
      if (relevant != null) return relevant;
      AllFieldsScreen screen = Util.castNullable(AllFieldsScreen.class, s);
      if (screen != null) allFields = screen;
    }
    return allFields;
  }

  @Nullable
  @Override
  public Object checkModelState(EditModelState model, @Nullable Object prevState) {
    return myRelevantFields.checkModelState(model, prevState);
  }

  @Override
  public String getTooltip() {
    return getName() + ": " + EditIssueFeature.I18N.getString("edit.screens.generated.relevant.tooltip");
  }

  @Override
  public List<Tab> getTabs(EditModelState model) {
    return Collections.singletonList(new Tab("", myRelevantFields.getFieldIds(model)));
  }

  public static void load(VersionSource source, EditItemModel model, List<EditIssueScreen> target) {
    RelevantFields relevantFields = RelevantFields.ensureLoaded(source, model);
    if (relevantFields == null) return;
    target.add(new RelevantScreen(relevantFields));
  }
}
