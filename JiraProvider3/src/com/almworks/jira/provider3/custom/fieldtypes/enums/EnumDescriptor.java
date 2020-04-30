package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.ConvertorFactory;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.schema.*;
import com.almworks.jira.provider3.sync.download2.meta.CommonEnumOptions;
import com.almworks.jira.provider3.sync.download2.rest.EntityParser;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.restconnector.json.JSONKey;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnumDescriptor {
  /**
   * If false do not get display name from XML (use id as value display name)
   * @deprecated Not used. Kept for compatibility with old data stored in DB
   */
  @Deprecated
  private static final TypedKey<Boolean> XML_DISPLAY_NAME = TypedKey.create("xmlDisplayName", Boolean.class);
  /**
   * If present defines value type. If absent means custom enum type.
   */
  private static final TypedKey<String> STATIC = TypedKey.create("static", String.class);
  /**
   * Use when CustomField.ENUM_SEARCH_TYPE is missing
   * @deprecated Not used. Kept for compatibility with old data stored in DB
   */
  @Deprecated
  private static final TypedKey<String> REMOTE_SEARCH = TypedKey.create("remoteSearch", String.class);
  /**
   * Value is 'true', 'false' or boolean system property name. Default is 'true'.
   * If set to property name - the value is obtained from the system property.
   * True means it loads full option set during connection sync and removes missing options from DB.
   * False leads to enum options are never removed - the learned options persists forever
   */
  public static final TypedKey<String> LOAD_FULL_SET = TypedKey.create("loadFullSet", String.class);

  public static final List<TypedKey<?>> KEYS = Collections15.<TypedKey<?>>unmodifiableListCopy(STATIC, REMOTE_SEARCH, XML_DISPLAY_NAME, EnumKind.JSON_ENUM_PARSER, LOAD_FULL_SET);

  private static final Map<String, EnumDescriptor> STATIC_ENUMS;
  static {
    HashMap<String,EnumDescriptor> map = Collections15.hashMap();
    map.put("project", new EnumDescriptor("singleProject", "multiProject", Project.ENUM_TYPE,
      IssueFields.P_PROJECT, "_no_project_", "None",
      new EnumConstraintFactory(Project.ID), "combo", "subset", null));
    map.put("version", new EnumDescriptor("singleVersion", "multiVersion", Version.ENUM_TYPE, IssueFields.P_VERSION, Version.NULL_CONSTRAINT_ID,
      Version.NULL_CONSTRAINT_NAME, new EnumConstraintFactory(Version.ID), "comboLegal", "subset", null));
    map.put("user", new EnumDescriptor("singleUser", "multiUser", User.ENUM_TYPE, IssueFields.P_USER,
      User.NULL_CONSTRAINT_ID, "None", ConvertorFactory.USER, "combo", "subset", User.CREATOR));
    map.put("group", new EnumDescriptor("singleGroup", "multiGroup",
      Group.ENUM_TYPE, IssueFields.P_GROUP, Group.NULL_CONSTRAINT_ID, "None", new EnumConstraintFactory(Group.ID), "combo", "subset", Group.CREATOR));
    STATIC_ENUMS = map;
  }

  private final String mySinglePrefix;
  private final String myMultiPrefix;
  private final EnumKind myEnumKind;
  private final String myNoneId;
  private final String myNoneName;
  private final ConvertorFactory myConstraintFactory;
  private final String mySingleEditorId;
  private final String myMultiEditorId;
  private final EnumItemCreator myCreator;

  private EnumDescriptor(String singlePrefix, String multiPrefix,
    DBStaticObject enumType, JsonEntityParser parser, String noneId, String noneName,
    ConvertorFactory constraintFactory, String singleEditor, String multiEditor, EnumItemCreator creator) {
    this(singlePrefix, multiPrefix, EnumKind.withType(new EnumTypeKind.StaticEnumType(parser.getType(), enumType), parser), noneId, noneName, constraintFactory, singleEditor, multiEditor, creator);
  }

  private EnumDescriptor(String singlePrefix, String multiPrefix, EnumKind enumKind, String noneId, String noneName, ConvertorFactory constraintFactory, String singleEditor,
    String multiEditor, EnumItemCreator creator) {
    mySinglePrefix = singlePrefix;
    myMultiPrefix = multiPrefix;
    myEnumKind = enumKind;
    myNoneId = noneId;
    myNoneName = noneName;
    myConstraintFactory = constraintFactory;
    mySingleEditorId = singleEditor;
    myMultiEditorId = multiEditor;
    myCreator = creator;
  }

  private static final JSONKey<String> ENUM_VALUE = JSONKey.textTrim("value");
  private static final JSONKey<String> ENUM_ID = JSONKey.textTrim("id");
  private static final EntityParser DEFAULT_ENTITY_PARSER = new EntityParser.Builder()
    .map(ENUM_VALUE, ServerCustomField.ENUM_DISPLAY_NAME)
    .map(ENUM_ID, ServerCustomField.ENUM_STRING_ID)
    .create(null);

  @NotNull
  public static EnumDescriptor getEnumDescriptor(Map<TypedKey<?>, ?> map) throws FieldType.CreateProblem {
    String enumId = STATIC.getFrom(map);
    if (enumId != null) {
      EnumDescriptor staticDescriptor = FieldType.CreateProblem.getFromMap(enumId, STATIC_ENUMS, "Unknown static enum");
      return staticDescriptor.specialize(map);
    }

    EntityParser parser = Util.NN(EnumKind.getEntityParser(map), DEFAULT_ENTITY_PARSER);

    JsonEntityParser.Impl<?> jsonParser = JsonEntityParser.create(null, ServerCustomField.ENUM_STRING_ID, ServerCustomField.ENUM_DISPLAY_NAME, parser, "id");
    EnumKind enumInfo = EnumKind.withoutType(fromLoadFullSetting(LOAD_FULL_SET.getFrom(map)), jsonParser);

    EnumConstraintFactory constraintFactory = new EnumConstraintFactory(CustomField.ENUM_DISPLAY_NAME);
    return new EnumDescriptor("singleEnum", "multiEnum", enumInfo, "_no_option_", "None", constraintFactory, "combo", "subset", null);
  }

  public static EnumTypeKind fromLoadFullSetting(@Nullable String setting) {
    setting = Util.NN(setting).trim();
    if (setting.isEmpty() || "true".equals(setting)) return EnumKind.CUSTOM_ENUM;
    if ("false".equals(setting)) return EnumKind.CUSTOM_ENUM_NO_FULL_SET;
    return new EnumTypeKind.CustomEnumType(CommonEnumOptions.removeByProperty(true, setting, true));
  }

  @NotNull
  private EnumDescriptor specialize(Map<TypedKey<?>, ?> map) throws FieldType.CreateProblem {
    EnumKind newKind = myEnumKind.specialize(map);
    if (newKind != myEnumKind) return new EnumDescriptor(mySinglePrefix, myMultiPrefix, newKind, myNoneId, myNoneName, myConstraintFactory, mySinglePrefix, myMultiPrefix, myCreator);
    return this;
  }

  @NotNull
  public String getSinglePrefix(boolean editable) {
    return mySinglePrefix + (editable ? "" : "RO");
  }

  public String getMultiPrefix(boolean editable) {
    return myMultiPrefix + (editable ? "" : "RO");
  }

  public EnumKind getEnumKind() {
    return myEnumKind;
  }

  public String getNoneId() {
    return myNoneId;
  }

  public String getNoneName() {
    return myNoneName;
  }

  public ConvertorFactory getConstraintFactory() {
    return myConstraintFactory;
  }

  public String getSingleEditorId() {
    return mySingleEditorId;
  }

  public String getMultiEditorId() {
    return myMultiEditorId;
  }

  public EnumItemCreator getCreator() {
    return myCreator;
  }
}
