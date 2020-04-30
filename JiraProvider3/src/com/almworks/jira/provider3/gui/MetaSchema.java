package com.almworks.jira.provider3.gui;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.columns.ColumnComparator;
import com.almworks.items.gui.meta.schema.columns.ColumnRenderer;
import com.almworks.items.gui.meta.schema.columns.Columns;
import com.almworks.items.gui.meta.schema.columns.SizePolicies;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.items.gui.meta.schema.dnd.DnDChange;
import com.almworks.items.gui.meta.schema.export.ExportPolicy;
import com.almworks.items.gui.meta.schema.export.Exports;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.gui.meta.util.GetModelKeyValue;
import com.almworks.items.gui.meta.util.MultiEnumInfo;
import com.almworks.items.gui.meta.util.ScalarFieldInfo;
import com.almworks.items.gui.meta.util.SingleEnumInfo;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.gui.edit.workflow.IssueWorkflowAction;
import com.almworks.jira.provider3.gui.table.SubtaskTreeStructure;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklogLoader;
import com.almworks.jira.provider3.gui.viewer.CommentImplLoader;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.LoadedIssue;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.ParentLoader;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.SubtasksLoader;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.jira.provider3.schema.Component;
import com.almworks.jira.provider3.schema.*;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.almworks.util.Collections15.arrayList;

public class MetaSchema {
  public static final DBIdentity FEATURE_ISSUE_BY_KEY_COMPARATOR = Jira.feature("comparator.issuesByKey");
  public static final DBIdentity FEATURE_ISSUE_KEY_EXPORT = Jira.feature("export.issueKey");
  public static final DBIdentity FEATURE_CASCADE_CONSTRAINT = Jira.feature("constraint.cascade");
  public static final DBIdentity FEATURE_ITEM_KEY_ICONS_VALUE_CONVERTOR = Jira.feature("convertor.itemKeyIconsValue");

  public static final String CONFIG_AFFECT_VERSIONS = "affectVersions";
  public static final String CONFIG_COMPONENTS = "components";
  public static final String CONFIG_FIX_VERSIONS = "fixVersions";
  public static final String CONFIG_ASSIGNEE = "assignee";
  public static final String CONFIG_SECURITY_LEVEL = "securityLevel";
  public static final String CONFIG_REPORTER = "reporter";
  public static final String CONFIG_PRIORITY = "priority";
  public static final String CONFIG_WATCHERS = "editWatchers";

  public static final DBStaticObject KEY_KEY = ModelKeys.create(Jira.JIRA_PROVIDER_ID, "key", ModelKeys.scalarDataLoader(Issue.KEY), ModelKeys.SEQUENCE_PROMOTION_ALWAYS);

  public static final DBStaticObject KEY_SUMMARY;
  public static final DBStaticObject KEY_PROJECT;
  public static final DBStaticObject KEY_ISSUE_TYPE;
  public static final DBStaticObject KEY_STATUS;
  public static final DBStaticObject KEY_PRIORITY;
  public static final DBStaticObject KEY_DESCRIPTION;
  public static final DBStaticObject KEY_ENVIRONMENT;
  public static final DBStaticObject KEY_VOTED;
  public static final DBStaticObject KEY_WATCHING;
  public static final DBStaticObject KEY_REMAIN_ESTIMATE;
  public static final DBStaticObject KEY_TIME_SPENT;
  public static final DBStaticObject KEY_WORKFLOW_ACTIONS = IssueWorkflowAction.createModelKey();

  public static final DBStaticObject EXPORT_KEY = Exports.create(Jira.JIRA_PROVIDER_ID, "KEY", "Key", ScalarSequence.create(FEATURE_ISSUE_KEY_EXPORT));
  public static final DBStaticObject EXPORT_SUMMARY;
  public static final DBStaticObject EXPORT_DESCRIPTION;
  public static final DBStaticObject EXPORT_ENVIRONMENT;
  public static final DBStaticObject EXPORT_DUE;
  public static final DBStaticObject EXPORT_CREATED;
  public static final DBStaticObject EXPORT_UPDATED;
  public static final DBStaticObject EXPORT_RESOLVED;
  public static final DBStaticObject EXPORT_PROJECT;
  public static final DBStaticObject EXPORT_ISSUE_TYPE;
  public static final DBStaticObject EXPORT_STATUS;
  public static final DBStaticObject EXPORT_PRIORITY;
  public static final DBStaticObject EXPORT_RESOLUTION;
  public static final DBStaticObject EXPORT_REPORTER;
  public static final DBStaticObject EXPORT_ASSIGNEE;
  public static final DBStaticObject EXPORT_COMPONENTS;
  public static final DBStaticObject EXPORT_AFFECT_VERSIONS;
  public static final DBStaticObject EXPORT_FIX_VERSIONS;
  public static final DBStaticObject EXPORT_SECURITY_LEVEL;
  public static final DBStaticObject EXPORT_REMAIN_ESTIMATE;
  public static final DBStaticObject EXPORT_TIME_SPENT;
  public static final DBStaticObject EXPORT_ORIGINAL_ESTIMATE;
  public static final DBStaticObject EXPORT_I_VOTED;
  public static final DBStaticObject EXPORT_VOTED_COUNT;
  public static final DBStaticObject EXPORT_VOTERS;
  public static final DBStaticObject EXPORT_I_WATCHING;
  public static final DBStaticObject EXPORT_WATCHING_COUNT;
  public static final DBStaticObject EXPORT_WATCHERS;

  public static final List<Pair<DBStaticObject, DBStaticObject>> KEYS_STP;

