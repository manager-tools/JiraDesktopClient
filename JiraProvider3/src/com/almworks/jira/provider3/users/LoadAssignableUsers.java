package com.almworks.jira.provider3.users;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.sync.download2.meta.LoadRestMeta;
import com.almworks.jira.provider3.sync.download2.meta.ProjectsAndTypes;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.meta.core.MetaOperation;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.jira.provider3.sync.schema.ServerUser;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.sax.JSONCollector;
import com.almworks.restconnector.json.sax.PeekArrayElement;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class LoadAssignableUsers extends MetaOperation {
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(LoadRestMeta.class.getClassLoader(), "com/almworks/jira/provider3/users/message");
  private static final LocalizedAccessor.Value M_LOAD_PROGRESS = I18N.getFactory("progress.user.load.assignableUsers");
  private static final EntityKey<Collection<Entity>> ASSIGNABLE_USERS = EntityKey.entityCollection("user.assignableUsers", null);
  public static final DBAttribute<Set<Long>> A_ASSIGNABLE_USERS = ServerJira.toLinkSetAttribute(ASSIGNABLE_USERS);

  public LoadAssignableUsers() {
    super(5);
  }

  @Override
  public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException, JiraInternalException {
    if (session.getCredentials().isAnonymous()) return; // Assignable users are not accessible for anonymous
    ProjectsAndTypes projects = context.getDataOrNull(LoadRestMeta.PROJECTS);
    if (projects == null) {
      LogHelper.warning("Skipping assignable users: no project meta loaded");
      return;
    }
    progress.startActivity(M_LOAD_PROGRESS.create());
    ProgressInfo[] infos = progress.split(projects.getProjectCount());
    for (int i = 0; i < projects.getProjectCount(); i++) {
      infos[i].startActivity(projects.getDisplayableProjectNameAt(i));
      String projectKey = projects.getProjectKeyAt(i);
      if (projectKey == null) {
        LogHelper.error("Skipping not loaded project", projects.getProjectIdAt(i));
        continue;
      }
      ArrayList<EntityHolder> users = loadAssignableUsers(session, projectKey, transaction);
      EntityHolder project = ServerProject.project(transaction, projects.getProjectIdAt(i));
      if (project != null) project.setReferenceCollection(ASSIGNABLE_USERS, users);
      infos[i].setDone();
    }
  }

  @Nullable
  private ArrayList<EntityHolder> loadAssignableUsers(RestSession session, @NotNull String projectKey, EntityTransaction transaction) {
    try {
      RestResponse response = session.restGet("api/2/user/assignable/search?project=" + projectKey, RequestPolicy.SAFE_TO_RETRY);
      int code = response.getStatusCode();
      if (code / 100 == 4) {
        if (code == 401) LogHelper.debug("Assignable users are not allowed");
        else LogHelper.error("Failed to query assignable users", code);
        return null;
      }
      response.ensureSuccessful();
      ServerUser.CollectFromJson users = new ServerUser.CollectFromJson(transaction);
      response.parseJSON(new PeekArrayElement(JSONCollector.objectConsumer(users)).getUpLink());
      ArrayList<EntityHolder> usersList = users.getUsers();
      LogHelper.debug("Assignable users loaded", projectKey, usersList.size());
      return usersList;
    } catch (ConnectorException e) {
      LogHelper.warning("Error loading assignable users", projectKey);
    }
    return null;
  }
}
