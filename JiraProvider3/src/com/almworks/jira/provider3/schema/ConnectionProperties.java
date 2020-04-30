package com.almworks.jira.provider3.schema;

import com.almworks.dbproperties.MapDeserializer;
import com.almworks.dbproperties.MapSerializer;
import com.almworks.dbproperties.SerializeSchema;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.connector2.JiraServerVersionInfo;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionProperties {
  // Migration fields
  private static final EntityKey<String> KEY_TIME_ZONE_301 = EntityKey.string("timeZone", null);
  private static final DBAttribute<String> SERVER_TIME_ZONE_301 = ServerJira.toScalarAttribute(KEY_TIME_ZONE_301);
  private static final EntityKey<String> KEY_LOCALE_301 = EntityKey.string("serverLocale", null);
  /** JIRA has "user locale" and "system locale". This is user locale, visible in the issue XML view.  */
  private static final DBAttribute<String> SERVER_LOCALE_301 = ServerJira.toScalarAttribute(KEY_LOCALE_301);

  public static final EntityKey<byte[]> ENTITY_KEY = EntityKey.bytes("connectionProperties", null);
  private static final DBAttribute<byte[]> ATTRIBUTE = ServerJira.toScalarAttribute(ENTITY_KEY);
  
  private static final TypedKey<String> USER_LOCALE_ID = TypedKey.create("localeId", String.class);
  private static final TypedKey<String> TIME_ZONE_ID = TypedKey.create("timeZoneId", String.class);
  private static final TypedKey<String> JIRA_VERSION = TypedKey.create("jiraVersion", String.class);
  @Deprecated
  private static final TypedKey<String> JIRA_EDITION = TypedKey.create("jiraEdition", String.class);
  private static final TypedKey<List<String>> SEARCHABLE_CUSTOM_FIELDS = TypedKey.create("customFields.searchable", (Class<List<String>>)(Class)List.class);
  
  private static final SerializeSchema SCHEMA;
  static {
    SCHEMA = new SerializeSchema();
    SCHEMA.addKey(USER_LOCALE_ID);
    SCHEMA.addKey(TIME_ZONE_ID);
    SCHEMA.addKey(JIRA_VERSION);
    SCHEMA.addListKey(SEARCHABLE_CUSTOM_FIELDS, String.class);
    //noinspection deprecation
    SCHEMA.addKey(JIRA_EDITION); // keep it for compatibility
  }
  
  private final Map<TypedKey<?>, ?> myRawValues;
  private final boolean myAllowChange;
  private List<String> mySearchableCustomFields;
  private TimeZone myTimeZone;
  private JiraServerVersionInfo myJiraVersion;

  private ConnectionProperties(Map<TypedKey<?>, ?> rawValues, boolean allowChange) {
    myRawValues = rawValues;
    myAllowChange = allowChange;
  }

  public static ConnectionProperties createEmpty() {
    return new ConnectionProperties(Collections15.<TypedKey<?>, Object>hashMap(), true);
  }
  
  @NotNull
  public static ConnectionProperties load(ItemVersion connection) {
    if (connection == null) return new ConnectionProperties(Collections15.<TypedKey<?>, Object>hashMap(), false); 
    return load(connection.getReader(), connection.getItem());
  }
  
  @NotNull
  public static ConnectionProperties load(DBReader reader, long connection) {
    byte[] bytes = reader.getValue(connection, ATTRIBUTE);
    Map<TypedKey<?>, ?> map;
    if (bytes == null) map = null;
    else map = MapDeserializer.restore(bytes, SCHEMA);
    if (map == null) map = Collections15.hashMap();
    return new ConnectionProperties(map, false);
  }

  @NotNull
  public static ConnectionProperties restore(EntityHolder connection) {
    Map<TypedKey<?>, ?> map = null; 
    if (connection != null) {
      byte[] bytes = connection.getScalarValue(ENTITY_KEY);
      if (bytes != null) {
        map = MapDeserializer.restore(bytes, SCHEMA);
      }
    }
    if (map == null) map = Collections15.hashMap();
    return new ConnectionProperties(map, false);
  }
  
  public static void migrateFrom3_0_1(DBDrain drain, long connection) {
    DBReader reader = drain.getReader();
    byte[] bytes = reader.getValue(connection, ATTRIBUTE);
    if (bytes != null) {
      Map<TypedKey<?>, ?> values = MapDeserializer.restore(bytes, SCHEMA);
      if (values != null) return;
      LogHelper.error("Failed to restore connection properties", bytes);
    }
    String timeZoneId = reader.getValue(connection, SERVER_TIME_ZONE_301);
    String userLocaleId = reader.getValue(connection, SERVER_LOCALE_301);
    HashMap<TypedKey<?>,Object> rawValues = Collections15.hashMap();
    if (userLocaleId != null) USER_LOCALE_ID.putTo(rawValues, userLocaleId);
    if (timeZoneId != null) TIME_ZONE_ID.putTo(rawValues, timeZoneId);
    bytes = MapSerializer.serialize(rawValues, SCHEMA);
    if (bytes == null) LogHelper.error("Failed to store connection properties", userLocaleId, timeZoneId);
    else drain.changeItem(connection).setValue(ATTRIBUTE, bytes);
  }

  public ConnectionProperties copy() {
    return new ConnectionProperties(myRawValues, true);
  }

  @Nullable
  public List<String> getSearchableCustomFields() {
    if (mySearchableCustomFields == null) {
      List<String> fields = SEARCHABLE_CUSTOM_FIELDS.getFrom(myRawValues);
      mySearchableCustomFields = Collections15.unmodifiableListCopy(fields);
    }
    return mySearchableCustomFields;
  }

  private static final Pattern LOCALE_ID = Pattern.compile("(.{2})?_(.{2})?_(.*)?");
  private static Locale decodeLocale(String localeId, @Nullable Locale defaultLocale) {
    if (localeId == null || localeId.isEmpty()) return defaultLocale;
    Matcher m = LOCALE_ID.matcher(localeId);
    if (!m.matches()) {
      LogHelper.error("Failed to restore locale", localeId);
      return defaultLocale;
    }
    return getLocale(m.group(1), m.group(2), m.group(3), defaultLocale);
  }

  private static final ConcurrentHashMap<String, Locale> ourLocales = new ConcurrentHashMap<String, Locale>();
  private static Locale getLocale(String lang, String country, String variant, Locale defaultLocale) {
    lang = Util.NN(lang);
    country = Util.NN(country);
    variant = Util.NN(variant);
    String id = getLocaleId(lang, country, variant);
    if (id.equals("__")) return defaultLocale;
    Locale locale = ourLocales.get(id);
    if (locale != null) return locale;
    locale = new Locale(lang, country, variant);
    Locale prevLocale = ourLocales.putIfAbsent(id, locale);
    return prevLocale != null ? prevLocale : locale;
  }

  public void setSearchableCustomFields(List<String> fields) {
    if (!myAllowChange) {
      LogHelper.error("Change not allowed");
      return;
    }
    if (fields == null) {
      LogHelper.error("Null custom fields list");
      return;
    }
    fields = Collections15.arrayList(fields);
    Collections.sort(fields, Containers.comparablesComparator());
    fields = Collections.unmodifiableList(fields);
    mySearchableCustomFields = fields;
    SEARCHABLE_CUSTOM_FIELDS.putTo(myRawValues, fields);
  }

  private static String getLocaleId(String lang, String country, String variant) {
    return Util.lower(Util.NN(lang)) + "_" + Util.upper(Util.NN(country)) + "_" + Util.NN(variant);
  }

  @Nullable
  public String getTimeZoneId() {
    return TIME_ZONE_ID.getFrom(myRawValues);
  }

  @Nullable
  public TimeZone getTimeZone() {
    if (myTimeZone == null) {
      String id = getTimeZoneId();
      if (id != null && !id.isEmpty()) myTimeZone = TimeZone.getTimeZone(id);
    }
    return myTimeZone;
  }

  public void setTimeZone(TimeZone timeZone) {
    if (!myAllowChange) {
      LogHelper.error("Change not allowed");
      return;
    }
    if (timeZone == null) return;
    myTimeZone = timeZone;
    TIME_ZONE_ID.putTo(myRawValues, timeZone.getID());
  }

  @Nullable
  public JiraServerVersionInfo getJiraVersion() {
    if (myJiraVersion == null) {
      String version = JIRA_VERSION.getFrom(myRawValues);
      myJiraVersion = JiraServerVersionInfo.create(version);
    }
    return myJiraVersion;
  }

  public void setJiraVersion(JiraServerVersionInfo jiraVersion) {
    if (jiraVersion == null) return;
    if (!myAllowChange) {
      LogHelper.error("Change not allowed");
      return;
    }
    myJiraVersion = jiraVersion;
    JIRA_VERSION.putTo(myRawValues, jiraVersion.getVersion());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    ConnectionProperties other = Util.castNullable(ConnectionProperties.class, obj);
    return other != null && other.myRawValues.equals(myRawValues);
  }

  @Override
  public int hashCode() {
    LogHelper.assertError(!myAllowChange, "Can be changed later");
    return myRawValues.hashCode();
  }

  @Override
  public String toString() {
    return "CP[" + myRawValues + "]";
  }

  @Nullable
  public byte[] serialize() {
    if (!myAllowChange) {
      LogHelper.error("Change not allowed");
      return null;
    }
    return MapSerializer.serialize(myRawValues, SCHEMA);
  }

  public void updateMissingValuesFrom(ConnectionProperties other) {
    if (other == null) return;
    boolean updated = false;
    for (Map.Entry<TypedKey<?>, ?> entry : other.myRawValues.entrySet()) {
      TypedKey<?> key = entry.getKey();
      if (key.getFrom(myRawValues) == null) {
        key.copyFromTo(other.myRawValues, myRawValues);
        updated = true;
      }
    }
    if (updated) {
      myTimeZone = null;
      myJiraVersion = null;
    }
  }

  public boolean hasAllData() {
    for (TypedKey<?> key : SCHEMA.getAllKeys()) if (key.getFrom(myRawValues) == null) return false;
    return true;
  }

  public boolean isEmpty() {
    return myRawValues.isEmpty();
  }
}
