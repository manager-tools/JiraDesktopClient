package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.gui.edit.fields.LoadedFieldInfo;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class RelevantFields {
  private static final TypedKey<RelevantFields> KEY = TypedKey.create(RelevantFields.class);

  private static final Set<ServerFields.Field> MANDATORY_FIELDS = Collections15.hashSet(ServerFields.PROJECT, ServerFields.ISSUE_TYPE);

  private final long myProject;
  private final long myType;
  @Nullable
  private final ArrayList<String> myRelevantFields;
  private final LoadedFieldInfo myFieldsInfo;

  RelevantFields(long project, long type, @Nullable ArrayList<String> relevantFields, LoadedFieldInfo fieldsInfo) {
    myProject = project;
    myType = type;
    myRelevantFields = relevantFields;
    myFieldsInfo = fieldsInfo;
  }

  @Nullable
  public static RelevantFields ensureLoaded(VersionSource source, EditItemModel model) {
    RelevantFields loaded = model.getRootModel().getValue(KEY);
    if (loaded != null) return loaded;
    LoadedFieldInfo info = LoadedFieldInfo.ensureLoaded(source, model);
    if (info == null) return null;
    LongSet applicableToAtLeastOne = new LongSet();
    LongList editingItems = model.getEditingItems();
    Long commonProject = null;
    Long commonType = null;
    ArrayList<String> fieldIds;
    if (editingItems.isEmpty()) fieldIds = null;
    else {
      for (ItemVersion issue : source.readItems(editingItems)) {
        LongList fields = Issue.FIELDS_FOR_EDIT.getValue(issue);
        if (fields == null) continue;
        applicableToAtLeastOne.addAll(fields);
        Long project = issue.getValue(Issue.PROJECT);
        if (commonProject == null) commonProject = project;
        else if (project != null && !commonProject.equals(project)) return null; // No common project
        Long type = issue.getValue(Issue.ISSUE_TYPE);
        if (commonType == null) commonType = type;
        else if (type != null && !commonType.equals(type)) return null; // No common type
      }
      if (applicableToAtLeastOne.isEmpty()) fieldIds = null;
      else {
        fieldIds = Collections15.arrayList();
        for (ServerFields.Field staticField : EditIssueScreen.STATIC_DEFAULT_ORDER) {
          long materialized = staticField.findItem(source);
          if (materialized <= 0) continue;
          if (MANDATORY_FIELDS.contains(staticField)
            ||applicableToAtLeastOne.contains(materialized)) fieldIds.add(staticField.getJiraId());
        }
        ArrayList<ResolvedField> fields = ResolvedField.load(source, applicableToAtLeastOne);
        Collections.sort(fields, ResolvedField.BY_DISPLAY_NAME);
        for (ResolvedField field : fields) if (!field.isStatic()) fieldIds.add(field.getJiraId());
      }
    }
    RelevantFields fields = new RelevantFields(Util.NN(commonProject, -1l), Util.NN(commonType, -1l), fieldIds, info);
    model.getRootModel().putHint(KEY, fields);
    return fields;
  }

  @NotNull
  public List<String> getFieldIds(EditModelState model) {
    Long project = model.getSingleEnumValue(Issue.PROJECT);
    Long type = model.getSingleEnumValue(Issue.ISSUE_TYPE);
    List<String> fields;
    if (myRelevantFields != null
      && (myProject <= 0 || project == null || project == myProject)
      && (myType <= 0 || type == null || type == myType))
      fields = myRelevantFields;
    else {
      Set<ResolvedField> relevantFields = myFieldsInfo.getFields(project, type);
      ArrayList<ResolvedField> customFields = Collections15.arrayList();
      for (ResolvedField field : relevantFields) if (!field.isStatic()) customFields.add(field);
      Collections.sort(customFields, ResolvedField.BY_DISPLAY_NAME);
      fields = Collections15.arrayList();
      for (ServerFields.Field field : EditIssueScreen.STATIC_DEFAULT_ORDER) if (ResolvedField.containsStatic(relevantFields, field)) fields.add(field.getJiraId());
      fields.addAll(ResolvedField.GET_JIRA_ID.collectList(customFields));
    }
    return Collections.unmodifiableList(fields);
  }

  public Object checkModelState(EditModelState model, Object prevState) {
    Long project = model.getSingleEnumValue(Issue.PROJECT);
    Long type = model.getSingleEnumValue(Issue.ISSUE_TYPE);
    Pair pair = Util.castNullable(Pair.class, prevState);
    if (pair != null && Util.equals(project, pair.getFirst()) && Util.equals(type, pair.getSecond())) return null;
    return Pair.create(project, type);
  }
}
