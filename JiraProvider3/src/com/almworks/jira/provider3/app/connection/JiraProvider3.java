package com.almworks.jira.provider3.app.connection;

import com.almworks.actions.console.actionsource.ConsoleActionsComponent;
import com.almworks.api.application.MetaInfo;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.engine.*;
import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpUtils;
import com.almworks.api.license.LicensedFeatures;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.search.TextSearch;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.api.Database;
import com.almworks.items.gui.edit.engineactions.NewItemAction;
import com.almworks.items.gui.edit.merge.MergeTableEditor;
import com.almworks.items.gui.edit.visibility.FieldVisibilityController;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.connector2.JiraEnv;
import com.almworks.jira.provider3.app.connection.setup.JiraConnectionWizard;
import com.almworks.jira.provider3.attachments.upload.PrepareAttachmentsUpload;
import com.almworks.jira.provider3.comments.PrepareCommentUpload;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.workflow.WFActionsLoader2;
import com.almworks.jira.provider3.gui.textsearch.SearchByIssueKeys;
import com.almworks.jira.provider3.gui.textsearch.SearchByIssueNumbers;
import com.almworks.jira.provider3.gui.timetrack.JiraTimeTrackingDescriptor;
import com.almworks.jira.provider3.links.upload.PrepareLinkUpload;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.schema.*;
import com.almworks.jira.provider3.services.IssueUrl;
import com.almworks.jira.provider3.services.upload.JiraUploadComponent;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.worklogs.PrepareWorklogsUpload;
import com.almworks.platform.DiagnosticRecorder;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.login.JiraLoginInfo;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.spi.provider.AbstractItemProvider;
import com.almworks.spi.provider.wizard.ConnectionWizard;
import com.almworks.util.English;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.MapMedium;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.Local;
import com.almworks.util.i18n.LocalTextProvider;
import com.almworks.util.i18n.ResourceBundleTextProvider;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.ui.actions.ActionRegistry;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;
import org.picocontainer.defaults.PicoInvocationTargetInitializationException;

import java.net.MalformedURLException;
import java.net.URL;

public class JiraProvider3 extends AbstractItemProvider implements Startable {
  public static final Role<JiraProvider3> ROLE = Role.role("jira", JiraProvider3.class);

  private final MutableComponentContainer myContainer;
  private final JiraMetaInfo3 myMetaInfo;
  private final GuiFeaturesManager myFeatures;
  private final ActionRegistry myActions;
  private final WFActionsLoader2 myWorkflowActions;
  private final SyncManager mySyncManager;
  private final WorkArea myWorkArea;
  private final DiagnosticRecorder myDiagnosticRecorder;
  private final ConsoleActionsComponent myConsoleActions;
  private final JiraUploadComponent myUploadComponent;
  private final JiraSchemaInit mySchemaInit;

  public JiraProvider3(ComponentContainer container, Configuration config, GuiFeaturesManager features, TextDecoratorRegistry decorators,
    ActionRegistry actions, SyncManager syncMan, WorkArea workArea, DiagnosticRecorder diagnosticRecorder, ConsoleActionsComponent consoleActions,
    JiraUploadComponent uploadComponent, JiraSchemaInit schemaInit)
  {
    super(config.getOrCreateSubset("connections"));
    myFeatures = features;
    myActions = actions;
    mySyncManager = syncMan;
    myWorkArea = workArea;
    myDiagnosticRecorder = diagnosticRecorder;
    myConsoleActions = consoleActions;
    myUploadComponent = uploadComponent;
    mySchemaInit = schemaInit;

    myContainer = container.createSubcontainer(getProviderID());
    Local.getBook().installProvider(
      new ResourceBundleTextProvider("com.almworks.rc.jira.Jira", LocalTextProvider.Weight.DEFAULT,
        getClass().getClassLoader()));
    myWorkflowActions = new WFActionsLoader2(myFeatures.getImage());
    myMetaInfo = JiraMetaInfo3.create(config.createSubset("metaInfo"), decorators, this);
  }

  public <T> T getActor(TypedKey<T> role) {
    return myContainer.getActor(role);
  }

  public WFActionsLoader2 getWorkflowActions() {
    return myWorkflowActions;
  }

  public GuiFeaturesManager getFeaturesManager() {
    return myFeatures;
  }
  @Override
  public final void start() {
    if (!myState.commitValue(ItemProviderState.NOT_STARTED, ItemProviderState.STARTING)) return;
    registerTriggers();
    myWorkflowActions.start(Lifespan.FOREVER);
    JiraEnv.installGlobalLogsDir(myWorkArea.getLogDir());
    JiraEnv.setDiagnosticRecorder(myDiagnosticRecorder);
    registerActions();
    MetaInfo.REGISTRY.registerMetaInfo(Issue.DB_TYPE, myMetaInfo);
    initDB();
    IssueReferenceParser.register(this);
    TextSearch textSearch = myContainer.getActor(TextSearch.ROLE);
    if (textSearch != null) {
      textSearch.addTextSearchType(Lifespan.FOREVER, new SearchByIssueKeys());
      textSearch.addTextSearchType(Lifespan.FOREVER, new SearchByIssueNumbers(myFeatures));
    }
    registerUploadFactories();
    myState.commitValue(ItemProviderState.STARTING, ItemProviderState.STARTED);
  }

