package com.almworks.jira.provider3.custom.impl;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.schema.applicability.Applicabilities;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.gui.meta.schema.columns.Columns;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.items.gui.meta.schema.dnd.DnDChange;
import com.almworks.items.gui.meta.schema.export.Exports;
import com.almworks.items.gui.meta.schema.gui.ViewerField;
import com.almworks.items.gui.meta.schema.reorders.Reorders;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.fieldtypes.enums.RelevantApplicability;
import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.tags.TagsComponentImpl;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class RemoteMetaConfig {
  private final Map<String, FieldKind> myFieldKinds;

  public RemoteMetaConfig(Map<String, FieldKind> fieldKinds) {
    myFieldKinds = fieldKinds;
  }

  public Map<String, FieldKind> getFieldKinds() {
    return myFieldKinds;
  }

  public void updateMetaInfo(DBDrain drain, DBIdentity connection) {
    List<DBStaticObject> viewerFields = Collections15.arrayList();
    addCommonFields(viewerFields);
    DBReader reader = drain.getReader();
    LongArray customFields = reader.query(DPEqualsIdentified.create(SyncAttributes.CONNECTION, connection)
      .and(DPEqualsIdentified.create(DBAttribute.TYPE, CustomField.DB_TYPE))).copyItemsSorted();
    StaticObjectsWriter metaWriter =
      StaticObjectsWriter.create(drain, connection, Columns.DB_TYPE, Descriptors.DB_TYPE, DnDChange.DB_TYPE, Exports.DB_TYPE, Reorders.DB_TYPE, ViewerField.DB_TYPE);
    for (ItemVersionCreator field : drain.changeItems(customFields)) {
      ScalarSequence applicability = getApplicability(field);
      Applicability.ATTRIBUTE.setIfChanged(field, applicability);
      FieldInfo info = createCustomFieldInfo(field, connection, applicability);
      if (info == null) continue;
      metaWriter.write(info.createColumn());
      metaWriter.write(info.createDescriptor());
      if (info.hasDnDChange()) metaWriter.write(info.createDnDChange());
      if (info.hasExport()) metaWriter.write(info.createExport());
      metaWriter.write(info.createReorder());
      DBStaticObject viewerField = info.createViewerField();
      if (viewerField != null) {
        viewerFields.add(viewerField);
        metaWriter.write(viewerField);
      }
    }
    metaWriter.deleteNotWritten();
    viewerFields.add(ViewerField.SEPARATOR);
    viewerFields.add(TagsComponentImpl.VIEWER);
    ViewerField.CONNECTION_FIELDS.setValue(drain.changeItem(connection), viewerFields);
  }

  @NotNull
  private static ScalarSequence getApplicability(ItemVersionCreator field) {
    LongList projects = CustomField.ONLY_IN_PROJECTS.getValue(field);
    LongList issueTypes = CustomField.ONLY_IN_ISSUE_TYPES.getValue(field);
    if (projects.isEmpty() && issueTypes.isEmpty()) return RelevantApplicability.sequence(field.getItem(), null);
    return Applicabilities.satisfyAll(
      Applicabilities.enumSubset(Issue.PROJECT, MetaSchema.KEY_PROJECT, projects),
      Applicabilities.enumSubset(Issue.ISSUE_TYPE, MetaSchema.KEY_ISSUE_TYPE, issueTypes));
  }

  @Nullable
  private FieldInfo createCustomFieldInfo(ItemVersion field, DBIdentity connectionIdentity, ScalarSequence applicability) {
    String key = field.getValue(CustomField.KEY);
    DBAttribute<?> attribute = BadUtil.getAttribute(field.getReader(), field.getValue(CustomField.ATTRIBUTE));
    if (key == null || attribute == null) return null;
    String id = field.getValue(CustomField.ID);
    String name = field.getValue(CustomField.NAME);
    FieldKind kind = myFieldKinds.get(key);
    if (kind == null) kind = FieldKeysLoader.UNKNOWN;
    ItemVersion connection = field.readValue(SyncAttributes.CONNECTION);
    String connectionId = connection != null ? connection.getValue(SyncAttributes.CONNECTION_ID) : null;
    String connectionIdPrefix = connectionId == null || connectionId.length() == 0 ? "" : connectionId + "_";
    return kind.createFieldInfo(field, id, name, attribute, connectionIdentity, connectionIdPrefix, applicability);
  }

  private void addCommonFields(List<DBStaticObject> viewerFields) {
    viewerFields.add(MetaSchema.FIELD_PROJECT);
    viewerFields.add(MetaSchema.FIELD_ISSUE_TYPE);
    viewerFields.add(MetaSchema.FIELD_SECURITY_LEVEL);
    viewerFields.add(ViewerField.SEPARATOR);

    viewerFields.add(MetaSchema.FIELD_STATUS);
    viewerFields.add(MetaSchema.FIELD_RESOLUTION);
    viewerFields.add(MetaSchema.FIELD_DUE);
    viewerFields.add(MetaSchema.FIELD_PRIORITY);
    viewerFields.add(ViewerField.SEPARATOR);

    viewerFields.add(MetaSchema.FIELD_CREATED);
    viewerFields.add(MetaSchema.FIELD_UPDATED);
    viewerFields.add(MetaSchema.FIELD_RESOLVED);
    viewerFields.add(MetaSchema.FIELD_TIME_SPENT);
    viewerFields.add(MetaSchema.FIELD_REMAIN_ESTIMATE);
    viewerFields.add(MetaSchema.FIELD_ORIGINAL_ESTIMATE);
    viewerFields.add(ViewerField.SEPARATOR);

    viewerFields.add(MetaSchema.FIELD_REPORTER);
    viewerFields.add(MetaSchema.FIELD_ASSIGNEE);
    viewerFields.add(ViewerField.SEPARATOR);

    viewerFields.add(MetaSchema.FIELD_COMPONENTS);
    viewerFields.add(MetaSchema.FIELD_AFFECT_VERSIONS);
    viewerFields.add(MetaSchema.FIELD_FIX_VERSIONS);
    viewerFields.add(ViewerField.SEPARATOR);

    viewerFields.add(MetaSchema.FIELD_VOTES_COUNT);
    viewerFields.add(MetaSchema.FIELD_VOTERS);
    viewerFields.add(MetaSchema.FIELD_WATCHERS_COUNT);
    viewerFields.add(MetaSchema.FIELD_WATCHERS);
    viewerFields.add(ViewerField.SEPARATOR);
  }
}
