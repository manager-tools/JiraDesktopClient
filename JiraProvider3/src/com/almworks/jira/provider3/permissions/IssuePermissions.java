package com.almworks.jira.provider3.permissions;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.integers.IntIterator;
import com.almworks.integers.IntList;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.sax.PeekEntryValue;
import com.almworks.restconnector.json.sax.PeekObjectEntry;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.properties.Role;
import com.almworks.util.text.TextUtil;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongLongProcedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.*;
import java.util.logging.Level;

public class IssuePermissions {
  private static final LocalizedAccessor I18N = CurrentLocale.createAccessor(IssuePermissions.class.getClassLoader(), "com/almworks/jira/provider3/permissions/message");
  private static final LocalizedAccessor.Value A_LOAD_GLOBAL = I18N.getFactory("globalPermissions.load");
  public static final Role<IssuePermissions> ROLE = Role.role(IssuePermissions.class);

  private static final EntityKey<Integer> KEY_PERMISSIONS = EntityKey.integer("permissions.flags", null);
  private static final DBAttribute<Integer> ATTR_PERMISSIONS = ServerJira.toScalarAttribute(KEY_PERMISSIONS);
  private static final DBStaticObject KEY_PERMISSIONS_FLAG = MetaSchema.intFlagsKey(ATTR_PERMISSIONS, "PermissionsFlags");

  public static final int WORKLOG_DELETE_OWN = 1;
  public static final int WORKLOG_DELETE_ALL = 1 << 1;
  public static final int WORKLOG_EDIT_ALL = 1 << 2;
  public static final int WORKLOG_EDIT_OWN = 1 << 3;
  public static final int ADD_COMMENT = 1 << 4;
  public static final int COMMENT_EDIT_OWN = 1 << 5;
  public static final int COMMENT_EDIT_ALL = 1 << 6;
  public static final int ADD_WORKLOG = 1 << 7;
  public static final int ATTACH_FILE = 1 << 8;
  public static final int TOGGLE_WATCH_ISSUE = 1 << 9;
  public static final int TOGGLE_VOTE_ISSUE = 1 << 10;
  public static final int MANAGER_WATCHERS = 1 << 11;
  public static final int CREATE_SUBTASK = 1 << 12;
  public static final int DELETE_ISSUE = 1 << 13;
  public static final int LINK_ISSUE = 1 << 14;
  public static final int EDIT_ISSUE = 1 << 15;
  public static final int COMMENT_DELETE_OWN = 1 << 16;
  public static final int COMMENT_DELETE_ALL = 1 << 17;
  public static final int ATTACHMENT_DELETE_OWN = 1 << 18;
  public static final int ATTACHMENT_DELETE_ALL = 1 << 19;