  public static DBStaticObject modelKeyAlways(String id, ScalarSequence dataLoader) {
    return ModelKeys.create(Jira.JIRA_PROVIDER_ID, id, dataLoader, ModelKeys.SEQUENCE_PROMOTION_ALWAYS);
  }

  public static final DBStaticObject KEY_COMMENTS_LIST = modelKeyAlways("commentsList", CommentImplLoader.SERIALIZABLE);
  public static final DBStaticObject KEY_ATTACHMENTS_LIST = modelKeyAlways("attachmentsList", JiraAttachments.MK_DATA_LOADER);
  public static final DBStaticObject KEY_LINKS_LIST = modelKeyAlways("linksList", JiraLinks.MK_DATA_LOADER);
  public static final DBStaticObject KEY_PARENT = modelKeyAlways("parent", ParentLoader.SERIALIZABLE);
  public static final DBStaticObject KEY_SUBTASKS = modelKeyAlways("subtasks", SubtasksLoader.SERIALIZABLE);
  public static final DBStaticObject KEY_WORKLOG_LIST = modelKeyAlways("worklogList", LoadedWorklogLoader.SERIALIZABLE);

  public static final DBStaticObject COLUMN_KEY =
    Columns.create(Jira.JIRA_PROVIDER_ID, "KEY", "Key", "Key", SizePolicies.freeLetterMWidth(15), Columns.convertModelKeyValue(KEY_KEY),
      ColumnRenderer.valueCanvasDefault(ItemDownloadStage.DUMMY, Font.ITALIC, "<new>"), ColumnComparator.l2Comparator(FEATURE_ISSUE_BY_KEY_COMPARATOR, null, null), null, null);

  public static final DBStaticObject COLUMN_SUMMARY;
  public static final DBStaticObject COLUMN_PROJECT;
  public static final DBStaticObject COLUMN_ISSUE_TYPE;
  public static final DBStaticObject COLUMN_PRIORITY;
  public static final DBStaticObject COLUMN_STATUS;
  public static final DBStaticObject COLUMN_RESOLUTION;
  public static final DBStaticObject COLUMN_REPORTER;
  public static final DBStaticObject COLUMN_ASSIGNEE;
  public static final DBStaticObject COLUMN_COMPONENTS;
  public static final DBStaticObject COLUMN_AFFECT_VERSIONS;
  public static final DBStaticObject COLUMN_FIX_VERSIONS;

  public static final DBStaticObject COLUMN_COMMENT_COUNT = listCountColumn("Comments #", KEY_COMMENTS_LIST, "commentCount", ItemDownloadStage.QUICK);
  public static final DBStaticObject COLUMN_ATTACHMENT_COUNT = listCountColumn("Attachments #", KEY_ATTACHMENTS_LIST,
    "attachmentCount", ItemDownloadStage.QUICK);
  public static final DBStaticObject COLUMN_LINK_COUNT = listCountColumn("Links #", KEY_LINKS_LIST, "linkCount", ItemDownloadStage.QUICK);
  public static final DBStaticObject COLUMN_WORKLOG_COUNT = listCountColumn("Work Log Entries #", KEY_WORKLOG_LIST,
    "worklogCount", ItemDownloadStage.QUICK);

  public static final DBStaticObject COLUMN_DUE;
  public static final DBStaticObject COLUMN_CREATED;
  public static final DBStaticObject COLUMN_UPDATED;
  public static final DBStaticObject COLUMN_RESOLVED;
  public static final DBStaticObject COLUMN_SECURITY_LEVEL;
  public static final DBStaticObject COLUMN_VOTES_COUNT;
  public static final DBStaticObject COLUMN_WATCHERS_COUNT;
  public static final DBStaticObject COLUMN_VOTERS;
  public static final DBStaticObject COLUMN_WATCHERS;
  public static final DBStaticObject COLUMN_I_VOTED;
  public static final DBStaticObject COLUMN_I_WATCH;
  public static final DBStaticObject COLUMN_ORIGINAL_ESTIMATE;
  public static final DBStaticObject COLUMN_REMAIN_ESTIMATE;
  public static final DBStaticObject COLUMN_TIME_SPENT;
  public static final DBStaticObject COLUMN_STP;

  public static final DBStaticObject CONSTRAINT_KEY =
    Descriptors.create(Jira.JIRA_PROVIDER_ID, Issue.KEY, "Key", ServerFields.LegacyIds.ID_KEY, null, Descriptors.SEQUENCE_SCALAR_CONSTRAINT, null);
  public static final DBStaticObject CONSTRAINT_SUMMARY;
  public static final DBStaticObject CONSTRAINT_DESCRIPTION;
  public static final DBStaticObject CONSTRAINT_ENVIRONMENT;
  public static final DBStaticObject CONSTRAINT_CREATED;
  public static final DBStaticObject CONSTRAINT_UPDATED;
  public static final DBStaticObject CONSTRAINT_RESOLVED;
  public static final DBStaticObject CONSTRAINT_DUE;
  public static final DBStaticObject CONSTRAINT_PROJECT;
  public static final DBStaticObject CONSTRAINT_ISSUE_TYPE;
  public static final DBStaticObject CONSTRAINT_PRIORITY;
  public static final DBStaticObject CONSTRAINT_STATUS;
  public static final DBStaticObject CONSTRAINT_RESOLUTION;
  public static final DBStaticObject CONSTRAINT_REPORTER;
  public static final DBStaticObject CONSTRAINT_ASSIGNEE;
  public static final DBStaticObject CONSTRAINT_COMPONENTS;
  public static final DBStaticObject CONSTRAINT_AFFECT_VERSIONS;
  public static final DBStaticObject CONSTRAINT_FIX_VERSIONS;
  public static final DBStaticObject CONSTRAINT_SECURITY_LEVEL;
  public static final DBStaticObject CONSTRAINT_VOTES_COUNT;
  public static final DBStaticObject CONSTRAINT_COMMENTS = Descriptors.slaveText(Jira.JIRA_PROVIDER_ID, "Comments", "comments", Comment.ISSUE, Comment.TEXT);