  private void initDB() {
    mySyncManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        doInitDB(drain);
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    }).waitForCompletion();
  }

  private void doInitDB(DBDrain drain) {
    drain.changeItem(Issue.DB_TYPE).setValue(SyncAttributes.IS_PRIMARY_TYPE, true);
    ServerFields.initDB(drain);
  }

  @Override
  public final void stop() {
  }

  long getProviderItem() {
    return mySchemaInit.getProviderItem();
  }

  private void registerUploadFactories() {
    myUploadComponent.registerUploadFactory(Issue.DB_TYPE, PrepareIssueUpload.INSTANCE);
    myUploadComponent.registerUploadFactory(Link.DB_TYPE, PrepareLinkUpload.INSTANCE);
    myUploadComponent.registerUploadFactory(Comment.DB_TYPE, PrepareCommentUpload.INSTANCE);
    myUploadComponent.registerUploadFactory(Worklog.DB_TYPE, PrepareWorklogsUpload.INSTANCE);
    myUploadComponent.registerUploadFactory(Attachment.DB_TYPE, PrepareAttachmentsUpload.INSTANCE);
  }

  private void registerTriggers() {
    Database db = getActor(Database.ROLE);
    JiraTimeTrackingDescriptor.registerTrigger(db);
  }

  private void registerActions() {
    NewItemAction.registerActions(myActions);
    MergeTableEditor.registerActions(myActions);
    FieldVisibilityController.registerActions(myActions);
    JiraActions.registerActions(myActions);
    JiraActions.registerConsoleActions(myConsoleActions);
    myWorkflowActions.registerConsoleActions(myConsoleActions);
  }

  public boolean isItemUrl(String url) {
    return IssueUrl.getNormalizedBaseUrl(url) != null;
  }

  public Configuration createDefaultConfiguration(String itemUrl) {
    IssueUrl issueUrl = IssueUrl.parseUrl(itemUrl);
    if (issueUrl == null) return null;
    Configuration config = MapMedium.createConfig();
    try {
      if (!JiraConfiguration.setBaseUrl(config, issueUrl.getNormalizedBaseUrl())) return null;
    } catch (MalformedURLException e) {
      return null;
    }
    JiraConfiguration.setLoginPassword(config, JiraLoginInfo.ANONYMOUS);
    config.setSetting(CommonConfigurationConstants.CONNECTION_NAME, fetchHostname(JiraConfiguration.getBaseUrl(config)) + " (auto)");
    return config;
  }

  private String fetchHostname(String baseUrl) {
    int k = baseUrl.indexOf("://");
    return k < 0 ? baseUrl : baseUrl.substring(k + 3);
  }

  @Override
  protected ConnectionWizard createNewConnectionWizard() {
    return JiraConnectionWizard.forNewConnection(myContainer);
  }

  @Override
  protected ConnectionWizard createEditConnectionWizard(Connection connection) {
    if(!(connection instanceof JiraConnection3)) {
      assert false;
      return null;
    }
    return JiraConnectionWizard.forEditing(myContainer, (JiraConnection3)connection);
  }

  @Override
  public Connection createConnection(String connectionID, ReadonlyConfiguration configuration, boolean isNew)
    throws ConfigurationException {
    assert connectionID != null;
    MutableComponentContainer container = myContainer.createSubcontainer(connectionID);
    container.registerActor(MutableComponentContainer.ROLE, container);
    container.registerActor(Role.role("providerID"), connectionID);
    container.registerActor(Role.role("readonlyConfiguration"), configuration);
    container.registerActor(Role.role("isNew"), isNew);
    try {
      return container.instantiate(JiraConnection3.class);
    } catch (PicoInvocationTargetInitializationException e) {
      Throwable cause = e.getCause();
      if (cause instanceof ConfigurationException)
        throw (ConfigurationException) cause;
      throw e;
    }
  }

  @Override
  public String getProviderID() {
    return "jira";
  }

  @Override
  public String getProviderName() {
    return "Jira";
  }

  @Override
  public String getLicenseFeature() {
    return LicensedFeatures.FEATURE_JIRA;
  }

  @Override
  public ProviderActivationAgent createActivationAgent() {
    return new ProviderActivationAgent() {
      public String getActivationIntroText(int siteCount) {
        return "Please enter the addresses of your Jira " + English.getSingularOrPlural("server", siteCount) + ":";
      }

      @Nullable
      public URL normalizeUrl(String url) {
        try {
          String norm = HttpUtils.normalizeBaseUrl(url);
          return new URL(norm);
        } catch (MalformedURLException e) {
          Log.debug(e);
          return null;
        }
      }

      @Nullable
      @CanBlock
      public String isUrlAccessible(String useUrl, ScalarModel<Boolean> cancelFlag) {
        HttpMaterial material = Context.require(HttpMaterialFactory.ROLE).create(null, false, JiraProvider3.getUserAgent());
        RestSession session = RestSession.create(useUrl, JiraCredentials.ANONYMOUS, material, null, getActor(SSLProblemHandler.ROLE).getSNIErrorHandler());
        try {
          RestServerInfo.get(session);
          return null;
        } catch (ConnectorException e) {
          return e.getShortDescription();
        } finally {
          session.dispose();
        }
      }
    };
  }

  @Override
  public PrimaryItemStructure getPrimaryStructure() {
    return Issue.STRUCTURE;
  }

  @Override
  @Nullable
  public String getDisplayableItemIdFromUrl(String url) {
    IssueUrl issueUrl = IssueUrl.parseUrl(url);
    return issueUrl != null ? issueUrl.getKey() : null;
  }
}
