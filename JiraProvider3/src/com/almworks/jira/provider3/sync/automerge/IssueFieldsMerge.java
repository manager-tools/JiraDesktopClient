package com.almworks.jira.provider3.sync.automerge;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.merge.AutoMergeLongSets;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Resolution;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class IssueFieldsMerge implements ItemAutoMerge {
  private static final List<DBAttribute<?>> EDITABLE_STATE_NO_DEFAULT = Collections15.unmodifiableListCopy(Issue.SUMMARY, Issue.DESCRIPTION, Issue.DUE, Issue.COMPONENTS,
    Issue.AFFECT_VERSIONS, Issue.FIX_VERSIONS);
  private static final List<DBAttribute<?>> EDITABLE_STATE_WITH_DEFAULT = Collections15.<DBAttribute<?>>unmodifiableListCopy(Issue.ASSIGNEE, Issue.PRIORITY);
  private static final List<DBAttribute<?>> EDITABLE_STATE;
  static {
    ArrayList<DBAttribute<?>> attributes = Collections15.arrayList();
    attributes.addAll(EDITABLE_STATE_NO_DEFAULT);
    attributes.addAll(EDITABLE_STATE_WITH_DEFAULT);
    EDITABLE_STATE = Collections.unmodifiableList(attributes);
  }
  private static final List<DBAttribute<Long>> HISTORY_AFFECTED = Collections15.unmodifiableListCopy(Issue.PROJECT, Issue.ISSUE_TYPE, Issue.STATUS);
  private final CustomFieldsComponent myCustomFields;

  public IssueFieldsMerge(CustomFieldsComponent customFieldEditors) {
    myCustomFields = customFieldEditors;
  }

  @Override
  public void preProcess(ModifiableDiff local) {
    Collection<? extends DBAttribute<?>> changes = local.getChanged();
    boolean changed = Containers.intersects(changes, EDITABLE_STATE);
    if (!changed) changed = isResolutionChanged(local);
    if (!changed) changed = isCustomFieldsChanged(local);
    if (!changed) {
      if (local.hasHistory()) changed = Containers.intersects(changes, HISTORY_AFFECTED);
    }
    if (changed) {
      local.addChange(EDITABLE_STATE_NO_DEFAULT);
      for (DBAttribute<?> attribute : EDITABLE_STATE_WITH_DEFAULT) {
        if (local.getElderValue(attribute) != null && local.getNewerValue(attribute) != null) local.addChange(attribute);
      }
      local.addChange(HISTORY_AFFECTED);
      local.addChange(Issue.RESOLUTION);
      addCustomFields(local);
    }
  }

  private boolean isResolutionChanged(ItemDiff local) {
    if (!local.isChanged(Issue.RESOLUTION)) return false;
    long elder = local.getElderNNValue(Issue.RESOLUTION, 0l);
    long newer = local.getElderNNValue(Issue.RESOLUTION, 0l);
    //noinspection SimplifiableIfStatement
    if (elder != 0 && newer != 0 && elder != newer) return true;
    return !Resolution.isUnresolved(local.getReader(), elder) && !Resolution.isUnresolved(local.getReader(), newer);
  }

  private void addCustomFields(ModifiableDiff issue) {
    long connection = issue.getNewerNNVale(SyncAttributes.CONNECTION, 0l);
    if (connection <= 0) {
      LogHelper.error("Missing connection", issue.getItem(), issue);
      return;
    }
    LongArray allFields = CustomField.queryKnownKey(issue.getReader(), connection);
    for (int i = 0; i < allFields.size(); i++) {
      ItemVersion field = issue.getNewerVersion().forItem(allFields.get(i));
      long attr = field.getNNValue(CustomField.ATTRIBUTE, 0l);
      DBAttribute<?> attribute = BadUtil.getAttribute(issue.getReader(), attr);
      if (attribute == null || !isControllingCustomField(issue.getNewerVersion(), attribute)) continue;
      issue.addChange(attribute);
    }
  }

  private boolean isCustomFieldsChanged(ItemDiff diff) {
    Collection<? extends DBAttribute<?>> attributes = diff.getChanged();
    for (DBAttribute<?> attribute : attributes) if (isControllingCustomField(diff.getNewerVersion(), attribute)) return true;
    return false;
  }

  private boolean isControllingCustomField(VersionSource source, DBAttribute<?> attribute) {
    if (attribute.getComposition() == DBAttribute.ScalarComposition.SET && attribute.getScalarClass() == Long.class) return false;
    long customField = CustomField.getCustomField(source.getReader(), attribute);
    return customField > 0 && myCustomFields.isEditable(source.forItem(customField));
  }

  @Override
  public void resolve(AutoMergeData data) {
    if (!data.hasHistory()) data.discardEdit(Issue.PROJECT, Issue.STATUS, Issue.ISSUE_TYPE, Issue.RESOLUTION);
    if (data.getLocal().isChanged(Issue.RESOLUTION)) {
      long server = data.getServer().getNewerNNVale(Issue.RESOLUTION, 0l);
      long local = data.getLocal().getNewerNNVale(Issue.RESOLUTION, 0l);
      if (server <= 0 || local <= 0 || server == local) {
        if (server == local || Resolution.isUnresolved(data.getReader(), server) || Resolution.isUnresolved(data.getReader(), local)) {
          if (server > 0) data.setResolution(Issue.RESOLUTION, server);
          else if (local > 0) data.setResolution(Issue.RESOLUTION, local);
          else data.setResolution(Issue.RESOLUTION, null);
        }
      }
    }
    resolveLocalNull(data, Issue.PRIORITY);
    resolveLocalNull(data, Issue.REPORTER);
    discardReadonly(data);
    mergeCustomSets(data);
    mergeCustomLocalNull(data);
  }

  private void mergeCustomLocalNull(AutoMergeData data) {
    for (DBAttribute<?> attribute : data.getUnresolved()) {
      long field = CustomField.getCustomField(data.getReader(), attribute);
      if (field <= 0) continue;
      resolveLocalNull(data, attribute);
    }
  }

  private void discardReadonly(AutoMergeData data) {
    for (DBAttribute<?> attribute : data.getUnresolved()) {
      long field = CustomField.getCustomField(data.getReader(), attribute);
      if (field <= 0) continue;
      if (myCustomFields.isEditable(data.getLocal().getNewerVersion().forItem(field))) data.discardEdit(attribute);
    }
  }

  private void mergeCustomSets(AutoMergeData data) {
    for (DBAttribute<?> attribute : data.getUnresolved()) {
      DBAttribute<? extends Collection<Long>> collectionAttr = BadUtil.castCollectionAttribute(Long.class, attribute);
      if (collectionAttr == null) continue;
      long field = CustomField.getCustomField(data.getReader(), collectionAttr);
      if (field <= 0) continue;
      AutoMergeLongSets.mergeSets(data, collectionAttr);
    }
  }

  private <T> void resolveLocalNull(AutoMergeData data, DBAttribute<T> attribute) {
    ItemDiff local = data.getLocal();
    if (local.isChanged(attribute) && local.getNewerValue(attribute) == null) {
      T serverValue = data.getServer().getNewerValue(attribute);
      if (serverValue != null) data.setResolution(attribute, serverValue);
    }
  }
}
