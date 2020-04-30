package com.almworks.jira.provider3.sync;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.TransactionCacheKey;
import com.almworks.items.util.DBNamespace;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines constants for static issue fields
 */
public class ServerFields {
  public static final Field SUMMARY = new Field("summary", Static.SUMMARY, "Summary");
  public static final Field DESCRIPTION = new Field("description", Static.DESCRIPTION, "Description");
  public static final Field ENVIRONMENT = new Field("environment", Static.ENVIRONMENT, "Environment");
  public static final Field PARENT = Field.create("parent", "Parent");
  public static final Field CREATED = Field.create("created", "Created");
  /** Estimate time */
  public static final Field TIME_TRACKING = new Field("timetracking", Static.ESTIMATE_SECONDS, "Time Tracking");
  public static final Field DUE = new Field("duedate", Static.DUE, "Due Date");
  public static final Field ASSIGNEE = new Field("assignee", Static.ASSIGNEE, "Assignee");
  public static final Field REPORTER = new Field("reporter", Static.REPORTER, "Reporter");
  public static final Field RESOLUTION = new Field("resolution", Static.RESOLUTION, "Resolution");
  public static final Field FIX_VERSIONS = new Field("fixVersions", Static.FIX_VERSIONS, "Fix Versions");
  public static final Field AFFECT_VERSIONS = new Field("versions", Static.AFFECT_VERSIONS, "Affect Versions");
  public static final Field PROJECT = new Field("project", Static.PROJECT, "Project");
  public static final Field PRIORITY = new Field("priority", Static.PRIORITY, "Priority");
  public static final Field STATUS = new Field("status", Static.STATUS, "Status");
  public static final Field ISSUE_TYPE = new Field("issuetype", Static.ISSUE_TYPE, "Type");
  public static final Field COMPONENTS = new Field("components", Static.COMPONENTS, "Components");
  public static final Field SECURITY = new Field("security", Static.SECURITY_LEVEL, "Security Level");
  public static final Field ATTACHMENT = new Field("attachment", Static.ATTACHMENTS, "Attachments");
  public static final Field WORK_LOG = new Field("worklog", Static.TIME_TRACKER, "Work Log");
  public static final Field LINKS = Field.create("issuelinks", "Links");
  public static final Field COMMENTS = Field.create("comment", "Comments");

  public static final Collection<Field> ALL_FIELDS =
    Collections15.unmodifiableListCopy(SUMMARY, DESCRIPTION, ENVIRONMENT, TIME_TRACKING, CREATED, DUE, ASSIGNEE, REPORTER, RESOLUTION, FIX_VERSIONS, AFFECT_VERSIONS, PROJECT, PARENT, PRIORITY,
      STATUS, ISSUE_TYPE, COMPONENTS, SECURITY, ATTACHMENT, WORK_LOG, LINKS, COMMENTS);

  public static final Collection<Field> EDITABLE_FIELDS =
    Collections15.unmodifiableListCopy(SUMMARY, DESCRIPTION, ENVIRONMENT, TIME_TRACKING, DUE, ASSIGNEE, REPORTER, RESOLUTION, FIX_VERSIONS, AFFECT_VERSIONS, PROJECT, PARENT, PRIORITY,
      ISSUE_TYPE, COMPONENTS, SECURITY, ATTACHMENT, WORK_LOG, LINKS, COMMENTS);

  private static final Map<String, Field> FIELDS_BY_JIRA_ID;
  static {
    HashMap<String, Field> map = Collections15.hashMap();
    for (Field field : ALL_FIELDS) map.put(field.getJiraId(), field);
    FIELDS_BY_JIRA_ID = Collections.unmodifiableMap(map);
  }

  @Nullable
  public static Entity staticFieldEntity(String jiraId) {
    Field field = FIELDS_BY_JIRA_ID.get(jiraId);
    return field != null ? field.getEntity() : null;
  }

  public static boolean isStatic(String jiraId) {
    return FIELDS_BY_JIRA_ID.containsKey(jiraId);
  }

  @NotNull
  public static Map<ServerFields.Field, Long> resolve(DBReader reader) {
    return resolve(reader, ALL_FIELDS);
  }

  public static Map<Field, Long> resolve(DBReader reader, Collection<Field> fields) {
    Map<Field, Long> resolution = Collections15.hashMap();
    for (Field field : fields) {
      long item = field.findItem(reader);
      if (item <= 0) LogHelper.warning("Field not found", field);
      else resolution.put(field, item);
    }
    return resolution;
  }

  public static void initDB(DBDrain drain) {
    for (Field field : ALL_FIELDS) drain.materialize(field.getDBField());
  }

  private static final TransactionCacheKey<Map<ServerFields.Field, Long>> CACHE = TransactionCacheKey.create("staticFields");
  @Nullable
  public static Field find(ItemVersion fieldItem) {
    Map<Field, Long> cache = CACHE.get(fieldItem);
    if (cache == null) {
      cache = resolve(fieldItem.getReader());
      CACHE.put(fieldItem, cache);
    }
    for (Map.Entry<Field, Long> entry : cache.entrySet()) if (fieldItem.getItem() == entry.getValue()) return entry.getKey();
    return null;
  }

