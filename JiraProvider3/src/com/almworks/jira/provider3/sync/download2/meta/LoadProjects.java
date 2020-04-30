package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.ConnectorException;
import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
import com.almworks.integers.IntList;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.*;
import com.almworks.jira.provider3.sync.schema.*;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.Trio;
import com.almworks.util.collections.ConvertingList;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Loads from rest/api/2/project, rest/api/2/project/KEY
 * <ul>
 * <li>projects</li>
 * <li>issue types</li>
 * <li>project roles</li>
 * <li>components</li>
 * <li>versions</li>
 * </ul>
 */
class LoadProjects {
  private final EntityTransaction myTransaction;
  private final ProjectsAndTypes myProjectsAndTypes = new ProjectsAndTypes();
  private final TLongObjectHashMap<EntityHolder> myTypesById = new TLongObjectHashMap<>();
//  /** Sorted by project id */
  private final List<Pair<EntityHolder, IntList>> myTypeIdsInProject = Collections15.arrayList();
  private final boolean myProcessTypes;

  LoadProjects(EntityTransaction transaction, boolean processTypes) {
    myTransaction = transaction;
    myProcessTypes = processTypes;
  }

  /**
   * @return list of (projectId, projectKey, projectName) for all configured projects
   */
  public static ProjectsAndTypes perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws ConnectorException {
    @Nullable Set<Integer> filter = context.getDataOrNull(LoadRestMeta.PROJECT_FILTER);
    ArrayList<Trio<Integer, String, String>> projects = loadBriefProjects(session, transaction, progress.spawn(0.2));
    filterProjects(projects, filter);
    LoadProjects operation = new LoadProjects(transaction, true);
    operation.loadFullProjects(session, projects, progress);
    return operation.myProjectsAndTypes;
  }

  public static ArrayList<Trio<Integer, String, String>> loadBriefProjects(RestSession session, EntityTransaction transaction, ProgressInfo progress)
    throws ConnectorException
  {
    List<JSONObject> projects = RestOperations.projectsBrief(session);
    progress.startActivity("projects");
    ArrayList<Trio<Integer, String, String>> filteredProjects = processProjectBag(transaction, projects);
    progress.setDone();
    return filteredProjects;
  }

  public static void filterProjects(ArrayList<Trio<Integer, String, String>> allProjects, @Nullable Set<Integer> filter) {
    if (filter == null) return;
    for (Iterator<Trio<Integer, String, String>> it = allProjects.iterator(); it.hasNext(); ) {
      Trio<Integer, String, String> project = it.next();
      if (!filter.contains(project.getFirst())) it.remove();
    }
  }

  private static ArrayList<Trio<Integer, String, String>> processProjectBag(EntityTransaction transaction, List<JSONObject> projects) {
    ArrayList<Trio<Integer, String, String>> result = Collections15.arrayList();
    StoreIterator it = new StoreIterator(transaction, ServerProject.TYPE, ServerProject.ID, JRProject.ID, projects);
    it.managerDeleteBag();
    Pair<JSONObject, EntityHolder> pair;
    while ((pair = it.next()) != null) {
      JSONObject prjObject = pair.getFirst();
      String key = JRProject.KEY.getValue(prjObject);
      String name = JRProject.NAME.getValue(prjObject);
      result.add(Trio.create(it.getLastId(), key, name));
      EntityHolder holder = pair.getSecond();
      holder.setNNValue(ServerProject.KEY, key);
      holder.setNNValue(ServerProject.NAME, name);
    }
    return result;
  }

  public void loadFullProjects(RestSession session, List<Trio<Integer, String, String>> prjIdKeyName, ProgressInfo progress) throws ConnectorException {
    progress.startActivity("projects");
    ProgressInfo[] progresses = progress.split(prjIdKeyName.size());
    for (int i = 0, resultSize = prjIdKeyName.size(); i < resultSize; i++) {
      Trio<Integer, String, String> project = prjIdKeyName.get(i);
      ProgressInfo stepProgress = progresses[i];
      String prjName = project.getThird();
      stepProgress.startActivity(prjName);
      String key = project.getSecond();
      myProjectsAndTypes.addProject(project.getFirst(), key, prjName);
      JSONObject fullProject = RestOperations.projectFull(session, key);
      if (fullProject == null) {
        LogHelper.warning("Failed to load project", key);
        continue;
      }
      storeProject(fullProject);
      stepProgress.setDone();
    }
    if (myProcessTypes) postProcessTypes();
    progress.setDone();
  }

