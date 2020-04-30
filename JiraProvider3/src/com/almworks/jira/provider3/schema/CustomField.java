package com.almworks.jira.provider3.schema;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.util.AttributeReference;
import com.almworks.items.cache.util.ItemSetAttribute;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;

public class CustomField {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerCustomField.TYPE);
  public static final DBAttribute<String> ID = ServerJira.toScalarAttribute(ServerCustomField.ID);
  public static final DBAttribute<String> KEY = ServerJira.toScalarAttribute(ServerCustomField.KEY);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerCustomField.NAME);
  public static final DBAttribute<Long> ATTRIBUTE = ServerJira.toLinkAttribute(ServerCustomField.ATTRIBUTE);
  public static final DBAttribute<Long> ENUM_TYPE = ServerJira.toLinkAttribute(ServerCustomField.ENUM_TYPE);
  public static final DBAttribute<String> ENUM_STRING_ID = ServerJira.toScalarAttribute(ServerCustomField.ENUM_STRING_ID);
  public static final DBAttribute<String> ENUM_DISPLAY_NAME = ServerJira.toScalarAttribute(ServerCustomField.ENUM_DISPLAY_NAME);
  public static final DBAttribute<Integer> ENUM_ID = ServerJira.toScalarAttribute(ServerCustomField.ENUM_ID);
  public static final DBAttribute<Integer> ENUM_ORDER = ServerJira.toScalarAttribute(ServerCustomField.ENUM_ORDER);
  public static final DBAttribute<Long> ENUM_PARENT = ServerJira.toLinkAttribute(ServerCustomField.ENUM_PARENT);
  public static final ItemSetAttribute ONLY_IN_PROJECTS = new ItemSetAttribute(ServerJira.toLinkSetAttribute(
    ServerCustomField.ONLY_IN_PROJECTS));
  public static final ItemSetAttribute ONLY_IN_ISSUE_TYPES = new ItemSetAttribute(ServerJira.toLinkSetAttribute(
    ServerCustomField.ONLY_IN_ISSUE_TYPES));

  public static final AttributeReference<?> ATTRIBUTE2 = AttributeReference.create(ATTRIBUTE);

  public static long getCustomField(DBReader reader, DBAttribute<?> attribute) {
    long attrItem = reader.findMaterialized(attribute);
    if (attrItem <= 0) return 0;
    LongArray fields = reader.query(DPEquals.create(ATTRIBUTE, attrItem)).copyItemsSorted();
    LogHelper.assertError(fields.size() <= 1, "Many fields for single attribute", attribute, fields);
    return fields.isEmpty() ? 0 : fields.get(0);
  }

  public static long getCustomField(DBReader reader, long connection, String fieldId) {
    LongArray fields = reader.query(DPEquals.create(ID, fieldId).and(DPEquals.create(SyncAttributes.CONNECTION, connection))).copyItemsSorted();
    if (fields.isEmpty()) return 0;
    if (fields.size() > 1) {
      LogHelper.error("Ambiguous field id", connection, fieldId, fields);
      return 0;
    }
    return fields.get(0);
  }

  /**
   * @return custom field items with known JIRA key for specified connection
   */
  public static LongArray queryKnownKey(DBReader reader, long connection) {
    return reader.query(
      DPEqualsIdentified.create(DBAttribute.TYPE, CustomField.DB_TYPE)
        .and(
          DPEquals.create(SyncAttributes.CONNECTION, connection).and(DPNotNull.create(CustomField.KEY)))
    ).copyItemsSorted();
  }

  public static LongArray queryKnownKey(ItemVersion connection) {
    return queryKnownKey(connection.getReader(), connection.getItem());
  }
}
