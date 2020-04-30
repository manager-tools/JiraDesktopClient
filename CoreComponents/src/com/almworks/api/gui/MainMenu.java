package com.almworks.api.gui;

/**
 * @author : Dyoma
 */
public interface MainMenu {
  String X_PUBLISH_TIME = "X.Publish.Time";

  interface File {
    String NEW_CONNECTION = "File.ShowWelcome";
    String RELOAD_CONFIGURATION = "File.DownloadChanges";
    String RELOAD_CONFIGURATION_POPUP = "File.DownloadChangesPopup";
    String DOWNLOAD_CHANGES_QUICK = "File.DownloadChangesQuick";
    String DOWNLOAD_CHANGES_QUICK_POPUP = "File.DownloadChangesQuickPopup";
    String UPLOAD_ALL_CHANGES = "File.UploadChanges";
    String SHOW_CONNECTION_INFO = "File.ShowConnectionInfo";
    String REMOVE_CONNECTION = "File.RemoveConnection";
    String EDIT_CONNECTION = "File.EditConnection";
    String RETRY_INITIALIZATION = "File.RetryInitialization";
    String EXIT = "File.Exit";
    String USE_SYSTEM_TRAY = "File.UseSystemTray";
    String CONNECTION_AUTO_SYNC = "File.ConnectionAutoSync";
    String RESET_LOGIN_FAILURE = "File.ResetLoginFailure";
  }

  interface Edit {
    String NEW_ITEM = "Edit.NewArtifact";
    String NEW_ITEM_HERE = "Edit.NewItemHere";
    String EDIT_ITEM = "Edit.EditArtifact";
    String CONFIGURE_FIELDS = "Edit.ConfigureFields";

    String DOWNLOAD = "Edit.Update";
    String UPLOAD = "Edit.Commit";
    String MERGE = "Edit.Merge";
    String DISCARD = "Edit.Discard";
    String VIEW_PROBLEMS = "Edit.ViewProblems";

    String NEW_FOLDER = "Edit.NewFolder";
    String RENAME = "Edit.Rename";
    String SORT_NODES = "Edit.SortNodes";

    String ADD_TO_FAVORITES = "Edit.AddToFavorites";
    String REMOVE_FROM_FAVORITES = "Edit.RemoveFromFavorites";
    String TAG = "Edit.Tag";
    String NEW_TAG = "Edit.NewTag";

//    String DOWNLOAD_DETAILS = "Download.Details";
    String DOWNLOAD_ATTACHMENTS = "Download.Attachments";
    String COPY_ID_SUMMARY = "Edit.CopyIdSummary";
    String CUSTOM_COPY = "Edit.CustomCopy";
    String DELETE_ATTACHMENT = "Edit.DeleteAttachment";

    String COMMENTS_SORT_OLDEST_FIRST = "Comments.SortOldestFirst";
    String COMMENTS_SORT_NEWEST_FIRST = "Comments.SortNewestFirst";
    String COMMENTS_SHOW_THREAD_TREE = "Comments.ShowThreadTree";
    String COMMENTS_EXPAND_ALL = "Comments.ExpandAll";
    String VIEW_CHANGES = "Edit.ViewItemChanges";
    String VIEW_ATTRIBUTES = "Edit.ViewAttributes";
    String VIEW_SHADOWS = "Edit.ViewShadows";
  }

  interface Search {
    String NEW_QUERY = "Search.NewQuery";
    String NEW_DISTRIBUTION = "Search.NewDistribution";
    String NEW_DISTRIBUTION_QUERIES = "Search.NewDistributionQueries";
    String EXCLUDE_FROM_DISTRIBUTION = "Search.ExcludeFromDistribution";
    String EDIT_QUERY = "Search.EditQuery";
    String TOP_DUPLICATE_QUERY = "Search.TopDuplicateQuery";

    String QUICK_SEARCH = "Search.QuickSearch";    