  private static final Map<String, Integer> GLOBAL_PERMISSIONS;
  private static final Map<String, Integer> KNOW_PERMISSIONS;
  static {
    // Permissions were renamed in JIRA 6.4-OD-05-008 (build number 64003 on 10 Sep 2014)
    HashMap<String, Integer> map = Collections15.hashMap();
    map.put("DELETE_OWN_WORKLOGS", WORKLOG_DELETE_OWN);
    map.put("DELETE_ALL_WORKLOGS", WORKLOG_DELETE_ALL);
    map.put("EDIT_ALL_WORKLOGS", WORKLOG_EDIT_ALL);
    map.put("EDIT_OWN_WORKLOGS", WORKLOG_EDIT_OWN);
    map.put("EDIT_OWN_COMMENTS", COMMENT_EDIT_OWN);
    map.put("EDIT_ALL_COMMENTS", COMMENT_EDIT_ALL);
    map.put("EDIT_ISSUES", EDIT_ISSUE); // Permission renamed in JIRA 6.3.? (? - seems to be 6)
    map.put("DELETE_OWN_COMMENTS", COMMENT_DELETE_OWN);
    map.put("DELETE_ALL_COMMENTS", COMMENT_DELETE_ALL);
    map.put("DELETE_OWN_ATTACHMENTS", ATTACHMENT_DELETE_OWN);
    map.put("DELETE_ALL_ATTACHMENTS", ATTACHMENT_DELETE_ALL);
    map.put("MANAGE_WATCHERS", MANAGER_WATCHERS);
    map.put("WORK_ON_ISSUES", ADD_WORKLOG);
    map.put("CREATE_ATTACHMENTS", ATTACH_FILE);
    map.put("ADD_COMMENTS", ADD_COMMENT);
    map.put("DELETE_ISSUES", DELETE_ISSUE);
    map.put("LINK_ISSUES", LINK_ISSUE);
    GLOBAL_PERMISSIONS = Collections.unmodifiableMap(map);

    map = new HashMap<>(GLOBAL_PERMISSIONS);
    map.put("ATTACHMENT_DELETE_ALL", ATTACHMENT_DELETE_ALL);
    map.put("ATTACHMENT_DELETE_OWN", ATTACHMENT_DELETE_OWN);
    map.put("CREATE_ATTACHMENT", ATTACH_FILE);
    map.put("COMMENT_DELETE_ALL", COMMENT_DELETE_ALL);
    map.put("COMMENT_DELETE_OWN", COMMENT_DELETE_OWN);
    map.put("COMMENT_EDIT_ALL", COMMENT_EDIT_ALL);
    map.put("COMMENT_EDIT_OWN", COMMENT_EDIT_OWN);
    map.put("DELETE_ISSUE", DELETE_ISSUE);
    map.put("EDIT_ISSUE", EDIT_ISSUE);
    map.put("LINK_ISSUE", LINK_ISSUE);
    map.put("MANAGE_WATCHER_LIST", MANAGER_WATCHERS);
    map.put("WORKLOG_DELETE_ALL", WORKLOG_DELETE_ALL);
    map.put("WORKLOG_DELETE_OWN", WORKLOG_DELETE_OWN);
    map.put("WORKLOG_EDIT_ALL", WORKLOG_EDIT_ALL);
    map.put("WORKLOG_EDIT_OWN", WORKLOG_EDIT_OWN);
    map.put("WORK_ISSUE", ADD_WORKLOG);
    KNOW_PERMISSIONS = Collections.unmodifiableMap(map);
  }
  private static final Map<String, Integer> KNOWN_OPERATIONS;
  static {
    HashMap<String, Integer> map = Collections15.hashMap();
    map.put("comment-issue", ADD_COMMENT);
    map.put("log-work", ADD_WORKLOG);
    map.put("attach-file", ATTACH_FILE);
    map.put("toggle-watch-issue", TOGGLE_WATCH_ISSUE);
    map.put("toggle-vote-issue", TOGGLE_VOTE_ISSUE);
//    map.put("manage-watchers", MANAGER_WATCHERS);
    map.put("create-subtask", CREATE_SUBTASK);
    map.put("delete-issue", DELETE_ISSUE);
    map.put("link-issue", LINK_ISSUE);
    KNOWN_OPERATIONS = Collections.unmodifiableMap(map);
  }

  private final Object myLock = new Object();
  private int myGlobalPermissions;
  private final TLongLongHashMap myProjectPermissions = new TLongLongHashMap();
  private final JiraConnection3 myConnection;

  public IssuePermissions(JiraConnection3 connection) {
    myConnection = connection;
  }

  public void loadGlobalPermissions(RestSession session, EntityTransaction transaction, ProgressInfo progress, IntList projectIds) throws CancelledException {
    progress.startActivity(A_LOAD_GLOBAL.create());
    Collection<String> permissions = loadPermissions(session, "", GLOBAL_PERMISSIONS.keySet());
    if (permissions != null) {
      int encoded = encodePermissions(permissions);
      updateGlobalPermissions(encoded);
      ServerInfo.changeConnection(transaction).setNNValue(KEY_PERMISSIONS, encoded);
      LogHelper.debug("Global permissions loaded:", encoded);
    } else LogHelper.debug("Global permissions not loaded");
    for (IntIterator cursor : projectIds) {
      int projectId = cursor.value();
      permissions = loadPermissions(session, "projectId=" + projectId, GLOBAL_PERMISSIONS.keySet());
      if (permissions != null) {
        int encoded = encodePermissions(permissions);
        updateProjectPermissions(projectId, encoded);
        EntityHolder prj = transaction.addEntity(ServerProject.project(projectId));
        if (prj != null) prj.setValue(KEY_PERMISSIONS, encoded);
        LogHelper.debug("Project permissions loaded. Project=", projectId, "permissions=", encoded);
      }
    }
    updateProjects(projectIds);
  }

  private void updateProjects(final IntList projectIds) {
    synchronized (myLock) {
      myProjectPermissions.retainEntries(new TLongLongProcedure() {
        @Override
        public boolean execute(long a, long b) {
          return projectIds.contains((int) a);
        }
      });
    }
  }

  private void updateProjectPermissions(int projectId, int permissions) {
    synchronized (myLock) {
      myProjectPermissions.put(projectId, permissions);
    }
  }

  private void updateGlobalPermissions(int permissions) {
    synchronized (myLock) {
      myGlobalPermissions = permissions;
    }
  }

