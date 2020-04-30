package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.gui.meta.util.BaseEnumInfo;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityType;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.jira.provider3.sync.download2.meta.CommonEnumOptions;
import com.almworks.jira.provider3.sync.download2.rest.EntityParser;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.download2.rest.StringIdToEntityConvertor;
import com.almworks.jira.provider3.sync.download2.rest.SupplyReference;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnumKind {
  /**
   * Override default JSON to {@link Entity entity} conversion.<br>
   * Values are fixed number of overriding parsers
   * @see #JSON_PARSERS
   */
  public static final TypedKey<String> JSON_ENUM_PARSER = TypedKey.create("jsonParser", String.class);

  private final EnumTypeKind myEnumTypeKind;
  private final JsonEntityParser<?> myParserTemplate;

  public EnumKind(EnumTypeKind enumKind, JsonEntityParser<?> parserTemplate) {
    assert parserTemplate != null;
    myEnumTypeKind = enumKind;
    myParserTemplate = parserTemplate;
  }

  public EntityType<?> createEnumType(String fieldId, String connectionId) {
    JsonEntityParser<?> parser = myParserTemplate;
    Entity type = parser.getType();
    if (type == null) {
      type = myEnumTypeKind.createType(connectionId, fieldId);
      parser = parser.withType(type);
    }
    return EntityType.create(parser, null);
  }

  public void setEnumType(BaseEnumInfo info, DBItemType type) {
    myEnumTypeKind.setEnumType(info, type);
  }

  public EnumTypeKind getEnumTypeKind() {
    return myEnumTypeKind;
  }

  private static final Map<String, EntityParser> JSON_PARSERS;
  static {
    HashMap<String, EntityParser> map = Collections15.hashMap();
    map.put("idOnly", new StringIdToEntityConvertor(ServerCustomField.ENUM_STRING_ID,ServerCustomField.ENUM_DISPLAY_NAME));
    map.put("userIdNum", new ParticipantsParser());
    map.put("ghSprint", new GhSprintParser());
    map.put("subComponent", new EntityParser.Builder()
      .map(JSONKey.textTrim("name"), ServerCustomField.ENUM_DISPLAY_NAME)
      .map(JSONKey.textOrInteger("id"), ServerCustomField.ENUM_STRING_ID)
      .create(SupplyReference.supplyProject(ServerCustomField.PROJECT)));
    map.put("tempoAccount", new EntityParser.Builder()
      .map(JSONKey.textTrim("name"), ServerCustomField.ENUM_DISPLAY_NAME)
      .map(new JSONKey<String>("id", JSONKey.TEXT_INTEGER), ServerCustomField.ENUM_STRING_ID)
      .create(null));
    JSON_PARSERS = map;
  }
  @NotNull
  public EnumKind specialize(Map<TypedKey<?>, ?> map) throws FieldType.CreateProblem {
    EntityParser parser = getEntityParser(map);
    return parser == null ? this : new EnumKind(myEnumTypeKind, myParserTemplate.withParser(parser));
  }

  @Nullable("When not specified")
  public static EntityParser getEntityParser(Map<TypedKey<?>, ?> map) throws FieldType.CreateProblem {
    String parserId = JSON_ENUM_PARSER.getFrom(map);
    if (parserId == null) return null;
    return FieldType.CreateProblem.getFromMap(parserId, JSON_PARSERS, "Unknown JSON parser");
  }

  public static final EnumTypeKind CUSTOM_ENUM = new EnumTypeKind.CustomEnumType(CommonEnumOptions.ORDERED);
  /** Custom enum that does not remove missing options when synchronizing connection */
  public static final EnumTypeKind CUSTOM_ENUM_NO_FULL_SET = new EnumTypeKind.CustomEnumType(CommonEnumOptions.ORDERED_NO_REMOVE);
  /**
   * Parses single participants value entity. The format of the value depends on mystique conditions such as plugin version and phase of the moon.<br>
   * In general it is "<i>userId</i>:<i>magicNumber</i>" like "dyoma:10000". However it may happen to be "<i>userId</i>(<i>userId</i>)" like "bjarnit2(bjarnit2)",
   * or even something meaningless like "com.atlassian.crowd.embedded.impl.ImmutableUser@34f970c5"
   */
  private static class ParticipantsParser implements EntityParser {
    private final Pattern ID_NUM = Pattern.compile("(.*):[^:]+");

    @Override
    public boolean fillEntity(Object value, @NotNull Entity entity) {
      if (value == null) return false;
      String idNum = JSONKey.TEXT_TRIM.convert(value);
      if (idNum == null || idNum.isEmpty()) return false;
      Matcher m = ID_NUM.matcher(idNum);
      String userId;
      if (!m.matches()) {
        userId = parse60(idNum);
        if (userId == null) userId = idNum;
      } else {
        userId = m.group(1);
        if (userId.isEmpty()) {
          LogHelper.error("Empty user", idNum);
          userId = idNum;
        }
      }
      entity.put(ServerUser.ID, userId).fix();
      return true;
    }

    /**
     * Atlassian changed JSON representation for (com.atlassian.jira.toolkit:participants).<br>
     * Old pattern: <b>userId</b>:<b>number</b><br>
     * New pattern: <b>userId</b>(<b>userId</b>) - a guess. Parser assumes that userId may contain '(' and ')' but any parenthesise is closed
     */
    @Nullable
    private String parse60(String value) {
      int i = value.length() - 1;
      if (value.charAt(i) != ')') {
        if (!value.contains("com.atlassian.crowd.embedded.impl.ImmutableUser")
          && !value.contains("com.atlassian.crowd.embedded.ofbiz.OfBizUser")) // Ignore well-known not parsable values
          LogHelper.error("Unexpected pattern", value);
        return null;
      }
      int closed = 1;
      while (i > 0) {
        i--;
        char c = value.charAt(i);
        if (c == ')') closed ++;
        else if (c == '(') closed --;
        if (closed == 0) break;
      }
      if (i <= 0) {
        LogHelper.error("Unexpected username pattern", value);
        return null;
      }
      return value.substring(0, i);
    }

    @Override
    public ValueSupplement<Entity> getSupplement() {
      return null;
    }
  }

  private static class GhSprintParser implements EntityParser {
    private static final Pattern VALUE = Pattern.compile("com.atlassian.greenhopper.service.sprint.Sprint@[0-9A-Fa-f]+\\[name=(.*),closed=.*,id=(\\d+)\\]");

    @Override
    public boolean fillEntity(Object object, @NotNull Entity entity) {
      String value = JSONKey.TEXT_TRIM.convert(object);
      if (value == null || value.isEmpty()) return false;
      ParseGHSprint sprint = ParseGHSprint.perform(value);
      String name = sprint.getName();
      String id = sprint.getId();
      if (name == null || id == null) {
        LogHelper.warning("Missing GH-Sprint id or name", value);
        return false;
      }
      entity.put(ServerCustomField.ENUM_STRING_ID, id);
      entity.put(ServerCustomField.ENUM_DISPLAY_NAME, name);
      return true;
    }

    @Override
    public ValueSupplement<Entity> getSupplement() {
      return null;
    }
  }
}