  public static final DBStaticObject FIELD_PROJECT;
  public static final DBStaticObject FIELD_ISSUE_TYPE;
  public static final DBStaticObject FIELD_STATUS;
  public static final DBStaticObject FIELD_PRIORITY;
  public static final DBStaticObject FIELD_RESOLUTION;
  public static final DBStaticObject FIELD_REPORTER;
  public static final DBStaticObject FIELD_ASSIGNEE;
  public static final DBStaticObject FIELD_COMPONENTS;
  public static final DBStaticObject FIELD_AFFECT_VERSIONS;
  public static final DBStaticObject FIELD_FIX_VERSIONS;
  public static final DBStaticObject FIELD_DUE;
  public static final DBStaticObject FIELD_CREATED;
  public static final DBStaticObject FIELD_UPDATED;
  public static final DBStaticObject FIELD_RESOLVED;
  public static final DBStaticObject FIELD_SECURITY_LEVEL;
  public static final DBStaticObject FIELD_VOTES_COUNT;
  public static final DBStaticObject FIELD_WATCHERS_COUNT;
  public static final DBStaticObject FIELD_VOTERS;
  public static final DBStaticObject FIELD_WATCHERS;
  public static final DBStaticObject FIELD_ORIGINAL_ESTIMATE;
  public static final DBStaticObject FIELD_REMAIN_ESTIMATE;
  public static final DBStaticObject FIELD_TIME_SPENT;

  public static final DBStaticObject DND_PROJECT;
  public static final DBStaticObject DND_ISSUE_TYPE;
  public static final DBStaticObject DND_STATUS;
  public static final DBStaticObject DND_PRIORITY;
  public static final DBStaticObject DND_RESOLUTION;
  public static final DBStaticObject DND_REPORTER;
  public static final DBStaticObject DND_ASSIGNEE;
  public static final DBStaticObject DND_COMPONENTS;
  public static final DBStaticObject DND_AFFECT_VERSIONS;
  public static final DBStaticObject DND_FIX_VERSIONS;
  public static final DBStaticObject DND_SECURITY_LEVEL;