  @Nullable("When error occurred")
  private Collection<String> loadPermissions(RestSession session, String args, Collection<String> queryPermissions) {
    StringBuilder path = new StringBuilder("api/2/mypermissions?");
    if (args != null && args.length() > 0)
      path.append(args).append("&");
    path
      .append("permissions=")
      .append(TextUtil.separate(queryPermissions, ","));
    RestResponse response;
    try {
      response = session.restGet(path.toString(), RequestPolicy.SAFE_TO_RETRY);
      response.ensureSuccessful();
    } catch (ConnectorException e) {
      LogHelper.error(e);
      return null;
    }
    StringBuilder debugInfo = LogHelper.isLoggable(Level.INFO) ? new StringBuilder() : null;
    ParsePermissions parser = new ParsePermissions(debugInfo);
    try {
      response.parseJSON(PeekObjectEntry.objectEntry("permissions", PeekEntryValue.objectValue(parser)));
    } catch (Exception e) {
      LogHelper.error(e);
      return null;
    }
    if (debugInfo != null) LogHelper.debug("[IssuePermissions]: Loaded permissions\n", debugInfo.toString());
    return parser.myPermissions;
  }

  private int encodePermissions(Collection<String> perms) {
    int encoded = encode(perms, KNOW_PERMISSIONS);
    if (LogHelper.isLoggable(Level.INFO)) LogHelper.debug("[IssuePermissions]: encoded permissions", encoded, perms);
    return encoded;
  }

  private int encodeOperations(Collection<String> operations) {
    int encoded = encode(operations, KNOWN_OPERATIONS);
    if (LogHelper.isLoggable(Level.INFO)) LogHelper.debug("[IssuePermissions]: encoded operations", encoded, operations);
    return encoded;
  }

  private int encode(Collection<String> ids, Map<String, Integer> mapping) {
    if (ids == null) return 0;
    int flags = 0;
    for (String permission : ids) {
      Integer mask = mapping.get(permission);
      if (mask != null) flags = flags | mask;
    }
    return flags;
  }

  public void loadIssuePermissions(EntityTransaction transaction, RestSession session, int issueId) {
    Collection<String> permissions = loadPermissions(session, "issueId=" + issueId, GLOBAL_PERMISSIONS.keySet());
    Collection<String> operations = loadIssueOperations(session, issueId);
    if (permissions == null && operations == null) {
      LogHelper.debug("No issue permissions or operations loaded", issueId);
      return;
    }
    int flags = 0;
    if (permissions != null) flags = encodePermissions(permissions);
    else LogHelper.debug("No issue permissions loaded", issueId);
    if (operations != null) flags = flags | encodeOperations(operations);
    else LogHelper.debug("No issue operations loaded", issueId);
    EntityHolder issue = ServerIssue.create(transaction, issueId, null);
    if (issue != null) issue.setValue(KEY_PERMISSIONS, flags);
    LogHelper.debug("Issue permissions and operations loaded", flags);
  }

  private static final JSONKey<JSONObject> OPERATIONS = JSONKey.object("operations");
  private static final ArrayKey<JSONObject> LINK_GROUPS = ArrayKey.objectArray("linkGroups");
  private static final ArrayKey<JSONObject> LINKS = ArrayKey.objectArray("links");
  private static final ArrayKey<JSONObject> GROUPS = ArrayKey.objectArray("groups");
  private static final JSONKey<String> LINK_ID = JSONKey.text("id");

  private Collection<String> loadIssueOperations(RestSession session, int issueId) {
    JSONObject operations;
    try {
      RestResponse response = session.restGet("api/2/issue/" + issueId + "?fields=id&expand=operations", RequestPolicy.SAFE_TO_RETRY);
      response.ensureSuccessful();
      try {
        operations = OPERATIONS.getValue(response.getJSONObject());
      } catch (ParseException e) {
        LogHelper.warning("Failed to parse operations");
        operations = null;
      }
      if (operations == null) {
        LogHelper.warning("Missing operations");
        return null;
      }
    } catch (ConnectorException e) {
      return null;
    }
    ArrayList<String> operationIds = Collections15.arrayList();
    for (JSONObject linkGroup : LINK_GROUPS.list(operations)) processOperationGroup(linkGroup, operationIds);
    return operationIds;
  }

  private void processOperationGroup(JSONObject group, ArrayList<String> target) {
    for (JSONObject subGroup : GROUPS.list(group)) processOperationGroup(subGroup, target);
    for (JSONObject link : LINKS.list(group)) {
      String id = LINK_ID.getValue(link);
      if (id != null) target.add(id);
    }
  }

