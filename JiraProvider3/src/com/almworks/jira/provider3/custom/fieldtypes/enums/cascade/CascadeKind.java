package com.almworks.jira.provider3.custom.fieldtypes.enums.cascade;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.gui.meta.util.SingleEnumInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.JqlSearchInfo;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumEditorInfo;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumFieldUtils;
import com.almworks.jira.provider3.custom.fieldtypes.enums.RelevantApplicability;
import com.almworks.jira.provider3.gui.CascadeSchema;
import com.almworks.jira.provider3.gui.JiraFields;
import com.almworks.jira.provider3.remotedata.issue.fields.CascadeFieldDescriptor;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.download2.meta.OptionsLoader;
import com.almworks.jira.provider3.sync.download2.rest.JRGeneric;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.jql.JqlEnum;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CascadeKind implements FieldKind, OptionsLoader<TLongObjectHashMap<Option>> {
  public static final FieldType CASCADE_TYPE = new FieldType("cascade") {
    @NotNull
    @Override
    public FieldKind createKind(Map<TypedKey<?>, ?> map) {
      return new CascadeKind();
    }
  };

  private static final ArrayKey<JSONObject> CHILDREN = ArrayKey.objectArray("children");
  private final String myPrefix = "singleCascade";

  public CascadeKind() {
  }

  @Override
  public Field createFieldsDescriptor(String fieldId, String connectionId, String fieldName) {
    Entity entityType = ServerCustomField.createCascadeType(fieldId, connectionId);
    JsonEntityParser parser = CascadeJsonParser.create(entityType);
    String fullId = ServerCustomField.createFullId(connectionId, myPrefix, fieldId);
    final EntityKey<Entity> key = EntityKey.entity(fullId, isEditable() ? EntityKeyProperties.shadowable() : null);
    return new Field(new CascadeFieldDescriptor(fieldId, fieldName, key, parser), ServerJira.toItemType(entityType));
  }

  @Override
  public void migrateField(ItemVersionCreator field) throws FieldType.MigrationProblem {
    Pair<String, String> pair = FieldType.getConnectionFieldIds(field);
    String connectionId = pair.getFirst();
    String fieldId = pair.getSecond();
    Entity enumType = ServerCustomField.createCascadeType(fieldId, connectionId);
    EnumFieldUtils.migrateField(field, myPrefix, true, ServerJira.toItemType(enumType));
  }

  @Override
  public FieldInfo createFieldInfo(ItemVersion field, String id, String name, DBAttribute<?> a, DBIdentity connection, String connectionIdPrefix,
    ScalarSequence applicability)
  {
    DBAttribute<Long> attribute = BadUtil.castScalar(Long.class, a);
    Long typeItem = field.getValue(CustomField.ENUM_TYPE);
    DBItemType type = BadUtil.getItemType(field.getReader(), typeItem);
    if (attribute == null || type == null) {
      LogHelper.error("Wrong attribute", a, id, name, typeItem, type);
      return null;
    }
    SingleEnumInfo info = JiraFields.singleEnum(ItemDownloadStage.QUICK)
      .setAttribute(attribute)
      .setOwner(connection)
      .setId(id)
      .setColumnId(connectionIdPrefix + id)
      .setConstraintId(name != null ? name : id)
      .setDisplayName(name)
      .setHideEmptyLeftField(true)
      .setConstraintKind(CascadeSchema.createNullableKind("_no_option_", "None"));
    info.buildType()
      .setUniqueKey(CustomField.ENUM_ID)
      .setType(type)
      .renderPathFromRoot(CustomField.ENUM_PARENT, " - ", CustomField.ENUM_DISPLAY_NAME, CustomField.ENUM_ID)
      .addAttributeSubloaders(CustomField.ENUM_DISPLAY_NAME, CustomField.ENUM_PARENT)
      .addParentSubloader(CustomField.ENUM_PARENT, CascadeSchema.PARENT_KEY)
      .addSubtreeSubloader(CustomField.ENUM_PARENT, CascadeSchema.SUBTREE_KEY)
      .orderByNumber(CustomField.ENUM_ORDER, true);
    info.setDefaultDnD(id, JiraFields.SEQUENCE_EDIT_APPLICABILITY, RelevantApplicability.sequence(field.getItem(), applicability));
    info.setApplicability(applicability);
    return info;
  }

  @Override
  public JQLConvertor getJqlSearch(ItemVersion field) {
    JqlSearchInfo<?> info = JqlSearchInfo.load(field);
    if (info == null) return null;
    return new JqlEnum(info.getJqlName(), info.getAttribute(), null, info.getDisplayName()) {
      protected Collection<String> loadArguments(JqlQueryBuilder context, Collection<Long> enumItems) {
        HashSet<String> result = Collections15.hashSet();
        for (ItemVersion item : context.readItems(enumItems)) {
          ItemVersion parent = item.readValue(CustomField.ENUM_PARENT);
          Integer thisId = item.getValue(CustomField.ENUM_ID);
          Integer parentId;
          Integer childId;
          if (parent == null) {
            parentId = thisId;
            childId = null;
          } else {
            childId = thisId;
            parentId = parent.getValue(CustomField.ENUM_ID);
          }
          String enumId;
          if (parentId == null) {
            LogHelper.error("Missing parent id", item, parent, childId);
            enumId = null;
          } else if (childId == null) enumId = "cascadeOption(" + parentId + ")";
          else enumId = "cascadeOption(" + parentId + "," + childId + ")";
          if (enumId != null) result.add(enumId);
        }
        return result;
      }
    };
  }

  @Override
  public boolean isEditable() {
    return true;
  }

  @Override
  public FieldEditor createEditor(ItemVersion field) {
    EnumEditorInfo<Long> info = EnumEditorInfo.SINGLE.load(field);
    if (info == null) return null;
    DropdownEditorBuilder builder = EnumEditorInfo.buildDropDown(info, false, false, null);
    return builder.createCascade(CascadeSchema.PARENT_KEY, CustomField.ENUM_DISPLAY_NAME);
  }

  @Override
  public <T> T getExtension(TypedKey<T> extensionKey) {
    if (CREATE_META.equals(extensionKey)) return extensionKey.cast(this);
    return null;
  }

  @Override
  public TLongObjectHashMap<Option> loadOptions(@Nullable TLongObjectHashMap<Option> prevResult, List<JSONObject> options) {
    if (prevResult == null) prevResult = new TLongObjectHashMap<>();
    addOptions(options, prevResult, null);
    return prevResult;
  }

  private void addOptions(List<JSONObject> options, TLongObjectHashMap<Option> target, @Nullable Option parent) {
    for (JSONObject optObject : options) {
      String strId = JRGeneric.ID_STR.getValue(optObject);
      String name = JRGeneric.VALUE.getValue(optObject);
      if (strId == null || name == null) {
        LogHelper.error("Missing cascade data", strId, name);
        continue;
      }
      int id;
      try {
        id = Integer.parseInt(strId);
      } catch (NumberFormatException e) {
        LogHelper.error("Wrong cascade id", strId);
        continue;
      }
      Option option = target.get(id);
      if (option == null) {
        option = new Option(id, name, parent, target.size());
        target.put(id, option);
      }
      addOptions(CHILDREN.list(optObject), target, option);
    }
  }

  @Override
  public void postProcess(EntityHolder field, @Nullable TLongObjectHashMap<Option> loadResult, boolean fullSet) {
    Entity type = ServerCustomField.createCascadeType(field);
    if (type == null) return;
    EnumFieldUtils.setupSingleEnumField(field, myPrefix, true, type);
    if (loadResult != null) {
      EntityTransaction transaction = field.getTransaction();
      EntityBag2.Optional allOptions = fullSet ? transaction.addBag(type).delete().toOptional() : EntityBag2.Optional.MOCK;
      for (Object obj : loadResult.getValues()) {
        Option option = (Option) obj;
        EntityHolder entity = option.createEntity(transaction, type);
        allOptions.exclude(entity);
      }
    }
  }
}