  public static void materializeObjects(DBDrain drain) {
    DBStaticObject.transactionForceFullMaterialize(drain);
    // Columns aren't referred by other items so they has to be explicitly materialized
    drain.materialize(COLUMN_KEY);
    drain.materialize(COLUMN_SUMMARY);
    drain.materialize(COLUMN_PROJECT);
    drain.materialize(COLUMN_ISSUE_TYPE);
    drain.materialize(COLUMN_PRIORITY);
    drain.materialize(COLUMN_STATUS);
    drain.materialize(COLUMN_RESOLUTION);
    drain.materialize(COLUMN_REPORTER);
    drain.materialize(COLUMN_ASSIGNEE);
    drain.materialize(COLUMN_COMPONENTS);
    drain.materialize(COLUMN_AFFECT_VERSIONS);
    drain.materialize(COLUMN_FIX_VERSIONS);
    drain.materialize(COLUMN_COMMENT_COUNT);
    drain.materialize(COLUMN_ATTACHMENT_COUNT);
    drain.materialize(COLUMN_LINK_COUNT);
    drain.materialize(COLUMN_WORKLOG_COUNT);
    drain.materialize(COLUMN_DUE);
    drain.materialize(COLUMN_CREATED);
    drain.materialize(COLUMN_UPDATED);
    drain.materialize(COLUMN_RESOLVED);
    drain.materialize(COLUMN_SECURITY_LEVEL);
    drain.materialize(COLUMN_VOTES_COUNT);
    drain.materialize(COLUMN_WATCHERS_COUNT);
    drain.materialize(COLUMN_VOTERS);
    drain.materialize(COLUMN_WATCHERS);
    drain.materialize(COLUMN_I_VOTED);
    drain.materialize(COLUMN_I_WATCH);
    drain.materialize(COLUMN_ORIGINAL_ESTIMATE);
    drain.materialize(COLUMN_REMAIN_ESTIMATE);
    drain.materialize(COLUMN_TIME_SPENT);
    drain.materialize(COLUMN_STP);

    // Right side viewers are hardcoded so we need to materialize model keys which aren't referred by columns.
    drain.materialize(KEY_ENVIRONMENT);
    drain.materialize(KEY_DESCRIPTION);
    drain.materialize(KEY_COMMENTS_LIST);
    drain.materialize(KEY_ATTACHMENTS_LIST);
    drain.materialize(KEY_LINKS_LIST);
    drain.materialize(KEY_WORKLOG_LIST);
    drain.materialize(KEY_PARENT);
    drain.materialize(KEY_SUBTASKS);

    drain.materialize(KEY_VOTED);
    drain.materialize(KEY_WATCHING);
    drain.materialize(KEY_WORKFLOW_ACTIONS);

    // Constrain descriptors aren't referred - need to be explicitly materialized
    drain.materialize(CONSTRAINT_KEY);
    drain.materialize(CONSTRAINT_SUMMARY);
    drain.materialize(CONSTRAINT_DESCRIPTION);
    drain.materialize(CONSTRAINT_ENVIRONMENT);
    drain.materialize(CONSTRAINT_CREATED);
    drain.materialize(CONSTRAINT_UPDATED);
    drain.materialize(CONSTRAINT_RESOLVED);
    drain.materialize(CONSTRAINT_DUE);
    drain.materialize(CONSTRAINT_PROJECT);
    drain.materialize(CONSTRAINT_ISSUE_TYPE);
    drain.materialize(CONSTRAINT_PRIORITY);
    drain.materialize(CONSTRAINT_STATUS);
    drain.materialize(CONSTRAINT_RESOLUTION);
    drain.materialize(CONSTRAINT_REPORTER);
    drain.materialize(CONSTRAINT_ASSIGNEE);
    drain.materialize(CONSTRAINT_COMPONENTS);
    drain.materialize(CONSTRAINT_AFFECT_VERSIONS);
    drain.materialize(CONSTRAINT_FIX_VERSIONS);
    drain.materialize(CONSTRAINT_SECURITY_LEVEL);
    drain.materialize(CONSTRAINT_VOTES_COUNT);
    drain.materialize(CONSTRAINT_COMMENTS);

    // Materialize enum types not referred by constraint descriptors
    drain.materialize(LinkType.ENUM_TYPE);
    drain.materialize(Group.ENUM_TYPE);
    drain.materialize(ProjectRole.ENUM_TYPE);

    // Materialize DnD changes - not referred by any other object, need explicit materialize
    drain.materialize(DND_PROJECT);
    drain.materialize(DND_ISSUE_TYPE);
    drain.materialize(DND_STATUS);
    drain.materialize(DND_PRIORITY);
    drain.materialize(DND_RESOLUTION);
    drain.materialize(DND_REPORTER);
    drain.materialize(DND_ASSIGNEE);
    drain.materialize(DND_COMPONENTS);
    drain.materialize(DND_AFFECT_VERSIONS);
    drain.materialize(DND_FIX_VERSIONS);
    drain.materialize(DND_SECURITY_LEVEL);

    // Materialize Exports - not referred by any other object
    drain.materialize(EXPORT_KEY);
    drain.materialize(EXPORT_SUMMARY);
    drain.materialize(EXPORT_PROJECT);
    drain.materialize(EXPORT_ISSUE_TYPE);
    drain.materialize(EXPORT_STATUS);
    drain.materialize(EXPORT_PRIORITY);
    drain.materialize(EXPORT_RESOLUTION);
    drain.materialize(EXPORT_REPORTER);
    drain.materialize(EXPORT_ASSIGNEE);
    drain.materialize(EXPORT_COMPONENTS);
    drain.materialize(EXPORT_AFFECT_VERSIONS);
    drain.materialize(EXPORT_FIX_VERSIONS);
    drain.materialize(EXPORT_DESCRIPTION);
    drain.materialize(EXPORT_ENVIRONMENT);
    drain.materialize(EXPORT_DUE);
    drain.materialize(EXPORT_CREATED);
    drain.materialize(EXPORT_UPDATED);
    drain.materialize(EXPORT_RESOLVED);
    drain.materialize(EXPORT_SECURITY_LEVEL);
    drain.materialize(EXPORT_ORIGINAL_ESTIMATE);
    drain.materialize(EXPORT_REMAIN_ESTIMATE);
    drain.materialize(EXPORT_TIME_SPENT);
    drain.materialize(EXPORT_ORIGINAL_ESTIMATE);
    drain.materialize(EXPORT_I_VOTED);
    drain.materialize(EXPORT_VOTED_COUNT);
    drain.materialize(EXPORT_VOTERS);
    drain.materialize(EXPORT_I_WATCHING);
    drain.materialize(EXPORT_WATCHING_COUNT);
    drain.materialize(EXPORT_WATCHERS);

    SubtaskTreeStructure.materializeObjects(drain);
  }

  public static DBStaticObject intFlagsKey(DBAttribute<Integer> attribute, String id) {
    return modelKeyAlways(id, new ScalarSequence.Builder().append(ModelKeys.FEATURE_LOADER_INTEGER).append(attribute).create());
  }

  public static void registerFeatures(FeatureRegistry features) {
    features.register(FEATURE_ISSUE_BY_KEY_COMPARATOR, SerializableFeature.NoParameters.create(Containers.convertingComparator(
      GetModelKeyValue.scalar(KEY_KEY, String.class), IssueKeyComparator.INSTANCE), Comparator.class));
    features.register(FEATURE_ISSUE_KEY_EXPORT, SerializableFeature.NoParameters.create(IssueKeyExportPolicy.INSTANCE, ExportPolicy.class));
    features.register(FEATURE_ITEM_KEY_ICONS_VALUE_CONVERTOR, SerializableFeature.NoParameters.create(ItemKeyIconsValue.convertor(KEYS_STP), Convertor.class));
    IssueWorkflowAction.registerFeatures(features);
    JiraFields.registerFeatures(features);
    Log.debug("JIRA features registered");
  }