  /**
   * Collects type bag<br>
   * Create types linear order<br>
   * Collect only-in-project value for each type
   */
  private void postProcessTypes() {
    EntityBag2 types = myTransaction.addBag(ServerIssueType.TYPE);
    types.delete();
    for (Object obj : myTypesById.getValues()) {
      EntityHolder type = (EntityHolder) obj;
      types.exclude(type);
    }
    IntList typeOrder = collectLinearOrder();
    for (int i = 0; i < typeOrder.size(); i++) {
      long typeId = typeOrder.get(i);
      EntityHolder type = myTypesById.get(typeId);
      if (type != null) type.setValue(ServerIssueType.ORDER, i);
    }
    // Collect only-in-projects for all types
    for (IntIterator cursor : typeOrder) {
      int typeId = cursor.value();
      ArrayList<EntityHolder> projects = Collections15.arrayList();
      for (Pair<EntityHolder, IntList> pair : myTypeIdsInProject) if (pair.getSecond().contains(typeId)) projects.add(pair.getFirst());
      EntityHolder type = myTypesById.get(typeId);
      if (type != null) type.setReferenceCollection(ServerIssueType.ONLY_IN_PROJECTS, projects);
      else LogHelper.error("Missing type", typeId);
    }
  }

  private IntList collectLinearOrder() {
    Iterator<IntList> it = ConvertingList.create(myTypeIdsInProject, Pair.<IntList>convertorGetSecond()).iterator();
    IntArray typeOrder = new IntArray();
    if (!it.hasNext()) return typeOrder;
    typeOrder.addAll(it.next());
    while (it.hasNext()) {
      IntList partialOrder = it.next();
      int insertIndex = 0;
      for (IntIterator cursor : partialOrder) {
        int typeId = cursor.value();
        int index = typeOrder.indexOf(typeId);
        if (index >= 0) insertIndex = index + 1;
        else {
          typeOrder.insert(insertIndex, typeId);
          insertIndex++;
        }
      }
    }
    return typeOrder;
  }

  private void storeProject(JSONObject fullProject) {
    StoreIterator it = new StoreIterator(myTransaction, ServerProject.TYPE, ServerProject.ID, JRProject.ID, Collections.singleton(fullProject));
    Pair<JSONObject, EntityHolder> prjPair = it.next();
    int prjId = it.getLastId();
    EntityHolder project = prjPair != null ? prjPair.getSecond() : null;
    if (project == null) return;
    project.setNNValue(ServerProject.KEY, JRProject.KEY.getValue(fullProject));
    project.setNNValue(ServerProject.NAME, JRProject.NAME.getValue(fullProject));
    project.setNNValue(ServerProject.DESCRIPTION, JRProject.DESCRIPTION.getValue(fullProject));
    JSONObject lead = JRProject.LEAD.getValue(fullProject);
    if (lead == null) project.setValue(ServerProject.LEAD, null);
    else project.setNNReference(ServerProject.LEAD, ServerUser.fromJson(myTransaction, lead));
    IntList issueTypeIds = collectIssueTypes(JRProject.ISSUE_TYPES.list(fullProject));
    myProjectsAndTypes.setProjectTypes(prjId, issueTypeIds);
    int insIndex = myTypeIdsInProject.size();
    for (int i = 0, myTypeIdsInProjectSize = myTypeIdsInProject.size(); i < myTypeIdsInProjectSize; i++) {
      Pair<EntityHolder, IntList> pair = myTypeIdsInProject.get(i);
      Integer id = pair.getFirst().getScalarValue(ServerProject.ID);
      if (id != null && id <= prjId) {
        LogHelper.assertError(id != prjId, id);
        insIndex = i;
      }
    }
    myTypeIdsInProject.add(insIndex, Pair.create(project, issueTypeIds));
    storeComponents(project, JRProject.COMPONENTS.list(fullProject));
    storeVersions(project, JRProject.VERSIONS.list(fullProject));
    storeRoles(JRProject.ROLES.getValue(fullProject));
  }