    String RUN_QUERY = "Search.RunQuery";
    String RUN_LOCALLY = "Search.RunQueryLocally";
    String RELOAD_QUERY = "Search.ReloadQuery";
    String RUN_QUERY_IN_BROWSER = "Search.RunQueryInBrowser";
    String FORCE_RELOAD_QUERY = "Search.ForceReloadQuery";
    String STOP_QUERY = "Search.StopQuery";
    String OPEN_ITEM_IN_BROWSER = "Search.OpenArtifactInBrowser";
    String OPEN_ITEM_IN_TAB = "Search.OpenArtifactInTab";
    String OPEN_ITEM_IN_FRAME = "Search.OpenArtifactInFrame";

    String KEEP_LIVE_RESULTS = "Search.KeepLiveResults";
    String REFRESH_RESULTS = "Search.RefreshResults";

    String HIDE_EMPTY_QUERIES_ON = "Search.HideEmptyQueries.On";
    String HIDE_EMPTY_QUERIES_OFF = "Search.HideEmptyQueries.Off";
    String HIDE_EMPTY_QUERIES_DEFAULT = "Search.HideEmptyQueries.Default";

//    String EXPAND_DISTRIBUTION = "Search.ExpandDistribution";
    String CLOSE_CURRENT_TAB = "Window.CloseTab";
    String SELECT_NEXT_TAB = "Window.NextTab";
    String SELECT_PREV_TAB = "Window.PrevTab";

    String FIND = "Table.Find";
    String CUSTOMIZE_HIERARCHY = "Search.CustomizeHierarchy";
  }

  interface Windows {
    String FOCUS_NAVIGATION_TREE = "Windows.focusNavigationTree";
    String FOCUS_TABLE = "Windows.focusTable";
    String FOCUS_ITEM_VIEWER = "Windows.focusArtifactViewer";
    String CYCLE_FOCUS = "Windows.cycleFocus";
    String CYCLE_FOCUS_BACK = "Windows.cycleFocusBack";
    String SHOW_DISTRIBUTION_TABLE = "Windows.showDistributionTable";
    String SHOW_NAVIGATION_AREA = "Windows.showNavigationArea";
    String TOGGLE_FULL_SCREEN = "Windows.toggleFullScreen";
  }

  interface Tools {
    String SHOW_SYNCHRONIZATION_WINDOW = "Tools.ShowSynchronizationWindow";
    String CONFIGURE_PROXY = "Tools.ConfigureProxy";
    String EXPORT = "Tools.Export";
    String QUICK_EXPORT = "Tools.ExportQuick";
    String REORDER_TABLE = "Tools.ReorderTable";

    String TIME_TRACKING = "Tools.TimeTracking";
    String TIME_TRACKING_START = "Tools.TimeTracking.Start";
    String TIME_TRACKING_PAUSE = "Tools.TimeTracking.Pause";
    String TIME_TRACKING_STOP = "Tools.TimeTracking.Stop";

    String TIME_TRACKING_START_WORK_ON_ISSUE = "Tools.TimeTracking.StartWork";
    String TIME_TRACKING_STOP_WORK_ON_ISSUE = "Tools.TimeTracking.StopWork";
    String TIME_TRACKING_PUBLISH = "Tools.TimeTracking.Publish";
    String TIME_TRACKING_OPTIONS = "Tools.TimeTracking.Options";

    String INTEGRATE_WITH_IDE = "Tools.IntegrateWithIDE";
    String EXTERNAL_SEARCH = "Tools.ExternalSearch";
    String WATCH_IN_IDEA = "Tools.WatchInIdea";

//    String VIEW_NOTE = "Tools.ViewNote";
    String DUMP_DB = "Tools.DumpDB";
    String VIEW_ITEM_ATTRIBUTES = "Tools.ViewItemAttributes";
    String OPERATION_CONSOLE = "Tools.OperationConsole";
    String EXPLORER_FIELDS = "Tools.ExploreFields";
    String CHECK_FOR_UPDATE = "Tools.CheckForUpdate";
    String EXCEPTION = "Tools.Exception";
    String DUMP_INDEXES = "Tools.DumpIndexes";
    String LOG_ERROR = "Tools.LogError";
    String EAT_MEMORY = "Tools.EatMemory";
    String DUPLICATE_PUBLIC_BUG = "Tools.DuplicatePublicBug";
    String CONNECTION_INTERRUPT = "Tools.ConnectionInterrupt";
    String CRASH_SQLITE = "Tools.CrashSQLite";

