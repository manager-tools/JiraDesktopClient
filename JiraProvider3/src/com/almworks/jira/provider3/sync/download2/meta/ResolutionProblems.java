package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.process.util.EntityDBUpdate;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.schema.ServerComponent;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.jira.provider3.sync.schema.ServerProjectRole;
import com.almworks.jira.provider3.sync.schema.ServerVersion;
import com.almworks.restconnector.RestSession;
import com.almworks.spi.provider.CancelFlag;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.Trio;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Solves entity-resolution problems for Projects, Components, Versions, Project Roles<br>
 * Loads brief projects to resolve Project problem<br>
 * Loads full corresponding projects to resolve Component or Version problem<br>
 * Load full all configured project to resolve Project Role problem
 * @see com.almworks.items.entities.api.collector.transaction.write.EntityWriter#getUncreatable()
 */
public class ResolutionProblems {
  private final RestSession mySession;
  private final ServerInfo myServerInfo;
  private final CancelFlag myCancelFlag;
  private final RemoteMetaConfig myMetaConfig;
  private final List<EntityHolder> myProblems = Collections15.arrayList();
  /**
   * Not null means has to load all projects. true - full load, false - brief only
   */
  private Boolean myAllProjectsFull = null;
  /**
   * Projects to load full, identified by ID or KEY
   */
  private final List<Pair<Integer, String>> myFullProjects = Collections15.arrayList();

  public ResolutionProblems(RestSession session, ServerInfo serverInfo, RemoteMetaConfig metaConfig) {
    mySession = session;
    myServerInfo = serverInfo;
    myCancelFlag = new CancelFlag();
    myMetaConfig = metaConfig;
  }

  public void addAll(Collection<EntityHolder> problems) {
    myProblems.addAll(problems);
  }

  public void resolve() throws ConnectorException {
    prepare();
    if (myAllProjectsFull == null && myFullProjects.isEmpty()) {
      LogHelper.error("No problem detected");
      return;
    }
    EntityTransaction transaction = myServerInfo.createTransaction();
    if (Boolean.TRUE.equals(myAllProjectsFull)) {
      ArrayList<Trio<Integer,String,String>> projects =
        LoadProjects.loadBriefProjects(mySession, transaction, ProgressInfo.createDeaf(myCancelFlag));
      LoadProjects.filterProjects(projects, myServerInfo.getConnection().getProjectsFilter());
      new LoadProjects(transaction, true).loadFullProjects(mySession, projects, ProgressInfo.createDeaf(myCancelFlag));
    } else if (!myFullProjects.isEmpty()) {
      ArrayList<Trio<Integer,String,String>> projects = LoadProjects.loadBriefProjects(mySession, transaction, ProgressInfo.createDeaf(myCancelFlag));
      for (Iterator<Trio<Integer, String, String>> it = projects.iterator(); it.hasNext(); ) {
        Trio<Integer, String, String> project = it.next();
        if (!needsFull(project)) it.remove();
      }
      if (projects.isEmpty()) LogHelper.error("No project requires full load", myFullProjects);
      else new LoadProjects(transaction, false).loadFullProjects(mySession, projects, ProgressInfo.createDeaf(myCancelFlag));
    }
    EntityDBUpdate update = new EntityDBUpdate(transaction, myMetaConfig);
    myServerInfo.getSyncManager().writeDownloaded(update).waitForCompletion();
  }

  private boolean needsFull(Trio<Integer, String, String> project) {
    for (Pair<Integer, String> pair : myFullProjects) {
      Integer id = pair.getFirst();
      String key = pair.getSecond();
      if (id != null) {
        if (id.equals(project.getFirst())) return true;
        else continue;
      }
      if (key != null) {
        if (key.equals(project.getSecond())) return true;
        else continue;
      }
      LogHelper.error("Missing project identity", pair);
    }
    return false;
  }

  private void prepare() throws JiraInternalException {
    for (EntityHolder problem : myProblems) {
      if (Boolean.TRUE.equals(myAllProjectsFull)) break;
      Entity type = problem.getItemType();
      if (ServerProjectRole.TYPE.equals(type)) myAllProjectsFull = true;
      else if (ServerProject.TYPE.equals(type)) myAllProjectsFull = false;
      else {
        EntityKey<Entity> projectRef;
        if (ServerVersion.TYPE.equals(type)) projectRef = ServerVersion.PROJECT;
        else if (ServerComponent.TYPE.equals(type)) projectRef = ServerComponent.PROJECT;
        else {
          LogHelper.error("Unknown problem", type, type.getTypeId(), problem);
          throw failure();
        }
        EntityHolder project = problem.getReference(projectRef);
        if (project == null) {
          LogHelper.error("Missing project", problem, type);
          throw failure();
        }
        markLoadFull(project);
      }
    }
  }

  private void markLoadFull(@NotNull EntityHolder project) throws JiraInternalException {
    String key = project.getScalarValue(ServerProject.KEY);
    Integer id = project.getScalarValue(ServerProject.ID);
    if (id == null && key == null) {
      LogHelper.error("No project identity", key, id, project);
      throw failure();
    }
    for (Pair<Integer, String> idKey : myFullProjects) {
      if (id != null && id.equals(idKey.getFirst())) return;
      if (key != null && key.equals(idKey.getSecond())) return;
    }
    myFullProjects.add(Pair.create(id, key));
  }

  static JiraInternalException failure() {
    return new JiraInternalException("Can not store data in local database");
  }
}