  private void storeRoles(JSONObject roles) {
    if (roles == null) return;
    for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) roles).entrySet()) {
      String name = Util.castNullable(String.class, entry.getKey());
      Integer id = JRProject.EXTRACT_ROLE_ID.convert(Util.castNullable(String.class, entry.getValue()));
      if (name  == null || id == null) {
        LogHelper.warning("Failed to parse project role", entry, name, id);
        continue;
      }
      EntityHolder role = myTransaction.addEntity(ServerProjectRole.TYPE, ServerProjectRole.ID, id);
      if (role == null) LogHelper.error("Failed to store role", id, name);
      else role.setValue(ServerProjectRole.NAME, name);
    }
  }

  private void storeVersions(EntityHolder project, List<JSONObject> versions) {
    SubProjectIterator it = new SubProjectIterator(ServerVersion.TYPE, ServerVersion.PROJECT, project, ServerVersion.ID, JRVersion.ID, versions);
    it.managerDeleteBag();
    Pair<JSONObject, EntityHolder> pair;
    while ((pair = it.next()) != null) {
      JSONObject version = pair.getFirst();
      EntityHolder holder = pair.getSecond();
      holder.setNNValue(ServerVersion.NAME, JRVersion.NAME.getValue(version));
      holder.setNNValue(ServerVersion.ARCHIVED, JRVersion.ARCHIVED.getValue(version));
      holder.setNNValue(ServerVersion.RELEASED, JRVersion.RELEASED.getValue(version));
      holder.setValue(ServerVersion.RELEASE_DATE, JRVersion.RELEASED_DATE.getValue(version));
      holder.setValue(ServerVersion.SEQUENCE, it.getIndex());
    }
  }

  private void storeComponents(EntityHolder project, List<JSONObject> components) {
    SubProjectIterator it = new SubProjectIterator(ServerComponent.TYPE, ServerComponent.PROJECT, project, ServerComponent.ID, JRComponent.ID, components);
    it.managerDeleteBag();
    Pair<JSONObject, EntityHolder> pair;
    while ((pair = it.next()) != null) {
      JSONObject component = pair.getFirst();
      EntityHolder holder = pair.getSecond();
      holder.setNNValue(ServerComponent.NAME, JRComponent.NAME.getValue(component));
    }
  }

  private IntList collectIssueTypes(List<JSONObject> issueTypes) {
    StoreIterator it = new StoreIterator(myTransaction, ServerIssueType.TYPE, ServerIssueType.ID, JRIssueType.ID, issueTypes);
    IntArray result = new IntArray();
    Pair<JSONObject, EntityHolder> pair;
    while ((pair = it.next()) != null) {
      int typeId = it.getLastId();
      result.add(typeId);
      if (myProjectsAndTypes.isKnownType(typeId)) continue;
//      if (myTypesById.containsKey(typeId)) continue;
      EntityHolder holder = pair.getSecond();
      myTypesById.put(typeId, holder);
      JSONObject type = pair.getFirst();
      holder.setNNValue(ServerIssueType.NAME, JRIssueType.NAME.getValue(type));
      holder.setNNValue(ServerIssueType.DESCRIPTION, JRIssueType.DESCRIPTION.getValue(type));
      holder.setNNValue(ServerIssueType.ICON_URL, JRIssueType.ICON.getValue(type));
      holder.setNNValue(ServerIssueType.SUBTASK, JRIssueType.SUBTASK.getValue(type));
    }
    return result;
  }

  private static class SubProjectIterator extends StoreIterator {
    private final EntityKey<Entity> myProjectKey;
    private final EntityHolder myProject;

    private SubProjectIterator(Entity type, EntityKey<Entity> projectKey, EntityHolder project, EntityKey<Integer> idKey, JSONKey<Integer> idAccessor,
      Iterable<JSONObject> source)
    {
      super(project.getTransaction(), type, idKey, idAccessor, source);
      myProjectKey = projectKey;
      myProject = project;
    }

    @Override
    protected EntityHolder createEntity(int id, JSONObject source) {
      EntityHolder holder = myProject.getTransaction().addEntity(myType, myProjectKey, myProject, myIdKey, id);
      LogHelper.assertError(holder != null, "Failed to store", myType, id, source);
      return holder;
    }

    @Override
    protected EntityBag2 createBag() {
      return myProject.getTransaction().addBagRef(myType, myProjectKey, myProject);
    }
  }

}
