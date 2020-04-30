package com.almworks.jira.provider3.gui.edit;

import com.almworks.api.application.ItemKey;
import com.almworks.api.engine.Connection;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ItemReference;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.LoadAttribute;
import com.almworks.items.gui.edit.editors.enums.ConnectionVariants;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.multi.CompactEnumSubsetEditor;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEnumEditor;
import com.almworks.items.gui.edit.editors.enums.single.SingleEditableEnumEditor;
import com.almworks.items.gui.edit.editors.scalar.DateEditor;
import com.almworks.items.gui.edit.editors.text.ScalarFieldEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.comments.gui.BaseEditComment;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.edit.editors.AssigneeEditor;
import com.almworks.jira.provider3.gui.edit.editors.PriorityEditor;
import com.almworks.jira.provider3.gui.edit.editors.ResolutionEditor;
import com.almworks.jira.provider3.gui.edit.editors.move.MoveController;
import com.almworks.jira.provider3.gui.timetrack.RemainEstimateEditor;
import com.almworks.jira.provider3.links.actions.AddLinksEditor;
import com.almworks.jira.provider3.schema.*;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.commons.Condition;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.Comparator;
import java.util.Set;

public class EditMetaSchema {
  private static final DefaultItemSelector THIS_USER = new DefaultItemSelector() {
    private final TypedKey<Long> myThisUserKey = TypedKey.create("thisConnectionUser");

    @Override
    public ItemKey selectDefaultItem(EditModelState model, AListModel<? extends ItemKey> variants) {
      Long thisUser = model.getValue(myThisUserKey);
      if (thisUser == null || thisUser <= 0) return null;
      for (ItemKey variant : variants) {
        if (variant.getItem() == thisUser) return variant;
      }
      return null;
    }

    @Override
    public void readDB(VersionSource source, EditModelState model) {
      model.putHint(myThisUserKey, getThisUser(source.getReader(), model));
    }
  };

  public static Long getThisUser(DBReader reader, EditModelState model) {
    long connection = EngineConsts.getConnectionItem(model);
    if (connection <= 0) return null;
    return reader.getValue(connection, Connection.USER);
  }

  public static final FieldEditor PROVIDE_PROJECT = new LoadAttribute(Issue.PROJECT);

  public static final DropdownEnumEditor SECURITY =
    new DropdownEditorBuilder().setStaticVariants(Security.ENUM_TYPE, MetaSchema.CONFIG_SECURITY_LEVEL)
      .setAttribute(Issue.SECURITY)
      .setDefaultItem(DefaultItemSelector.ALLOW_EMPTY)
      .setNullPresentation("<None>")
      .setLabelText(NameMnemonic.parseString("Security &Level"))
      .setVerify(true)
      .createFixed();

  public static final DropdownEnumEditor STATUS =
    new DropdownEditorBuilder().setStaticVariants(Status.ENUM_TYPE, "status")
      .setAttribute(Issue.STATUS)
      .setLabelText(NameMnemonic.rawText("Status"))
      .setVerify(true)
      .createFixed();

  public static final SingleEditableEnumEditor REPORTER =
    new DropdownEditorBuilder().setStaticVariants(User.ENUM_TYPE, MetaSchema.CONFIG_REPORTER)
      .setAttribute(Issue.REPORTER, User.CREATOR)
      .setDefaultItem(THIS_USER)
      .setLabelText(NameMnemonic.parseString("&Reporter"))
      .overrideRenderer(ItemKey.NAME_ID_RENDERER)
      .createEditable();

  public static final FieldEditor ASSIGNEE = AssigneeEditor.EDITOR;