    String DROP_INDEXES = "Tools.DropIndexes";

    String REMOVE_BAD_ISSUE = "Tools.RemoveBadIssue";

    String SCREENSHOT = "Tools.Screenshot";

    String RECORD_DIAGNOSTICS = "Tools.RecordDiagnostics";

    String IMPORT_TAGS = "Tools.ImportTags";

    String SYSTEM_PROPERTIES = "Tools.SystemProperties";

    String SPELL_CHECKER_SETTINGS = "Tools.SpellCheckerSettings";
    String DUMP_THREADS = "Tools.dumpThreads";
  }

  interface Help {
    String ABOUT = "Help.About";
    String USER_MANUAL = "Help.UserManual";
    String WHATS_NEW = "Help.WhatsNew";
  }


  interface NewItem {
    String COMMIT = "NewArtifact.Commit";
    String SAVE_DRAFT = "NewArtifact.SaveDraft";
    String DISCARD = "NewArtifact.Discard";
  }

  interface WorkflowEditor {
    String COMMIT = "WorkflowEditor.Commit";
    String SAVE_DRAFT = "WorkflowEditor.SaveDraft";
    String DISCARD = "WorkflowEditor.Discard";
  }

  interface Attachments {
    String DOWNLOAD = "Attachments.Download";
    String DOWNLOAD_ALL = "Attachments.DownloadAll";
    String SAVE_AS = "Attachments.SaveAs";
    String DOWNLOAD_AND_VIEW = "Attachments.DownloadAndView";
    String DOWNLOAD_AND_VIEW_INTERNAL = "Attachments.DownloadAndViewInternal";
    String DOWNLOAD_AND_OPEN_EXTERNAL = "Attachments.DownloadAndOpenExternal";
    String COPY_FILE_URL = "Attachments.CopyFileUrl";
    String SAVE_ALL = "Attachments.SaveAll";
    String COPY_FILE_PATH = "Attachments.CopyFilePath";

    String OPEN_WITH = "Attachments.OpenWith";
    String OPEN_FOLDER = "Attachments.OpenFolder";
    String OPEN_EXTERNAL = "Attachments.OpenExternal";
  }

  interface RecentItems {
    String SHOW_RECENT_ITEMS = "RecentArtifacts.ShowRecentArtifacts";
    String COPY_ITEM = "RecentArtifacts.CopyArtifact";
    String COPY_ID_SUMMARY = "RecentArtifacts.CopyIdAndSummary";
    String CUSTOM_COPY = "RecentArtifacts.CustomCopy";
    String PASTE_ITEM_KEY = "RecentArtifacts.PasteItemKey";
    String OPEN_IN_FRAME = "RecentArtifacts.OpenInFrame";
    String OPEN_IN_TAB = "RecentArtifacts.OpenInTab";
    String OPEN_IN_BROWSER = "RecentArtifacts.OpenInBrowser";
    String EDIT_ITEM = "RecentArtifacts.EditArtifact";
    String START_WORK = "RecentArtifacts.StartWork";
    String STOP_WORK = "RecentArtifacts.StopWork";
  }

  interface Merge {
    String MANUAL_MERGE_COMMENTS = "Merge.ManualMergeComments";
    String COPY = "Merge.Copy";
    String PASTE = "Merge.Paste";
    String DELETE_COMMENT = "Merge.DeleteComment";
    String HIDE_NOT_CHANGED = "Merge.HideNotChanged";
    String RESOLVE_LOCAL = "Merge.ResolveLocal";
    String RESOLVE_BASE = "Merge.ResolveBase";
    String RESOLVE_REMOTE = "Merge.ResolveRemote";
    String RESOLVE_IGNORE = "Merge.ResolveIgnore";
    String COMMIT_UPLOAD = "Merge.CommitUpload";
  }
}