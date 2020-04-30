package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.issue.editor.IssueScreen;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.JSONValueException;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;

class JiraScreens {
  private static final EntityKey<String> JIRA_FIELD_CONFIG = EntityKey.string("connection.field.config", null);
  private static final DBAttribute<String> FIELDS_CONFIG = ServerJira.toScalarAttribute(JIRA_FIELD_CONFIG);

  private static final JSONKey<Integer> VERSION = JSONKey.integer("version");
  private static final ArrayKey<JSONObject> CONFIGS = ArrayKey.objectArray("configs");
  private static final ArrayKey<JSONObject> SCREENS = ArrayKey.objectArray("screens");
  private static final ArrayKey<JSONObject> TYPES = ArrayKey.objectArray("types");
  private static final ArrayKey<JSONObject> SCREEN_ITEMS = ArrayKey.objectArray("items");
  private static final ArrayKey<JSONObject> TABS = ArrayKey.objectArray("tabs");
  private static final ArrayKey<JSONObject> FIELDS = ArrayKey.objectArray("fields");
  private static final JSONKey<Long> SCREEN_ID = JSONKey.longInt("screenId");
  private static final JSONKey<Long> LID = JSONKey.longInt("id");
  private static final JSONKey<String> SID = JSONKey.textNNTrim("id");
  private static final JSONKey<Integer> POSITION = JSONKey.integer("position");
  private static final JSONKey<String> NAME = JSONKey.textNNTrim("name");
  private static final JSONKey<String> OPERATION = JSONKey.textNNTrim("opName");

  private final ScreenScheme myJIRAScheme;
  private final Collection<ScreenScheme.ScreenInfo> myAllScreens;

  private JiraScreens(ScreenScheme JIRAScheme, Collection<ScreenScheme.ScreenInfo> allScreens) {
    myJIRAScheme = JIRAScheme;
    myAllScreens = allScreens;
  }

  public ScreenScheme resolve(ItemVersion connection) {
    ScreenScheme result = new ScreenScheme();
    for (Map.Entry<Long, Map<Long, Map<Integer, ScreenScheme.ScreenInfo>>> prjEntry : myJIRAScheme.getProjectTypeOperationToScreen().entrySet()) {
      Long projectId = prjEntry.getKey();
      Long project = Project.resolveById(connection, projectId.intValue());
      if (project == null) continue;
      for (Map.Entry<Long, Map<Integer, ScreenScheme.ScreenInfo>> typeEntry : prjEntry.getValue().entrySet()) {
        Long typeId = typeEntry.getKey();
        Long type;
        if (typeId == null) type = null;
        else {
          type = IssueType.resolveById(connection, typeId.intValue());
          if (type == null) continue;
        }
        for (Map.Entry<Integer, ScreenScheme.ScreenInfo> entry : typeEntry.getValue().entrySet()) result.addScreen(project, type, entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  @Nullable
  public ScreenScheme.ScreenInfo getScreen(long screenId) {
    for (ScreenScheme.ScreenInfo screen : myAllScreens) if (screenId == screen.getId()) return screen;
    return null;
  }

  public Collection<ScreenScheme.ScreenInfo> getAllScreens() {
    return myAllScreens;
  }

  public static JiraScreens parse(JSONObject root) throws JSONValueException {
    int version = VERSION.getNotNull(root);
    if (1 != version) throw new JSONValueException("Unsupported screens version: " + version);
    Map<Long, ScreenScheme.ScreenInfo> screens = loadScreens(SCREENS.list(root));
    ScreenScheme scheme = new ScreenScheme();
    for (JSONObject project : CONFIGS.list(root)) {
      Long projectId = LID.getNotNull(project);
      for (JSONObject type : TYPES.list(project)) {
        String strTypeId = SID.getNotNull(type);
        Long typeId;
        if (strTypeId.isEmpty()) typeId = null;
        else
          try {
            typeId = Long.parseLong(strTypeId);
          } catch (NumberFormatException e) {
            LogHelper.error("Wrong type ID", strTypeId);
            continue;
          }
        for (JSONObject item : SCREEN_ITEMS.list(type)) {
          Long screenId = SCREEN_ID.getNotNull(item);
          ScreenScheme.ScreenInfo screen = screens.get(screenId);
          if (screen == null) throw new JSONValueException("Unknown screen ID " + screenId + " (project=" + projectId + ", type=" + typeId + ")");
          scheme.addScreen(projectId, typeId, OPERATION.getNotNull(item), screen);
        }
      }
    }
    return new JiraScreens(scheme, screens.values());
  }

  private static Map<Long, ScreenScheme.ScreenInfo> loadScreens(List<JSONObject> screens) throws JSONValueException {
    HashMap<Long,ScreenScheme.ScreenInfo> result = Collections15.hashMap();
    for (JSONObject screen : screens) {
      Long id = LID.getNotNull(screen);
      List<Pair<Integer, IssueScreen.Tab>> tabs = loadTabs(TABS.list(screen));
      Collections.sort(tabs, Containers.convertingComparator(Pair.<Integer>convertorGetFirst()));
      result.put(id, new ScreenScheme.ScreenInfo(id, NAME.getNotNull(screen), Pair.<IssueScreen.Tab>convertorGetSecond().collectList(tabs)));
    }
    return result;
  }

  private static List<Pair<Integer, IssueScreen.Tab>> loadTabs(List<JSONObject> tabs) throws JSONValueException {
    ArrayList<Pair<Integer, IssueScreen.Tab>> result = Collections15.arrayList();
    for (JSONObject tab : tabs) {
      List<Pair<Integer, String>> fields = loadFields(FIELDS.list(tab));
      Collections.sort(fields, Containers.convertingComparator(Pair.<Integer>convertorGetFirst()));
      result.add(Pair.create(POSITION.getNotNull(tab), new IssueScreen.Tab(NAME.getNotNull(tab), Pair.<String>convertorGetSecond().collectList(fields))));
    }
    return result;
  }

  private static List<Pair<Integer, String>> loadFields(List<JSONObject> fields) throws JSONValueException {
    ArrayList<Pair<Integer, String>> result = Collections15.arrayList();
    for (JSONObject field : fields) result.add(Pair.create(POSITION.getNotNull(field), SID.getNotNull(field)));
    return result;
  }

  public static void updateScreens(EntityTransaction transaction, JSONObject screens) {
    try {
      JiraScreens.parse(screens); // Check reply (syntax, etc)
    } catch (JSONValueException e) {
      LogHelper.error("Failed to parse screen configuration", e);
      return;
    }
    ServerInfo.changeConnection(transaction).setValue(JIRA_FIELD_CONFIG, screens.toJSONString());
  }

  public static JiraScreens load(ItemVersion connection) {
    String json = connection.getValue(FIELDS_CONFIG);
    if (json == null) return null;
    try {
      Object parsed = new JSONParser().parse(json);
      return parse(JSONKey.ROOT_OBJECT.getNotNull(parsed));
    } catch (ParseException e) {
      LogHelper.error(e);
      return null;
    } catch (JSONValueException e) {
      LogHelper.error(e);
      return null;
    }
  }
}