  public static class Static {
    private static final DBNamespace NS = ServerJira.NS.subNs("staticFields");
    /**
     * Add worklog field
     */
    public static final DBIdentifiedObject TIME_TRACKER = NS.object("timeTracker"); // todo support JC-666
    public static final DBIdentifiedObject ATTACHMENTS = NS.object("attachments"); // todo support JC-666
    public static final DBIdentifiedObject SECURITY_LEVEL = NS.object("securityLevel");
    public static final DBIdentifiedObject COMPONENTS = NS.object("components");
    public static final DBIdentifiedObject ISSUE_TYPE = NS.object("issueType");
    public static final DBIdentifiedObject STATUS = NS.object("status");
    public static final DBIdentifiedObject PRIORITY = NS.object("priority");
    public static final DBIdentifiedObject PROJECT = NS.object("project");
    public static final DBIdentifiedObject FIX_VERSIONS = NS.object("fixVersions");
    public static final DBIdentifiedObject AFFECT_VERSIONS = NS.object("affectVersions");
    public static final DBIdentifiedObject RESOLUTION = NS.object("resolution");
    public static final DBIdentifiedObject REPORTER = NS.object("reporter");
    public static final DBIdentifiedObject ASSIGNEE = NS.object("assignee");
    public static final DBIdentifiedObject DUE = NS.object("due");
    /**
     * Remaining estimate field
     */
    public static final DBIdentifiedObject ESTIMATE_SECONDS = NS.object("estimateSeconds");
    public static final DBIdentifiedObject ENVIRONMENT = NS.object("environment");
    public static final DBIdentifiedObject DESCRIPTION = NS.object("description");
    public static final DBIdentifiedObject SUMMARY = NS.object("summary");
  }

  /** Legacy IDs (used in config) */
  public static class LegacyIds {
    public static final String ID_PRIORITY = "PRIORITY_ID";
    public static final String ID_STATUS = "STATUS_ID";
    public static final String ID_KEY = "KEY";
    public static final String ID_COMPONENTS = "LIST_COMPONENTS";
    public static final String ID_ASSIGNEE = "ASSIGNEE_USER";
    public static final String ID_REPORTER = "REPORTER_USER";
    public static final String ID_PROJECT = "PROJECT_KEY";
    public static final String ID_ISSUE_TYPE = "TYPE_ID";
    public static final String ID_SECURITY = "SECURITY_ID";
    public static final String ID_RESOLUTION = "RESOLUTION_ID";
    public static final String ID_DESCRIPTION = "DESCRIPTION";
    public static final String ID_ENVIRONMENT = "ENVIRONMENT";
    public static final String ID_SUMMARY = "SUMMARY";
    public static final String ID_DUE = "DUE";
    public static final String ID_CREATED = "CREATED";
    public static final String ID_UPDATED = "UPDATED";
    public static final String ID_RESOLVED = "RESOLVED";
    public static final String ID_VOTES_COUNT = "VOTES";
    public static final String ID_VOTERS = "LIST_VOTERS";
    public static final String ID_WATCHERS_COUNT = "WATCHERS_COUNT";
    public static final String ID_WATCHERS = "LIST_WATCHERS";
    public static final String ID_ORIGINAL_ESTIMATE = "ORIGINAL_ESTIMATE";
    public static final String ID_REMAIN_ESTIMATE = "ESTIMATE";
    public static final String ID_TIMESPENT = "TIMESPENT";
  }

  public static class Field {
    /** JIRA REST field ID */
    private final String myJiraId;
    /** DB object that represents the field */
    private final DBIdentifiedObject myDBField;
    /** Default display name of the field */
    private final String myDefaultDisplayName;
    private Entity myEntity;

    public Field(String jiraId, DBIdentifiedObject DBField, String defaultDisplayName) {
      myJiraId = jiraId;
      myDBField = DBField;
      myDefaultDisplayName = defaultDisplayName;
    }

    private static Field create(String jiraId, String defaultDisplayName) {
      return new Field(jiraId, Static.NS.object(jiraId), defaultDisplayName);
    }

    public String getJiraId() {
      return myJiraId;
    }

    public DBIdentifiedObject getDBField() {
      return myDBField;
    }

    public String getDefaultDisplayName() {
      return myDefaultDisplayName;
    }

    public Entity getEntity() {
      Entity entity = myEntity;
      if (entity == null) {
        entity = StoreBridge.fromDBObject(myDBField);
        myEntity = entity;
      }
      return entity;
    }

    public long findItem(VersionSource source) {
      return findItem(source.getReader());
    }

    public long findItem(DBReader reader) {
      long item = myDBField.findItem(reader);
      if (item <= 0) LogHelper.error("Not materialized yet", this);
      return item;
    }

    @Override
    public String toString() {
      return "Field[" + myJiraId + "]";
    }
  }
}