  public static final EditorsScheme DEFAULT = new EditorsScheme(null)
    .addEditor(ServerFields.SUMMARY, ScalarFieldEditor.shortText(NameMnemonic.parseString("&Summary"), Issue.SUMMARY, true))
    .addEditor(ServerFields.DESCRIPTION, ScalarFieldEditor.textPane(NameMnemonic.parseString("&Description"), Issue.DESCRIPTION, 100))
    .addEditor(ServerFields.ENVIRONMENT, ScalarFieldEditor.textPane(NameMnemonic.parseString("&Environment"), Issue.ENVIRONMENT))
    .addEditor(ServerFields.TIME_TRACKING, RemainEstimateEditor.INSTANCE)
    .addEditor(ServerFields.DUE, DateEditor.createDate(NameMnemonic.parseString("D&ue Date"), Issue.DUE))
    .addEditor(ServerFields.ASSIGNEE, ASSIGNEE)
    .addEditor(ServerFields.REPORTER, REPORTER)
    .addEditor(ServerFields.RESOLUTION, ResolutionEditor.MANDATORY_EDITOR)
    .addEditor(ServerFields.AFFECT_VERSIONS, versionEditor("Affected &Versions", Issue.AFFECT_VERSIONS, MetaSchema.CONFIG_AFFECT_VERSIONS, true))
    .addEditor(ServerFields.FIX_VERSIONS, versionEditor("&Fix Versions", Issue.FIX_VERSIONS, MetaSchema.CONFIG_FIX_VERSIONS, false))
    .addEditor(ServerFields.PROJECT, MoveController.PROJECT)
    .addEditor(ServerFields.ISSUE_TYPE, MoveController.COMMON_ISSUE_TYPE)
    .addEditor(ServerFields.PARENT, MoveController.PARENT)
    .addEditor(ServerFields.COMPONENTS, subsetEditor("&Components", Issue.COMPONENTS, Component.ENUM_TYPE, MetaSchema.CONFIG_COMPONENTS))
    .addEditor(ServerFields.COMMENTS, BaseEditComment.COMMENT_SLAVE)
    .addEditor(ServerFields.SECURITY, SECURITY)
    .addEditor(ServerFields.LINKS, AddLinksEditor.inlineAddLinks(NameMnemonic.rawText("Add Links"), false))
    .addEditor(ServerFields.PRIORITY, new PriorityEditor())
    .fix();

  private static CompactEnumSubsetEditor versionEditor(String nameMnemonic, DBAttribute<Set<Long>> attribute, String recentConfig, boolean releasedFirst) {
    Comparator<ItemKey> comparator = LoadedItemKey.compareLoadedItems(new Comparator<LoadedItemKey>() {
      @Override
      public int compare(LoadedItemKey o1, LoadedItemKey o2) {
        boolean r1 = Version.isReleasedOrArchived(o1);
        boolean r2 = Version.isReleasedOrArchived(o2);
        if (r1 == r2) return 0;
        return r1 == releasedFirst ? -1 : 1;
      }
    });
    ConnectionVariants allVersions = ConnectionVariants.createStatic(Version.ENUM_TYPE, recentConfig, comparator);
    Condition<ItemKey> archivedFilter = new Condition<ItemKey>() {
      @Override
      public boolean isAccepted(ItemKey value) {
        LoadedItemKey item = Util.castNullable(LoadedItemKey.class, value);
        return item != null && !Version.isArchived(item);
      }
    };
    return new CompactEnumSubsetEditor(NameMnemonic.parseString(nameMnemonic), attribute, allVersions, null, archivedFilter);
  }

  private static CompactEnumSubsetEditor subsetEditor(String nameMnemonic, DBAttribute<Set<Long>> attribute, ItemReference enumType, String recentConfig) {
    return compactSubsetEditor(NameMnemonic.parseString(nameMnemonic), attribute, enumType, recentConfig);
  }

  private static CompactEnumSubsetEditor compactSubsetEditor(NameMnemonic label, DBAttribute<Set<Long>> attribute, ItemReference enumType, String recentConfig) {
    return new CompactEnumSubsetEditor(label, attribute, ConnectionVariants.createStatic(enumType, recentConfig), null);
  }

}