  static {
    MultiEnumInfo versions = JiraFields.staticMultiEnum(ItemDownloadStage.QUICK, Version.ENUM_TYPE, 20, true, true);
    versions
      .setAttribute(Issue.AFFECT_VERSIONS)
      .setId("LIST_VERSIONS")
      .setDisplayName("Affects Versions")
      .setNullableEnum(Version.NULL_CONSTRAINT_ID, Version.NULL_CONSTRAINT_NAME)
      .setDefaultDnD(CONFIG_AFFECT_VERSIONS, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_AFFECT_VERSIONS = versions.createColumn();
    CONSTRAINT_AFFECT_VERSIONS = versions.createDescriptor();
    FIELD_AFFECT_VERSIONS = versions.createViewerField();
    EXPORT_AFFECT_VERSIONS = versions.createExport();
    DND_AFFECT_VERSIONS = versions.createDnDChange();

    versions
      .setAttribute(Issue.FIX_VERSIONS)
      .setId("LIST_FIXED_VERSION")
      .setDisplayName("Fix Versions")
      .setDefaultDnD(CONFIG_FIX_VERSIONS, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_FIX_VERSIONS = versions.createColumn();
    CONSTRAINT_FIX_VERSIONS = versions.createDescriptor();
    FIELD_FIX_VERSIONS = versions.createViewerField();
    EXPORT_FIX_VERSIONS = versions.createExport();
    DND_FIX_VERSIONS = versions.createDnDChange();
  }

  static {
    MultiEnumInfo components = JiraFields.staticMultiEnum(ItemDownloadStage.QUICK, Component.ENUM_TYPE, 20, true, false)
      .setAttribute(Issue.COMPONENTS)
      .setId(ServerFields.LegacyIds.ID_COMPONENTS)
      .setDisplayName("Component")
      .setNullableEnum(Component.NULL_CONSTRAINT_ID, Component.NULL_CONSTRAINT_NAME)
      .setDefaultDnD(CONFIG_COMPONENTS, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_COMPONENTS = components.createColumn();
    CONSTRAINT_COMPONENTS = components.createDescriptor();
    FIELD_COMPONENTS = components.createViewerField();
    EXPORT_COMPONENTS = components.createExport();
    DND_COMPONENTS = components.createDnDChange();
  }

  static {
    SingleEnumInfo assignee = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, User.ENUM_TYPE, 15, false)
      .setAttribute(Issue.ASSIGNEE)
      .setId(ServerFields.LegacyIds.ID_ASSIGNEE)
      .setDisplayName("Assignee")
      .setNullableEnum(User.NULL_CONSTRAINT_ID, "Unassigned")
      .setDefaultDnD(CONFIG_ASSIGNEE, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_ASSIGNEE = assignee.createColumn();
    CONSTRAINT_ASSIGNEE = assignee.createDescriptor();
    FIELD_ASSIGNEE = assignee.createViewerField();
    EXPORT_ASSIGNEE = assignee.createExport();
    DND_ASSIGNEE = assignee.createDnDChange();

    SingleEnumInfo reporter = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, User.ENUM_TYPE, 15, false);
    reporter
      .setAttribute(Issue.REPORTER)
      .setId(ServerFields.LegacyIds.ID_REPORTER)
      .setDisplayName("Reporter")
      .setNullableEnum(User.NULL_CONSTRAINT_ID, "No Reporter")
      .setDefaultDnD(CONFIG_REPORTER, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_REPORTER = reporter.createColumn();
    CONSTRAINT_REPORTER = reporter.createDescriptor();
    FIELD_REPORTER = reporter.createViewerField();
    EXPORT_REPORTER = reporter.createExport();
    DND_REPORTER = reporter.createDnDChange();
  }

  static {
    SingleEnumInfo project = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, Project.ENUM_TYPE, 10, false)
      .setAttribute(Issue.PROJECT)
      .setDisplayName("Project")
      .setId(ServerFields.LegacyIds.ID_PROJECT);
    KEY_PROJECT = project.createModelKey();
    COLUMN_PROJECT = project.createColumn();
    CONSTRAINT_PROJECT = project.createDescriptor();
    FIELD_PROJECT = project.createViewerField();
    EXPORT_PROJECT = project.createExport();
    DND_PROJECT = DnDChange.createStaticNoChange(Jira.JIRA_PROVIDER_ID, Issue.PROJECT, KEY_PROJECT, "Move issue is not supported");
  }

  static {
    SingleEnumInfo type = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, IssueType.ENUM_TYPE, 15, false)
      .setAttribute(Issue.ISSUE_TYPE)
      .setDisplayName("Type")
      .setId(ServerFields.LegacyIds.ID_ISSUE_TYPE);
    KEY_ISSUE_TYPE = type.createModelKey();
    COLUMN_ISSUE_TYPE = type.createColumn();
    CONSTRAINT_ISSUE_TYPE = type.createDescriptor();
    FIELD_ISSUE_TYPE = type.createViewerField();
    EXPORT_ISSUE_TYPE = type.createExport();
    DND_ISSUE_TYPE = DnDChange.createStaticNoChange(Jira.JIRA_PROVIDER_ID, Issue.ISSUE_TYPE, KEY_ISSUE_TYPE, "Move issue is not supported");
  }

  static {
    SingleEnumInfo security = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, Security.ENUM_TYPE, 15, true)
      .setAttribute(Issue.SECURITY)
      .setDisplayName("Security Level")
      .setId(ServerFields.LegacyIds.ID_SECURITY)
      .setNullableEnum("_security_no_set_", "None")
      .setDefaultDnD(CONFIG_SECURITY_LEVEL, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_SECURITY_LEVEL = security.createColumn();
    CONSTRAINT_SECURITY_LEVEL = security.createDescriptor();
    FIELD_SECURITY_LEVEL = security.createViewerField();
    EXPORT_SECURITY_LEVEL = security.createExport();
    DND_SECURITY_LEVEL = security.createDnDChange();
  }

  static {
    SingleEnumInfo status = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, Status.ENUM_TYPE, 15, false)
      .setAttribute(Issue.STATUS)
      .setDisplayName("Status")
      .setId(ServerFields.LegacyIds.ID_STATUS);
    KEY_STATUS = status.createModelKey();
    COLUMN_STATUS = status.createColumn();
    CONSTRAINT_STATUS = status.createDescriptor();
    FIELD_STATUS = status.createViewerField();
    EXPORT_STATUS = status.createExport();
    DND_STATUS = DnDChange.createStaticNoChange(Jira.JIRA_PROVIDER_ID, Issue.STATUS, KEY_STATUS, "Cannot change status");
  }

