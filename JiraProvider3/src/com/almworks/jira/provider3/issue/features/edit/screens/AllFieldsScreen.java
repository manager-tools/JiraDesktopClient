package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.Pair;
import com.almworks.util.images.Icons;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class AllFieldsScreen extends EditIssueScreen {
  private final List<String> myModifiableFields = Collections15.arrayList();
  private final List<String> myFields = Collections.unmodifiableList(myModifiableFields);

  private AllFieldsScreen() {
    super(EditIssueFeature.I18N.getFactory("edit.screens.generated.all.name"), "all", Icons.JIRA_CLIENT_SMALL);
  }

  @Override
  public String getTooltip() {
    return getName() + ": " + EditIssueFeature.I18N.getString("edit.screens.generated.all.tooltip");
  }

  void setFields(Collection<ResolvedField> fields) {
    myModifiableFields.clear();
    List<ResolvedField> leftFields = Collections15.arrayList(fields);
    for (ServerFields.Field field : STATIC_DEFAULT_ORDER) {
      ResolvedField found = ResolvedField.findStatic(leftFields, field);
      if (found != null) {
        myModifiableFields.add(field.getJiraId());
        leftFields.remove(found);
      }
    }
    Collections.sort(leftFields, ResolvedField.BY_DISPLAY_NAME);
    for (ResolvedField field : leftFields) myModifiableFields.add(field.getJiraId());
  }

  public List<Tab> getTabs(EditModelState model) {
    return Collections.singletonList(new Tab("", myFields));
  }

  @Nullable
  @Override
  public Object checkModelState(EditModelState model, @Nullable Object prevState) {
    Long project = model.getSingleEnumValue(Issue.PROJECT);
    Long type = model.getSingleEnumValue(Issue.ISSUE_TYPE);
    Pair pair = Util.castNullable(Pair.class, prevState);
    if (pair != null && Util.equals(project, pair.getFirst()) && Util.equals(type, pair.getSecond())) return null;
    return Pair.create(project, type);
  }

  public static void load(ItemVersion connection, Collection<EditIssueScreen> target) {
    List<ResolvedField> allFields = ResolvedField.loadAll(connection);
    AllFieldsScreen allScreen = new AllFieldsScreen();
    allScreen.setFields(allFields);
    target.add(allScreen);
  }
}
