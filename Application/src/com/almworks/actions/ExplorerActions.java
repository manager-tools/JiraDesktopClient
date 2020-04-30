package com.almworks.actions;

import com.almworks.actions.console.actionsource.ActionGroup;
import com.almworks.actions.console.actionsource.ConsoleActionsComponent;
import com.almworks.actions.distribution.CreateDistribution;
import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.LifeMode;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.qb.QueryBuilderComponent;
import com.almworks.api.application.tree.*;
import com.almworks.api.application.viewer.CommentsController;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.QueryUrlInfo;
import com.almworks.api.explorer.TableController;
import com.almworks.api.gui.MainMenu;
import com.almworks.explorer.ExplorerComponentImpl;
import com.almworks.explorer.ShowHideDistributionTableAction;
import com.almworks.explorer.ShowHideNavigationAreaAction;
import com.almworks.explorer.qbuilder.filter.BinaryCommutative;
import com.almworks.explorer.tree.ExcludeDisributionNodeAction;
import com.almworks.explorer.tree.NavigationTree;
import com.almworks.explorer.tree.OutboxNode;
import com.almworks.explorer.tree.TemporaryQueriesNode;
import com.almworks.integers.LongList;
import com.almworks.sysprop.SystemPropertiesAction;
import com.almworks.tags.AddRemoveFavoritesAction;
import com.almworks.tags.ImportTagsAction;
import com.almworks.tags.NewTagAction;
import com.almworks.tags.TagItemsAction;
import com.almworks.util.*;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Procedure;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.TreeModelAdapter;
import com.almworks.util.components.tabs.TabActions;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class ExplorerActions implements Startable {
  private final ActionRegistry myRegistry;
  private final ConsoleActionsComponent myActionConsole;
  private final Engine myEngine;

  public ExplorerActions(ActionRegistry registry, Engine engine, ConsoleActionsComponent actionConsole) {
    myRegistry = registry;
    myActionConsole = actionConsole;
    myEngine = engine;
  }

  public void start() {
//    myRegistry.registerAction(MainMenu.Edit.EDIT_ITEM, new EditPrimaryItemAction());  replaced in JC3
    myRegistry.registerAction(MainMenu.Edit.VIEW_PROBLEMS, new ShowItemProblemAction());
    myRegistry.registerAction(MainMenu.Edit.DOWNLOAD, new DownloadItemAction(myEngine));
    myRegistry.registerAction(MainMenu.Edit.UPLOAD, new UploadItemAction());
    myRegistry.registerAction(MainMenu.Edit.DISCARD, new DiscardLocalChangesAction());
    myRegistry.registerAction(MainMenu.Edit.RENAME, new RenameAction());
    myRegistry.registerAction(MainMenu.Edit.NEW_FOLDER, createCreateFolderAction());
    myRegistry.registerAction(MainMenu.Edit.ADD_TO_FAVORITES, new AddRemoveFavoritesAction(true));
    myRegistry.registerAction(MainMenu.Edit.REMOVE_FROM_FAVORITES, new AddRemoveFavoritesAction(false));
    myRegistry.registerAction(MainMenu.Edit.TAG, new TagItemsAction());
    myRegistry.registerAction(MainMenu.Edit.NEW_TAG, new NewTagAction(false));
    myRegistry.registerAction(MainMenu.Edit.COPY_ID_SUMMARY, new CopyIdAndSummaryAction());

    myRegistry.registerAction(MainMenu.Search.NEW_QUERY, createCreateNewQueryAction());
    myRegistry.registerAction(MainMenu.Search.NEW_DISTRIBUTION, CreateDistribution.createAction());
    myRegistry.registerAction(MainMenu.Search.EXCLUDE_FROM_DISTRIBUTION, new ExcludeDisributionNodeAction());
    myRegistry.registerAction(MainMenu.Search.EDIT_QUERY, new EditQueryAction());
    myRegistry.registerAction(MainMenu.Search.RUN_QUERY, new RunQueryAction());
    myRegistry.registerAction(MainMenu.Search.RUN_LOCALLY, new ShowCachedResultAction());
    myRegistry.registerAction(MainMenu.Search.RELOAD_QUERY, new ReloadQueryFromServerAction());
    myRegistry.registerAction(MainMenu.Search.RUN_QUERY_IN_BROWSER, new OpenQueryInBrowserAction());
    myRegistry.registerAction(MainMenu.Search.STOP_QUERY, new StopQueryAction());
    myRegistry.registerAction(MainMenu.Search.CLOSE_CURRENT_TAB, TabActions.createCloseTabAction(
      ExplorerComponentImpl.MAIN_TABS_MANAGER, "Close Current Ta&b", MainMenu.Search.CLOSE_CURRENT_TAB));
    myRegistry.registerAction(MainMenu.Search.SELECT_NEXT_TAB, TabActions.createForwardTabAction(
      ExplorerComponentImpl.MAIN_TABS_MANAGER, "Select Next Tab", MainMenu.Search.SELECT_NEXT_TAB));
    myRegistry.registerAction(MainMenu.Search.SELECT_PREV_TAB, TabActions.createBackwardTabAction(
      ExplorerComponentImpl.MAIN_TABS_MANAGER, "Select Previous Tab", MainMenu.Search.SELECT_PREV_TAB));
    myRegistry.registerAction(MainMenu.Search.OPEN_ITEM_IN_BROWSER, new ViewItemInBrowserAction());

    myRegistry.registerAction(MainMenu.Search.OPEN_ITEM_IN_TAB, new OpenInNewTabAction());
    myRegistry.registerAction(MainMenu.Search.OPEN_ITEM_IN_FRAME, new OpenInNewFrameAction());
    myRegistry.registerAction(MainMenu.Edit.SORT_NODES, new SortNodesAction());

    myRegistry.registerAction(MainMenu.File.SHOW_CONNECTION_INFO, ShowConnectionInfo.MAIN);
    myRegistry.registerAction(MainMenu.File.EDIT_CONNECTION, EditConnectionAction.MAIN);
    myRegistry.registerAction(MainMenu.File.RETRY_INITIALIZATION, RetryInitializationAction.MAIN);
    myRegistry.registerAction(MainMenu.File.REMOVE_CONNECTION, RemoveConnectionsAction.create());
    myRegistry.registerAction(MainMenu.File.UPLOAD_ALL_CHANGES, new UploadLocalChangesAction());

    myRegistry.registerAction(MainMenu.Search.TOP_DUPLICATE_QUERY, new TemporaryDuplicateAction());
//    myRegistry.registerAction(MainMenu.Search.EXPAND_DISTRIBUTION, new ExpandLazyDistributionAction());
//    myRegistry.registerAction(MainMenu.Tools.VIEW_NOTE, new ViewNoteAction());

//    myRegistry.registerAction(MainMenu.Edit.DOWNLOAD_DETAILS, new DownloadDetailsAction());
//    myRegistry.registerAction(MainMenu.Edit.DOWNLOAD_ATTACHMENTS, new DownloadAttachmentsAction());

    myRegistry.registerAction(MainMenu.Windows.FOCUS_NAVIGATION_TREE,
      new FocusComponentRoleAction(NavigationTree.NAVIGATION_JTREE));
    myRegistry.registerAction(MainMenu.Windows.FOCUS_TABLE,
      new FocusComponentRoleAction(TableController.TABLE_COMPONENT));
    myRegistry.registerAction(MainMenu.Windows.FOCUS_ITEM_VIEWER,
      new FocusComponentRoleAction(TableController.ARTIFACT_VIWER_COMPONENT));
    CycleFocusComponentRoleAction cycleFocusAction = new CycleFocusComponentRoleAction(NavigationTree.NAVIGATION_JTREE,
      TableController.TABLE_COMPONENT, TableController.ARTIFACT_VIWER_COMPONENT);
    myRegistry.registerAction(MainMenu.Windows.CYCLE_FOCUS, cycleFocusAction);
    myRegistry.registerAction(MainMenu.Windows.CYCLE_FOCUS_BACK, cycleFocusAction.backwardAction());
    myRegistry.registerAction(MainMenu.Windows.SHOW_DISTRIBUTION_TABLE, new ShowHideDistributionTableAction());
    myRegistry.registerAction(MainMenu.Windows.SHOW_NAVIGATION_AREA, ShowHideNavigationAreaAction.INSTANCE);
    myRegistry.registerAction(TabActions.EXPAND_SHRINK_TABS_ID, ShowHideNavigationAreaAction.INSTANCE);

    myRegistry.registerAction(MainMenu.Tools.REMOVE_BAD_ISSUE, new RemovePrimaryItemAction(myEngine));
    myRegistry.registerAction(MainMenu.Tools.IMPORT_TAGS, new ImportTagsAction());
    myRegistry.registerAction(MainMenu.Tools.SYSTEM_PROPERTIES, new SystemPropertiesAction());

    myRegistry.registerAction(MainMenu.Search.FIND, new ShowHighlightPanelAction());

    if (Env.getBoolean(GlobalProperties.INTERNAL_ACTIONS)) {
      myRegistry.registerAction(MainMenu.Edit.VIEW_CHANGES, new ViewChangesAction());
      myRegistry.registerAction(MainMenu.Edit.VIEW_ATTRIBUTES, new ViewAttributesAction());
      myRegistry.registerAction(MainMenu.Tools.VIEW_ITEM_ATTRIBUTES, ViewAttributesAction.createViewItem());
      myRegistry.registerAction(MainMenu.Edit.VIEW_SHADOWS, new ViewShadowsAction());
      myRegistry.registerAction(MainMenu.Tools.OPERATION_CONSOLE, ConsoleActionsComponent.OPEN_CONSOLE);
    }

    registerHideEmptyQueriesActions(myRegistry);

    ToggleLiveModeAction.registerActions(myRegistry);
    RefreshArtifactsTableAction.registerActions(myRegistry);
//    CreateItemHelper.registerActions(myRegistry);
//    MergePrimaryItemAction.registerActions(myRegistry);
    CommentsController.registerActions(myRegistry);
//    PrimaryItemEditor.registerActions(myRegistry);
//    NewArtifactForm.registerActions(myRegistry);
//    EditorContent.registerActions(myRegistry);
    registerConsoleActions();
  }

  private void registerConsoleActions() {
    // Navigation tree/connection related actions
    myActionConsole.addGroup(new ActionGroup.InContext("Connection", GenericNode.NAVIGATION_NODE,
      MainMenu.File.EDIT_CONNECTION, MainMenu.File.SHOW_CONNECTION_INFO, MainMenu.File.RETRY_INITIALIZATION, MainMenu.File.REMOVE_CONNECTION,
      MainMenu.File.DOWNLOAD_CHANGES_QUICK_POPUP, MainMenu.File.RELOAD_CONFIGURATION_POPUP));
  }

  private void registerHideEmptyQueriesActions(ActionRegistry registry) {
    registry.registerAction(MainMenu.Search.HIDE_EMPTY_QUERIES_ON, new HideEmptyQueriesChooserAction("Hide",
        "Hide sub-queries that would show zero " + Terms.ref_artifacts, Boolean.TRUE));
    registry.registerAction(MainMenu.Search.HIDE_EMPTY_QUERIES_OFF, new HideEmptyQueriesChooserAction("Show",
        "Do not hide sub-queries that would show zero " + Terms.ref_artifacts, Boolean.FALSE));
  }

  private CreateChildNodeAction createCreateNewQueryAction() {
    CreateChildNodeAction action =
      CreateChildNodeAction.create(Icons.ACTION_CREATE_NEW_QUERY, TreeNodeFactory.NodeType.QUERY);
    action.setDefaultPresentation(PresentationKey.NAME, L.actionName("New &" + Terms.Query));
    action.setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Create a new " + Terms.query));
    return action;
  }

  private CreateChildNodeAction createCreateFolderAction() {
    CreateChildNodeAction action = CreateChildNodeAction.create(TreeNodeFactory.NodeType.FOLDER);
    action.setDefaultPresentation(PresentationKey.NAME, L.actionName("&New " + Terms.Folder));
    action.setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Create a new " + Terms.folder));
    return action;
  }

  public static <T extends GenericNode> T getSingleNavigationNode(ActionContext context, DataRole<T> role)
    throws CantPerformException
  {
    try {
      return context.getSourceObject(role);
    } catch (CantPerformException e) {
      GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      if (role.matches(node))
        return (T) node;
      throw new CantPerformException("No role: " + role + " object: " + node);
    }
  }


  public void stop() {
  }

  private static class HideEmptyQueriesChooserAction extends SimpleAction {
    private Boolean myValue;

    public HideEmptyQueriesChooserAction(String name, String description, Boolean value) {
      super(name);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, Local.parse(description));
      watchRole(GenericNode.NAVIGATION_NODE);
      myValue = value;
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      final GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      context.putPresentationProperty(PresentationKey.TOGGLED_ON, Boolean.valueOf(node.getHideEmptyChildren()).equals(myValue));
      context.putPresentationProperty(PresentationKey.ENABLE,
        node.canHideEmptyChildren() ? EnableState.ENABLED : EnableState.INVISIBLE);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      node.setHideEmptyChildren(myValue);
    }
  }


  private static class UploadLocalChangesAction extends SimpleAction {
    private boolean mySubscribed = false;
    private final BasicScalarModel<Boolean> myEnabled = BasicScalarModel.createModifiable(false);

    public UploadLocalChangesAction() {
      super(L.actionName("&Upload Changes"));
      watchModifiableRole(LocalChangesCounterComponent.ROLE);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Upload all local modifications to the server");
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
//      DBDataRoles.checkAnyConnectionHasCapability(context, Connection.Capability.EDIT_ITEM);

      if(!mySubscribed) {
        final LocalChangesCounterComponent changes = context.getSourceObject(LocalChangesCounterComponent.ROLE);
        context.updateOnChange(changes);
        final ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
        trySubscribeToTree(explorer);
      } else {
        context.updateOnChange(myEnabled);
      }

      context.setEnabled(myEnabled.getValue());
    }

    private void trySubscribeToTree(ExplorerComponent explorer) {
      if(mySubscribed) {
        return;
      }

      final RootNode rn = explorer.getRootNode();
      if(rn == null) {
        return;
      }

      final DefaultTreeModel tm = rn.getTreeNode().getTreeModel();
      if(tm == null) {
        return;
      }

      class Crawler extends Condition<GenericNode> implements Runnable {
        @Override
        public void run() {
          final List<GenericNode> outboxes = rn.collectNodes(this);
          boolean hasChanges = false;
          for(final GenericNode node : outboxes) {
            if(node.getPreviewCount(false) > 0) {
              hasChanges = true;
              break;
            }
          }
          myEnabled.setValue(hasChanges);
        }

        @Override
        public boolean isAccepted(GenericNode value) {
          return value instanceof OutboxNode;
        }
      }

      final Bottleneck bottleneck = new Bottleneck(300, ThreadGate.AWT, new Crawler());

      tm.addTreeModelListener(new TreeModelAdapter() {
        @Override
        protected void treeModelEvent(TreeModelEvent e) {
          bottleneck.request();
        }
      });

      mySubscribed = true;
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      final LocalChangesCounterComponent changes = context.getSourceObject(LocalChangesCounterComponent.ROLE);
      ThreadGate.LONG(this).execute(new Runnable() {
        public void run() {
          Map<Connection, LongList> changedArtifacts = changes.getChangedItems().waitForCompletion();
          for (Map.Entry<Connection, LongList> e : changedArtifacts.entrySet()) {
            e.getKey().uploadItems(e.getValue());
          }
        }
      });
    }
  }


  private static class EditQueryAction extends SimpleAction {
    public EditQueryAction() {
      super(L.actionName("&Edit " + Terms.Query), Icons.ACTION_EDIT_QUERY);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Edit selected query"));
      watchRole(GenericNode.NAVIGATION_NODE);
      watchRole(UserQueryNode.USER_QUERY_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(EnableState.INVISIBLE);
      UserQueryNode node = ExplorerActions.getSingleNavigationNode(context, UserQueryNode.USER_QUERY_NODE);
      context.getSourceObject(Engine.ROLE);
      context.getSourceObject(QueryBuilderComponent.ROLE);
      context.setEnabled(node.isNode());
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      UserQueryNode queryNode = getSingleNavigationNode(context, UserQueryNode.USER_QUERY_NODE);
      QueryBuilderComponent qb = context.getSourceObject(QueryBuilderComponent.ROLE);
      queryNode.openQueryEditor(L.dialog(Terms.Query + " Builder"), qb).showWindow();
    }
  }


  private static class TemporaryDuplicateAction extends SimpleAction {
    public TemporaryDuplicateAction() {
      super(L.actionName("Create &Top-Level Copy"), null);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
        L.tooltip("Combine this query and all parent queries in a single top-level query under Temporary Queries folder"));
      setDefaultPresentation(PresentationKey.SHORTCUT,
        KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
      watchRole(GenericNode.NAVIGATION_NODE);
      watchRole(QueryNode.QUERY_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(EnableState.INVISIBLE);
      QueryNode query = ExplorerActions.getSingleNavigationNode(context, QueryNode.QUERY_NODE);
      context.getSourceObject(TreeNodeFactory.TREE_NODE_FACTORY);
      GenericNode parent = query.getParent();
      if (parent == null || query.getAncestorOfType(ConnectionNode.class) == null)
        context.setEnabled(EnableState.INVISIBLE);
      else
        context.setEnabled(!(parent instanceof TemporaryQueriesNode));
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      QueryNode queryNode = getSingleNavigationNode(context, QueryNode.QUERY_NODE);
      if (queryNode == null) {
        assert false;
        return;
      }
      GenericNode parent = queryNode.getAncestorOfType(ConnectionNode.class);
      if (parent == null) {
        assert false;
        return;
      }
      GenericNode temp = ((ConnectionNode) parent).findTemporaryFolder();
      if (temp != null)
        parent = temp;

      List<FilterNode> constraints = Collections15.arrayList();
      QueryNode node = queryNode;
      StringBuffer name = new StringBuffer();
      while (node != null) {
        FilterNode filterNode = node.getFilterStructure();
        if (filterNode != null) {
          constraints.add(filterNode);
          if (name.length() > 0)
            name.insert(0, '*');
          name.insert(0, node.getPresentation().getText());
        }
        node = node.getStrictAncestorOfType(QueryNode.class);
      }
      int size = constraints.size();
      if (size == 0)
        throw new CantPerformExceptionExplained("Cannot duplicate invalid query");
      FilterNode filter;
      if (size == 1) {
        filter = constraints.get(0);
      } else {
        Collections.reverse(constraints);
        filter = new BinaryCommutative.And(constraints);
      }

      TreeNodeFactory nodeFactory = context.getSourceObject(TreeNodeFactory.TREE_NODE_FACTORY);
      UserQueryNode newQuery = nodeFactory.createUserQuery(parent);
      newQuery.setFilter(filter);
      newQuery.getPresentation().setText(name.toString());
      nodeFactory.selectNode(newQuery, true);
    }
  }


  private static class StopQueryAction extends SimpleAction {
    public StopQueryAction() {
      super(L.actionName("Stop " + Terms.Query), Icons.ACTION_STOP_QUERY);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
      setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.ESCAPE);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Cancel execution of a " + Terms.query);
      watchRole(TableController.DATA_ROLE);
      watchRole(LifeMode.LIFE_MODE_DATA);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      if (isArtifactViewer(context))
        FocusComponentRoleAction.focusComponentByRole(context, TableController.TABLE_COMPONENT);
      else
        context.getSourceObject(TableController.DATA_ROLE).stopLoading();
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      if (isArtifactViewer(context)) {
        context.setEnabled(EnableState.ENABLED);
        context.getSourceObject(TableController.TABLE_COMPONENT);
        return;
      }
      LifeMode mode = context.getSourceObject(LifeMode.LIFE_MODE_DATA);
      context.setEnabled(mode.isLoading() ? EnableState.ENABLED : EnableState.DISABLED);
    }

    private boolean isArtifactViewer(ActionContext context) {
      try {
        context.getSourceObject(TableController.ARTIFACT_VIEW_FOCUS_MARK);
        return true;
      } catch (CantPerformException e) {
        return false;
      }
    }
  }


  private static class ShowCachedResultAction extends SimpleAction {
    public ShowCachedResultAction() {
      super("Show &Cached " + Terms.Query + " Result", null);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
        L.tooltip("Execute query without requesting data from the remote server"));
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
      watchRole(GenericNode.NAVIGATION_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      QueryResult result = context.getSourceObject(GenericNode.NAVIGATION_NODE).getQueryResult();
      context.updateOnChange(result);
      if (!result.isRunnable())
        return;
      context.setEnabled(result.getRunLocallyProcedure() != null ? EnableState.ENABLED : EnableState.INVISIBLE);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      Procedure2<ExplorerComponent, GenericNode> procedure = node.getQueryResult().getRunLocallyProcedure();
      if (procedure != null)
        procedure.invoke(context.getSourceObject(ExplorerComponent.ROLE), node);
    }
  }


  private static class ReloadQueryFromServerAction extends SimpleAction {
    public ReloadQueryFromServerAction() {
      super("Reload " + Terms.Query + " from &Server", null);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
        L.tooltip("Request data from the remote server even if the query is synchronized"));
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
      watchRole(GenericNode.NAVIGATION_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      final GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      if(node.getConnection() == null) {
        context.setEnabled(EnableState.INVISIBLE);
        return;
      }

      final QueryResult result = node.getQueryResult();
      context.updateOnChange(result);
      if(!result.isRunnable()) {
        return;
      }

      context.setEnabled(result.getRunWithReloadProcedure() != null ? EnableState.ENABLED : EnableState.INVISIBLE);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      Procedure2<ExplorerComponent, GenericNode> procedure = node.getQueryResult().getRunWithReloadProcedure();
      if (procedure != null)
        procedure.invoke(context.getSourceObject(ExplorerComponent.ROLE), node);
    }
  }


  private static class OpenQueryInBrowserAction extends SimpleAction {
    public OpenQueryInBrowserAction() {
      super(L.actionName("Open " + Terms.Query + " in Bro&wser"), Icons.ACTION_OPEN_IN_BROWSER);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Open query in web browser"));
      KeyStroke key = Env.isMac() ?
        KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK) :
        KeyStroke.getKeyStroke(KeyEvent.VK_F9, KeyEvent.ALT_DOWN_MASK);
      setDefaultPresentation(PresentationKey.SHORTCUT, key);
      watchRole(GenericNode.NAVIGATION_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      QueryResult result = node.getQueryResult();
      if (!result.hasQueryUrl() || !result.isRunnable())
        return;
      context.getUpdateRequest().updateOnChange(result);
      context.setEnabled(result.canRunNow());
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
      QueryResult result = node.getQueryResult();
      result.getQueryURL(new Procedure<QueryUrlInfo>() {
        public void invoke(QueryUrlInfo info) {
          Threads.assertAWTThread();
          if (info == null) return;
          if (!info.isValid()) {
            String fatal = info.getFatalProblem();
            if (fatal != null) {
              JOptionPane.showMessageDialog(null, "<html>Cannot open query in browser.<br>" + fatal,
                "Open Query in Browser Failed", JOptionPane.ERROR_MESSAGE);
            }
          } else {
            List<String> urls = info.getQueryUrls();
            if (urls != null && urls.size() > 0) {
              String warning = info.getWarning();
              if (urls.size() > 1) {
                String w = "Would you like to open " + urls.size() + " browser windows?";
                warning = "<html>" + (warning == null ? w : warning + "<br>" + w);
              } else if (warning != null) {
                warning = "<html>" + warning + "<br>Would you like to continue?";
              }
              boolean proceed = warning == null;
              if (!proceed) {
                int r = JOptionPane.showConfirmDialog(null, new JLabel(warning), "Confirm Open Query in Browser",
                  JOptionPane.YES_NO_OPTION);
                proceed = r == JOptionPane.YES_OPTION;
              }
              if (proceed) {
                LogHelper.debug("Opening query urls", urls);
                for (String url : urls) {
                  ExternalBrowser.openURL(url, true);
                }
              }
            }
          }
        }
      });
    }
  }
}