  static {
    SingleEnumInfo priority = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, Priority.ENUM_TYPE, 15, false)
      .setAttribute(Issue.PRIORITY)
      .setDisplayName("Priority")
      .setId(ServerFields.LegacyIds.ID_PRIORITY)
      .setDefaultDnD(CONFIG_PRIORITY, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_PRIORITY = priority.createColumn();
    CONSTRAINT_PRIORITY = priority.createDescriptor();
    FIELD_PRIORITY = priority.createViewerField();
    KEY_PRIORITY = priority.createModelKey();
    EXPORT_PRIORITY = priority.createExport();
    DND_PRIORITY = priority.createDnDChange();

    KEYS_STP = arrayList(
      Pair.create(KEY_STATUS, COLUMN_STATUS),
      Pair.create(KEY_ISSUE_TYPE, COLUMN_ISSUE_TYPE),
      Pair.create(KEY_PRIORITY, COLUMN_PRIORITY));
  }

  static {
    SingleEnumInfo resolution = JiraFields.staticSingleEnum(ItemDownloadStage.QUICK, Resolution.ENUM_TYPE, 15, true)
      .setNullColumnPresentation(Font.ITALIC, "Unresolved")
      .setAttribute(Issue.RESOLUTION)
      .setDisplayName("Resolution")
      .setId(ServerFields.LegacyIds.ID_RESOLUTION);
    COLUMN_RESOLUTION = resolution.createColumn();
    CONSTRAINT_RESOLUTION = resolution.createDescriptor();
    FIELD_RESOLUTION = resolution.createViewerField();
    EXPORT_RESOLUTION = resolution.createExport();
    DND_RESOLUTION = DnDChange.createStaticNoChange(Jira.JIRA_PROVIDER_ID, Issue.RESOLUTION, resolution.createModelKey(), "Cannot change resolution");
  }

  static {
    ScalarFieldInfo description = JiraFields.longText()
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setDisplayName("Description")
      .setId(ServerFields.LegacyIds.ID_DESCRIPTION)
      .setAttribute(Issue.DESCRIPTION);
    CONSTRAINT_DESCRIPTION = description.createDescriptor();
    KEY_DESCRIPTION = description.createModelKey();
    EXPORT_DESCRIPTION = description.createExport();
  }

  static {
    ScalarFieldInfo<String> environment = JiraFields.longText()
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setDisplayName("Environment")
      .setId(ServerFields.LegacyIds.ID_ENVIRONMENT)
      .setAttribute(Issue.ENVIRONMENT);
    CONSTRAINT_ENVIRONMENT = environment.createDescriptor();
    KEY_ENVIRONMENT = environment.createModelKey();
    EXPORT_ENVIRONMENT = environment.createExport();
  }

  static {
    ScalarFieldInfo<String> summary = JiraFields.shortText(ItemDownloadStage.QUICK, false)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setCharCount(60)
      .setDisplayName("Summary")
      .setId(ServerFields.LegacyIds.ID_SUMMARY)
      .setAttribute(Issue.SUMMARY);
    CONSTRAINT_SUMMARY = summary.createDescriptor();
    KEY_SUMMARY = summary.createModelKey();
    COLUMN_SUMMARY = summary.createColumn();
    EXPORT_SUMMARY = summary.createExport();
  }

  private static <T> ScalarFieldInfo<T> setProviderOwner(ScalarFieldInfo<T> date) {
    return date.setOwner(Jira.JIRA_PROVIDER_ID);
  }

  static {
    final ScalarFieldInfo<Integer> due = setProviderOwner(JiraFields.day(ItemDownloadStage.QUICK, 5))
      .setHideEmptyLeftField(true)
      .setDisplayName("Due Date")
      .setId(ServerFields.LegacyIds.ID_DUE)
      .setAttribute(Issue.DUE);
    COLUMN_DUE = due.createColumn();
    CONSTRAINT_DUE = due.createDescriptor();
    FIELD_DUE = due.createViewerField();
    EXPORT_DUE = due.createExport();
  }

  static {
    final ScalarFieldInfo<Date> created = setProviderOwner(JiraFields.dateTime(ItemDownloadStage.QUICK, false))
      .setDisplayName("Created")
      .setId(ServerFields.LegacyIds.ID_CREATED)
      .setConstraint(Descriptors.dateConstraint(true))
      .setDataPromotion(ModelKeys.SEQUENCE_PROMOTION_ALWAYS)
      .setAttribute(Issue.CREATED);
    COLUMN_CREATED = created.createColumn();
    CONSTRAINT_CREATED = created.createDescriptor();
    FIELD_CREATED = created.createViewerField();
    EXPORT_CREATED = created.createExport();
  }

  static {
    final ScalarFieldInfo<Date> updated = setProviderOwner(JiraFields.dateTime(ItemDownloadStage.QUICK, false))
      .setDisplayName("Updated")
      .setId(ServerFields.LegacyIds.ID_UPDATED)
      .setConstraint(Descriptors.dateConstraint(true))
      .setAttribute(Issue.UPDATED);
    COLUMN_UPDATED = updated.createColumn();
    CONSTRAINT_UPDATED = updated.createDescriptor();
    FIELD_UPDATED = updated.createViewerField();
    EXPORT_UPDATED = updated.createExport();
  }

