package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.integers.IntList;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.app.sync.BaseOperation;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.jira.provider3.issue.features.edit.screens.LoadScreensOperation;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadedMetaKey;
import com.almworks.jira.provider3.sync.download2.meta.core.MetaOperation;
import com.almworks.jira.provider3.sync.download2.process.util.EntityDBUpdate;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.schema.ServerGroup;
import com.almworks.jira.provider3.sync.schema.ServerProjectRole;
import com.almworks.jira.provider3.users.LoadAssignableUsers;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;

import java.util.List;
import java.util.Set;

public class LoadRestMeta extends BaseOperation {
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(LoadRestMeta.class.getClassLoader(), "com/almworks/jira/provider3/sync/download2/meta/message");
  /**
   * Nullable. Null means no filter - synchronize with all projects
   */
  public static final LoadedMetaKey<Set<Integer>> PROJECT_FILTER = LoadedMetaKey.createMetaKey("projectFilter", null);
  public static final LoadedMetaKey<ProjectsAndTypes> PROJECTS = LoadedMetaKey.createMetaKey("projectsTypes", I18N.getFactory("download.meta.projects.noProjects"));

  private final ServerInfo myServerInfo;
  private final RemoteMetaConfig myMetaConfig;
  private final List<MetaOperation> myOperations = Collections15.arrayList();
  /**
   * Set by {@link com.almworks.jira.provider3.sync.download2.meta.LoadProjects}
   */
  private final LoadMetaContext myContext = new LoadMetaContext();
  private final FieldCollector myFieldCollector;

  public LoadRestMeta(ServerInfo serverInfo, ProgressInfo progress, RemoteMetaConfig metaConfig) {
    super(progress);
    CustomFieldsComponent customFieldsComponent = serverInfo.getConnection().getCustomFields();
    myFieldCollector = new FieldCollector(customFieldsComponent);
    myServerInfo = serverInfo;
    myMetaConfig = metaConfig;
    myContext.putLoadedData(PROJECT_FILTER, myServerInfo.getConnection().getProjectsFilter());
    myOperations.add(new MetaOperation(2) {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException {
        loadServerInfo(session, transaction, progress);
      }
    });
    myOperations.add(new MetaOperation(5) {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException {
        ProjectsAndTypes projectsAndTypes;
        try {
          projectsAndTypes = LoadProjects.perform(session, transaction, progress, context);
        } catch (CancelledException e) {
          throw e;
        } catch (ConnectorException e) {
          LogHelper.warning("Failed to load projects", e);
          return;
        }
        context.putLoadedData(PROJECTS, projectsAndTypes);
      }
    });
    myOperations.add(new MetaOperation(3) {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException {
        LoadGenericStatics.perform(session, transaction, progress);
      }
    });
    myOperations.add(new LoadCustomFields(customFieldsComponent));
    myOperations.add(new MetaOperation(2) {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException {
        try {
          loadCommentsVisibility(session, transaction, progress);
        } catch (CancelledException e) {
          throw e;
        } catch (ConnectorException e) {
          LogHelper.warning("Failed to load comments visibility", e);
        }
      }
    });
    myOperations.add(new LoadCreateMeta(myFieldCollector));
    myOperations.add(new LoadLabels());
    myOperations.add(new LoadEditMeta(myFieldCollector));
    myOperations.add(new MetaOperation(4) {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws JiraInternalException, CancelledException {
        IssuePermissions permissions = myServerInfo.getConnection().getActor(IssuePermissions.ROLE);
        if (permissions == null) return;
        IntList projectIds = context.getData(PROJECTS).getProjectIds();
        permissions.loadGlobalPermissions(session, transaction, progress, projectIds);
      }
    });
    myOperations.add(new LoadAssignableUsers());
    myOperations.add(new LoadScreensOperation());
    myOperations.add(new MetaOperation(2) {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws CancelledException, JiraInternalException {
        LoadLinkTypes.perform(session, transaction);
      }
    });
  }

  @Override
  public void perform(RestSession session) throws CancelledException {
    myProgress.startActivity("Loading Jira configuration");
    try {
      myServerInfo.ensureHasData(session);
    } catch (ConnectorException e) {
      LogHelper.warning("Failed to ensure server info", e);
    }
    EntityTransaction transaction = myServerInfo.createTransaction();
    double[] ratios = new double[myOperations.size()];
    for (int i = 0; i < myOperations.size(); i++) ratios[i] = myOperations.get(i).getEstimate();
    ProgressInfo[] progresses = myProgress.splitRatio(ratios);
    for (int i = 0; i < myOperations.size(); i++) {
      myProgress.checkCancelled();
      try {
        myOperations.get(i).perform(session, transaction, progresses[i], myContext);
      } catch (JiraInternalException e) {
        LogHelper.warning(e); // continue to load other metaInfo
      } catch (RuntimeException e) {
        LogHelper.error(e); // continue to load other metaInfo
      } finally {
        progresses[i].setDone();
      }
    }
    myProgress.checkCancelled();
    myFieldCollector.postProcess(transaction, true);
    EntityDBUpdate update = new EntityDBUpdate(transaction, myMetaConfig);
    myServerInfo.getSyncManager().writeDownloaded(update).waitForCompletion();
  }

  private void loadCommentsVisibility(RestSession session, EntityTransaction transaction, ProgressInfo progress) throws ConnectorException {
    List<String> groups = LoadCommentVisibility.loadCommentVisibilityGroups(session);
    progress.setDone();
    if (groups == null) return;
    EntityHolder connection = ServerInfo.changeConnection(transaction);
    connection.setValue(ServerProjectRole.PROJECT_ROLES_ONLY, groups.isEmpty());
    for (String group : groups) transaction.addEntity(ServerGroup.TYPE, ServerGroup.ID, group);
  }

  private void loadServerInfo(RestSession session, EntityTransaction transaction, ProgressInfo progress) throws CancelledException {
    progress.startActivity("server info");
    try {
      ServerInfo.load(session, transaction);
    } catch (CancelledException e) {
      throw e;
    } catch (ConnectorException e) {
      LogHelper.warning("Failed to load serverinfo", e);
    }
    progress.setDone();
  }
}