  public void load(DBDrain drain) {
    ItemVersion connection = drain.forItem(myConnection.getConnectionObj());
    Integer global = connection.getValue(ATTR_PERMISSIONS);
    LongArray
      projects = drain.getReader().query(DPEqualsIdentified.create(DBAttribute.TYPE, Project.DB_TYPE).and(DPEquals.create(SyncAttributes.CONNECTION, connection.getItem()))).copyItemsSorted();
    TLongLongHashMap prjPermissions = new TLongLongHashMap();
    for (ItemVersion project : drain.readItems(projects)) {
      Integer perProject = project.getValue(ATTR_PERMISSIONS);
      Integer id = project.getValue(Project.ID);
      if (id == null || perProject == null) continue;
      prjPermissions.put(id, perProject);
    }
    synchronized (myLock) {
      if (myGlobalPermissions == 0 && global != null) myGlobalPermissions = global;
      prjPermissions.forEachEntry(new TLongLongProcedure() {
        @Override
        public boolean execute(long a, long b) {
          if (!myProjectPermissions.containsKey(a)) myProjectPermissions.put(a, b);
          return true;
        }
      });
    }
  }

  private int getProjectPermissions(@Nullable Integer projectId) {
    synchronized (myLock) {
      if (projectId != null && myProjectPermissions.containsKey(projectId)) return (int) myProjectPermissions.get(projectId);
      else return myGlobalPermissions;
    }
  }

  /**
   * @param permissionsMask should be single-bit permission constant
   * @return if current user has the specified permission for the issue<br>
   * if issue permissions are available result is based on issue permissions<br>
   * otherwise if the permission is JIRA-operation returns true<br>
   * if the permission is JIRA-permission result is based on issue project permissions or global permissions (if no project permissions is available)
   */
  public static boolean hasPermission(ItemWrapper issue, int permissionsMask) {
    if (LogHelper.isLoggable(Level.INFO)) LogHelper.debug("[IssuePermissions]: checking permissions", permissionsMask, issue);
    final JiraConnection3 connection = JiraConnection3.getInstance(issue.services());
    if(connection == null) {
      LogHelper.debug("[IssuePermissions]: missing connection", issue);
      return false;
    }
    GuiFeaturesManager features = connection.getGuiFeatures();
    LoadedModelKey<Integer> flagsKey = features.findScalarKey(KEY_PERMISSIONS_FLAG, Integer.class);
    Integer flags = null;
    if (flagsKey != null) flags = issue.getModelKeyValue(flagsKey);
    if (flags == null) {
      if (isJiraOperation(permissionsMask)) {
        LogHelper.debug("[IssuePermissions]: JIRA operation", permissionsMask, issue);
        return true;
      }
      IssuePermissions permissions = connection.getActor(ROLE);
      LoadedModelKey<ItemKey> keyProject = features.findScalarKey(MetaSchema.KEY_PROJECT, ItemKey.class);
      Integer prjId;
      if (keyProject != null) {
        LoadedItemKey project = Util.castNullable(LoadedItemKey.class, issue.getModelKeyValue(keyProject));
        prjId = project != null ? project.getValue(Project.ID) : null;
      } else prjId = null;
      flags = permissions.getProjectPermissions(prjId);
      if (LogHelper.isLoggable(Level.INFO)) LogHelper.debug("[IssuePermissions]: using project permissions", prjId, flags, flagsKey, issue);
    } else {
      if (LogHelper.isLoggable(Level.INFO)) LogHelper.debug("[IssuePermissions]: using issue permissions", flags, issue);
    }
    boolean result = flags != null && (flags & permissionsMask) == permissionsMask;
    if (LogHelper.isLoggable(Level.INFO)) LogHelper.debug("[IssuePermissions]: checking permissions done", result, flags, permissionsMask, issue);
    return result;
  }

  private static boolean isJiraOperation(int permissionMask) {
    return KNOWN_OPERATIONS.containsValue(permissionMask);
  }

  public static void materialize(DBDrain drain) {
    drain.materialize(KEY_PERMISSIONS_FLAG);
  }

  private static class ParsePermissions implements Procedure2<String, JSONObject> {
    private static final JSONKey<Boolean> HAVE_PERMISSION  = JSONKey.bool("havePermission");
    private final HashSet<String> myPermissions = Collections15.hashSet();
    @Nullable
    private final StringBuilder myDebugInfo;

    public ParsePermissions(@Nullable StringBuilder debugInfo) {
      myDebugInfo = debugInfo;
    }

    @Override
    public void invoke(String key, JSONObject permission) {
      Boolean have = HAVE_PERMISSION.getValue(permission);
      if (have == null) LogHelper.error("No havePermission", key);
      else {
        if (have) myPermissions.add(key);
        if (myDebugInfo != null) myDebugInfo.append(key).append("=").append(have).append("\n");
      }
    }
  }
}