  static {
    final ScalarFieldInfo<Date> resolved = setProviderOwner(JiraFields.dateTime(ItemDownloadStage.QUICK, true))
      .setHideEmptyLeftField(true)
      .setDisplayName("Resolved")
      .setId(ServerFields.LegacyIds.ID_RESOLVED)
      .setConstraint(Descriptors.dateConstraint(false))
      .setAttribute(Issue.RESOLVED);
    COLUMN_RESOLVED = resolved.createColumn();
    CONSTRAINT_RESOLVED = resolved.createDescriptor();
    FIELD_RESOLVED = resolved.createViewerField();
    EXPORT_RESOLVED = resolved.createExport();
  }

  static {
    ScalarFieldInfo<Integer> votesCount = JiraFields.integerReversed(ItemDownloadStage.QUICK)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setHideEmptyLeftField(true)
      .setDisplayName("Votes")
      .setAttribute(Issue.VOTES_COUNT)
      .setId(ServerFields.LegacyIds.ID_VOTES_COUNT)
      .setConstraintId("voting.count");
    COLUMN_VOTES_COUNT = votesCount.createColumn();
    FIELD_VOTES_COUNT = votesCount.createViewerField();
    CONSTRAINT_VOTES_COUNT = votesCount.createDescriptor();
    EXPORT_VOTED_COUNT = votesCount.createExport();

    ScalarFieldInfo<Integer> watchersCount = JiraFields.integerReversed(ItemDownloadStage.QUICK)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setHideEmptyLeftField(true)
      .setDisplayName("Watching")
      .setAttribute(Issue.WATCHERS_COUNT)
      .setId(ServerFields.LegacyIds.ID_WATCHERS_COUNT)
      .setConstraintId("watching.count")
      .setDataPromotion(ModelKeys.SEQUENCE_PROMOTION_FULL_DOWNLOAD)
      .setEmptyNullRenderer(ItemDownloadStage.QUICK);
    COLUMN_WATCHERS_COUNT = watchersCount.createColumn();
    FIELD_WATCHERS_COUNT = watchersCount.createViewerField();
    EXPORT_WATCHING_COUNT = watchersCount.createExport();

    MultiEnumInfo voters = JiraFields.staticMultiEnum(ItemDownloadStage.FULL, User.ENUM_TYPE, 30, true, false)
      .setDisplayName("Voters")
      .setId(ServerFields.LegacyIds.ID_VOTERS)
      .setAttribute(Issue.VOTERS)
      .setDataPromotion(ModelKeys.SEQUENCE_PROMOTION_ALWAYS);
    COLUMN_VOTERS = voters.createColumn();
    FIELD_VOTERS = voters.createViewerField();
    EXPORT_VOTERS = voters.createExport();

    MultiEnumInfo watchers = JiraFields.staticMultiEnum(ItemDownloadStage.FULL, User.ENUM_TYPE, 30, true, false)
      .setDisplayName("Watchers")
      .setId(ServerFields.LegacyIds.ID_WATCHERS)
      .setAttribute(Issue.WATCHERS)
      .setDataPromotion(ModelKeys.SEQUENCE_PROMOTION_ALWAYS)
      .setDefaultDnD(CONFIG_WATCHERS, JiraFields.SEQUENCE_EDIT_APPLICABILITY);
    COLUMN_WATCHERS = watchers.createColumn();
    FIELD_WATCHERS = watchers.createViewerField();
    EXPORT_WATCHERS = watchers.createExport();

    ScalarFieldInfo<Boolean> voted = JiraFields.bool(ItemDownloadStage.QUICK)
      .setDisplayName("I Voted")
      .setId(Issue.VOTED.getName())
      .setAttribute(Issue.VOTED)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setDataPromotion(ModelKeys.SEQUENCE_PROMOTION_FULL_DOWNLOAD)
      .setCustomKeyLoader(new ScalarSequence.Builder().append(ModelKeys.FEATURE_LOADER_BOOLEAN_WITH_STATE_ICON)
        .append(Issue.VOTED)
        .append(Icons.VOTED.getName())
        .append(0)
        .append("You have voted for this " + Terms.ref_artifact)
        .create());
    COLUMN_I_VOTED = voted.createColumn();
    KEY_VOTED = voted.createModelKey();
    EXPORT_I_VOTED = voted.createExport();

    ScalarFieldInfo<Boolean> watch = JiraFields.bool(ItemDownloadStage.QUICK)
      .setDisplayName("I Watch")
      .setId(Issue.WATCHING.getName())
      .setAttribute(Issue.WATCHING)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setDataPromotion(ModelKeys.SEQUENCE_PROMOTION_FULL_DOWNLOAD)
      .setCustomKeyLoader(new ScalarSequence.Builder().append(ModelKeys.FEATURE_LOADER_BOOLEAN_WITH_STATE_ICON)
        .append(Issue.WATCHING)
        .append(Icons.WATCHED.getName())
        .append(0)
        .append("You are watching this " + Terms.ref_artifact)
        .create());
    COLUMN_I_WATCH = watch.createColumn();
    KEY_WATCHING = watch.createModelKey();
    EXPORT_I_WATCHING = watch.createExport();
  }

  static {
    ScalarFieldInfo<Integer> originalEstimate = JiraFields.secondsDuration(ItemDownloadStage.QUICK)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setHideEmptyLeftField(true)
      .setDisplayName("Estimated")
      .setAttribute(Issue.ORIGINAL_ESTIMATE)
      .setId(ServerFields.LegacyIds.ID_ORIGINAL_ESTIMATE);
    COLUMN_ORIGINAL_ESTIMATE = originalEstimate.createColumn();
    FIELD_ORIGINAL_ESTIMATE = originalEstimate.createViewerField();
    EXPORT_ORIGINAL_ESTIMATE = originalEstimate.createExport();

    ScalarFieldInfo<Integer> remainEstimate = JiraFields.secondsDuration(ItemDownloadStage.QUICK)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setHideEmptyLeftField(true)
      .setDisplayName("Remaining")
      .setAttribute(Issue.REMAIN_ESTIMATE)
      .setId(ServerFields.LegacyIds.ID_REMAIN_ESTIMATE);
    COLUMN_REMAIN_ESTIMATE = remainEstimate.createColumn();
    FIELD_REMAIN_ESTIMATE = remainEstimate.createViewerField();
    KEY_REMAIN_ESTIMATE = remainEstimate.createModelKey();
    EXPORT_REMAIN_ESTIMATE = remainEstimate.createExport();

    ScalarFieldInfo<Integer> timeSpent = JiraFields.secondsDuration(ItemDownloadStage.QUICK)
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setHideEmptyLeftField(true)
      .setDisplayName("Logged")
      .setAttribute(Issue.TIME_SPENT)
      .setId(ServerFields.LegacyIds.ID_TIMESPENT);
    COLUMN_TIME_SPENT = timeSpent.createColumn();
    FIELD_TIME_SPENT = timeSpent.createViewerField();
    KEY_TIME_SPENT = timeSpent.createModelKey();
    EXPORT_TIME_SPENT = timeSpent.createExport();
  }

  static {
    COLUMN_STP = Columns.create(Jira.JIRA_PROVIDER_ID, "Icons_StatusTypePriority", "Status, Type, Priority (icons)", "STP",
      SizePolicies.fixedPixels(ItemKeyIconsValue.getTablePxWidth(KEYS_STP.size())),
      ScalarSequence.create(FEATURE_ITEM_KEY_ICONS_VALUE_CONVERTOR),
      ColumnRenderer.valueCanvasDefault(ItemDownloadStage.QUICK, 0, ""),
      null,
      Columns.convertingTooltipProvider(FEATURE_ITEM_KEY_ICONS_VALUE_CONVERTOR),
      "Status, Type, Priority");
  }

  public static ModelKey<String> issueKey(GuiFeaturesManager manager) {
    return manager.findScalarKey(KEY_KEY, String.class);
  }

  @Nullable
  public static String issueKey(ItemWrapper issue) {
    if (issue == null) return null;
    ModelKey<String> keyKey = issueKey(issue.services().getActor(GuiFeaturesManager.ROLE));
    if (keyKey == null) return null;
    return issue.getModelKeyValue(keyKey);
  }

  public static ModelKey<String> issueSummary(GuiFeaturesManager manager) {
    return manager.findScalarKey(KEY_SUMMARY, String.class);
  }

  public static ModelKey<ItemKey> issueType(ActionContext context) throws CantPerformException {
    GuiFeaturesManager manager = context.getSourceObject(GuiFeaturesManager.ROLE);
    LoadedModelKey<?> key = manager.findModelKey(KEY_ISSUE_TYPE);
    return CantPerformException.ensureNotNull(key.castScalar(ItemKey.class));
  }

  public static ModelKey<ItemKey> project(ActionContext context) throws CantPerformException {
    GuiFeaturesManager manager = context.getSourceObject(GuiFeaturesManager.ROLE);
    LoadedModelKey<?> key = manager.findModelKey(KEY_PROJECT);
    return CantPerformException.ensureNotNull(key.castScalar(ItemKey.class));
  }

  public static ModelKey<LoadedIssue> parentTask(ActionContext context) throws CantPerformException {
    GuiFeaturesManager manager = context.getSourceObject(GuiFeaturesManager.ROLE);
    return CantPerformException.ensureNotNull(parentTask(manager));
  }

  public static LoadedModelKey<LoadedIssue> parentTask(GuiFeaturesManager manager) {
    return manager.findScalarKey(KEY_PARENT, LoadedIssue.class);
  }

  public static ModelKey<List<LoadedIssue>> subtasks(GuiFeaturesManager manager) {
    return manager.findListModelKey(KEY_SUBTASKS, LoadedIssue.class);
  }

  public static ModelKey<List<LoadedIssue>> subtasks(ActionContext context) throws CantPerformException {
    GuiFeaturesManager manager = context.getSourceObject(GuiFeaturesManager.ROLE);
    return CantPerformException.ensureNotNull(subtasks(manager));
  }

  public static <T> T getScalarKeyValue(ItemWrapper item, DBStaticObject keyId, Class<T> aClass) {
    if (item == null) return null;
    GuiFeaturesManager manager = item.services().getActor(GuiFeaturesManager.ROLE);
    if (manager == null) return null;
    LoadedModelKey<T> key = manager.findScalarKey(keyId, aClass);
    if (key == null) return null;
    return item.getModelKeyValue(key);
  }

  @NotNull
  public static <T> T getNNScalarKeyValue(ItemWrapper item, DBStaticObject keyId, Class<T> aClass, @NotNull T missing) {
    return Util.NN(getScalarKeyValue(item, keyId, aClass), missing);
  }

  private static DBStaticObject listCountColumn(String headerText, DBStaticObject listModelKey, String id, ItemDownloadStage stage) {
    return Columns.create(Jira.JIRA_PROVIDER_ID, id, headerText, headerText, SizePolicies.freeLetterMWidth(10), Columns.convertListCount(listModelKey),
      ColumnRenderer.valueCanvasDefault(stage, 0, ""),
      ColumnComparator.l2Comparator(FEATURE_ISSUE_BY_KEY_COMPARATOR, stage, JiraFields.COMPARATOR_COUNT_LIST),
      null, null);

  }
}
