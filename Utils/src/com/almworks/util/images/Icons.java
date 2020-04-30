package com.almworks.util.images;

import javax.swing.*;

/**
 * This class contains icon handles for all icons in the application.
 * <p/>
 * Two rules:
 * <ol>
 * <li>Do not EVER change icon ID. It is fixed and used NOT ONLY in this file, but in other non-java resources,
 * such as Excel files.
 * <li>Group icons by meaning. There's a tool - IconsCounter - that would give you the next image ID.
 * </ol>
 */
public interface Icons {
  // Do not change icon ID!

  // Navigation Tree
  IconHandle NODE_QUERY = IconHandle.smallIcon(1);
  IconHandle NODE_CONNECTION = IconHandle.smallIcon(3);
  IconHandle NODE_CONNECTION_WITH_ALERT = IconHandle.smallIcon(35);
  IconHandle NODE_CONNECTION_OFFLINE = NODE_CONNECTION_WITH_ALERT;
  IconHandle NODE_CONNECTION_INITIALIZING = IconHandle.smallIcon(205, "com/almworks/rc/i205.gif");
  IconHandle NODE_REMOTE_QUERY = IconHandle.smallIcon(29);
  IconHandle NODE_SYSTEM_QUERY = IconHandle.smallIcon(30);
  IconHandle NODE_GENERIC = IconHandle.smallIcon(28);
  IconHandle NODE_FOLDER_OPEN = IconHandle.smallIcon(11);
  IconHandle NODE_FOLDER_CLOSED = IconHandle.smallIcon(12);
  IconHandle NODE_SYSTEM_FOLDER_OPEN = IconHandle.smallIcon(113);
  IconHandle NODE_SYSTEM_FOLDER_CLOSED = IconHandle.smallIcon(116);

  // Query Builder
  IconHandle QUERY_CONDITION_ENUM_ATTR = IconHandle.smallIcon(4);
  IconHandle QUERY_CONDITION_ENUM_SET = IconHandle.smallIcon(119);
  IconHandle QUERY_CONDITION_ENUM_TREE = IconHandle.smallIcon(124);
  IconHandle QUERY_CONDITION_INT_ATTR = IconHandle.smallIcon(5);
  IconHandle QUERY_CONDITION_TEXT_ATTR = IconHandle.smallIcon(6);
  IconHandle QUERY_CONDITION_DATE_ATTR = IconHandle.smallIcon(106);
  IconHandle QUERY_CONDITION_DATE_PRESET = QUERY_CONDITION_DATE_ATTR;
  IconHandle QUERY_CONDITION_FULL_TEXT_SEARCH = IconHandle.smallIcon(52);
  IconHandle QUERY_CONDITION_GENERIC = IconHandle.smallIcon(31);
  IconHandle QUERY_AND_ACTION = IconHandle.smallIcon(36);
  IconHandle QUERY_OR_ACTION = IconHandle.smallIcon(37);
  IconHandle QUERY_NOT_ACTION = IconHandle.smallIcon(38);
  IconHandle QUERY_NOT_NODE = IconHandle.smallIcon(40);
  IconHandle QUERY_NOR_ACTION = IconHandle.smallIcon(104);
  IconHandle QUERY_AND_NODE = IconHandle.smallIcon(73);
  IconHandle QUERY_OR_NODE = IconHandle.smallIcon(74);

  // Artifacts Table - Header
  IconHandle TABLE_COLUMN_SORTED_ASCENDING = IconHandle.smallIcon(7);
  IconHandle TABLE_COLUMN_SORTED_DESCENDING = IconHandle.smallIcon(8);

  // Artifacts Table - State
  IconHandle ARTIFACT_STATE_MODEL_EDITED = IconHandle.smallIcon(21);
  IconHandle ARTIFACT_STATE_MODEL_CONFLICT = IconHandle.smallIcon(22);
  IconHandle ARTIFACT_STATE_OUTDATED = IconHandle.smallIcon(23);
  IconHandle ARTIFACT_STATE_HAS_UNSYNC_CHANGES = IconHandle.smallIcon(24);
  IconHandle ARTIFACT_STATE_HAS_SYNC_CONFLICT = IconHandle.smallIcon(25);
  IconHandle ARTIFACT_STATE_HAS_SYNC_PROBLEM = IconHandle.smallIcon(26);
  IconHandle ARTIFACT_STATE_LOCALLY_ADDED = IconHandle.smallIcon(195);
  IconHandle ARTIFACT_STATE_LOCALLY_REMOVED = IconHandle.smallIcon(196);

  // Synchronization State
  IconHandle STATUSBAR_SYNCHRONIZATION_IN_PROGRESS = IconHandle.smallIcon(32, "com/almworks/rc/i32.gif");
  IconHandle STATUSBAR_SYNCHRONIZATION_PROBLEMS = ARTIFACT_STATE_HAS_SYNC_PROBLEM;
  IconHandle STATUSBAR_SYNCHRONIZATION_UNKNOWN = STATUSBAR_SYNCHRONIZATION_PROBLEMS;

  // Main menu and global actions
  IconHandle ACTION_CONFIGURE = IconHandle.smallIcon(10);
  IconHandle ACTION_GENERIC_ADD = IconHandle.smallIcon(13);
  IconHandle ACTION_GENERIC_REMOVE = IconHandle.smallIcon(14);
  IconHandle ACTION_GENERIC_COPY = IconHandle.smallIcon(208);
  IconHandle ACTION_SYNCHRONIZE = IconHandle.smallIcon(16);
  IconHandle ACTION_SYNCHRONIZE_FULL = IconHandle.smallIcon(17);
  IconHandle ACTION_SYNCHRONIZE_DOWNLOAD_ONLY = IconHandle.smallIcon(18);

  IconHandle ACTION_CREATE_NEW_ITEM = IconHandle.smallIcon(15);
  IconHandle ACTION_CREATE_NEW_QUERY = IconHandle.smallIcon(42);
  IconHandle ACTION_EDIT_QUERY = IconHandle.smallIcon(51);

  IconHandle ACTION_LIVE_TABLE_MODE = IconHandle.smallIcon(90);
  IconHandle ACTION_REFRESH_TABLE = ARTIFACT_STATE_OUTDATED;

  // Bug actions
  IconHandle ACTION_SYNCHRONIZE_THIS = IconHandle.smallIcon(19);
  IconHandle ACTION_OPEN_IN_BROWSER = IconHandle.smallIcon(41);
  IconHandle ACTION_SAVE = IconHandle.smallIcon(44);
  IconHandle ACTION_DISCARD = IconHandle.smallIcon(45);
  IconHandle ACTION_SAVE_ALL = IconHandle.smallIcon(46);
  IconHandle ACTION_SAVE_AS_TEMPLATE = IconHandle.smallIcon(66);
  IconHandle ACTION_COMMIT_NEW = IconHandle.smallIcon(86);
  IconHandle ACTION_EDIT_ARTIFACT = IconHandle.smallIcon(87);

  // Merge
  IconHandle MERGE_ACTION = IconHandle.smallIcon(75);
  IconHandle MERGE_STATE_CHANGED_LOCALLY = IconHandle.smallIcon(54);
  IconHandle MERGE_STATE_CHANGED_REMOTELY = IconHandle.smallIcon(55);
  IconHandle MERGE_STATE_CONFLICT = ARTIFACT_STATE_HAS_SYNC_CONFLICT;
  IconHandle MERGE_ACTION_APPLY_LOCAL = IconHandle.smallIcon(68);
  IconHandle MERGE_ACTION_APPLY_REMOTE = IconHandle.smallIcon(69);
  IconHandle MERGE_ACTION_APPLY_ORIGINAL = IconHandle.smallIcon(197);
  IconHandle MERGE_CHANGE_REMOTE = IconHandle.smallIcon(77);
  IconHandle MERGE_CHANGE_LOCAL = IconHandle.smallIcon(78);
  IconHandle MERGE_CHANGE_REMOTE_CONFLICT = IconHandle.smallIcon(83);
  IconHandle MERGE_CHANGE_LOCAL_CONFLICT = IconHandle.smallIcon(84);
  IconHandle ACTION_MERGE_HIDE_SAME = IconHandle.smallIcon(94);

  IconHandle DETAILS_PANEL = IconHandle.smallIcon(158);

  // Miscellaneous
  IconHandle ARROW_UP = IconHandle.smallIcon(47);
  IconHandle ARROW_DOWN = IconHandle.smallIcon(48);
  IconHandle ARROW_LEFT = IconHandle.smallIcon(49);
  IconHandle ARROW_RIGHT = IconHandle.smallIcon(50);
  IconHandle ACTION_GENERIC_CANCEL_OR_REMOVE = IconHandle.smallIcon(39);
  IconHandle STATUSBAR_UPDATE_AVAILABLE = IconHandle.smallIcon(20);

  IconHandle PROVIDER_ICON_BUGZILLA = IconHandle.icon(27, 50, 32);
  IconHandle PROVIDER_ICON_JIRA = IconHandle.icon(103, 50, 32);

  IconHandle ADD_ME_ACTION = IconHandle.smallIcon(57);
  IconHandle ACTION_CANCEL_ALL_TASKS = IconHandle.smallIcon(61);
  IconHandle ACTION_RESOLVE_PROBLEM = IconHandle.smallIcon(62);
  IconHandle APPLICATION_LOGO_ICON_BIG = IconHandle.smallIcon(64);
  IconHandle APPLICATION_LOGO_ICON_SMALL = IconHandle.smallIcon(76);
  IconHandle POINTING_TRIANGLE = IconHandle.smallIcon(79);

  IconHandle ACTION_UNDO = IconHandle.smallIcon(155);
  IconHandle ACTION_REDO = IconHandle.smallIcon(156);
  IconHandle ACTION_RESET = IconHandle.smallIcon(157);

  // Comments
  IconHandle ACTION_COMMENT_ADD = IconHandle.smallIcon(58);
  IconHandle ACTION_COMMENT_EDIT = IconHandle.smallIcon(59);
  IconHandle ACTION_COMMENT_REPLY = IconHandle.smallIcon(60);
  IconHandle ACTION_COMMENT_DELETE = IconHandle.smallIcon(173);

  // Links
  IconHandle ACTION_CREATE_LINK = IconHandle.smallIcon(144);
  IconHandle ACTION_REMOVE_LINK = IconHandle.smallIcon(146);
  IconHandle ACTION_VIEW_LINKED_ISSUE = NODE_SYSTEM_QUERY;

  IconHandle ATTENTION = IconHandle.smallIcon(81);
  IconHandle ATTENTION_BLINKING = IconHandle.smallIcon(206, "com/almworks/rc/i206.gif");

  IconHandle ERROR_REPORT_SENT_SUCCESSFULLY = IconHandle.smallIcon(80);
  IconHandle ERROR_REPORT_NOT_SENT = ATTENTION;

  ImageHandle ABOUT_BOX_BACKGROUND = ImageHandle.image(82, 386, 290);
  IconHandle ACTION_UPDATE_ARTIFACT = IconHandle.smallIcon(85);
  IconHandle ACTION_UPDATING_ITEM = IconHandle.smallIcon(203, "com/almworks/rc/i203.gif");
  IconHandle ACTION_COMMIT_ARTIFACT = IconHandle.smallIcon(93);
  IconHandle ACTION_COMMITTING_ITEM = IconHandle.smallIcon(204, "com/almworks/rc/i204.gif");
  IconHandle ACTION_UPDATE_ALL_ARTIFACTS = IconHandle.smallIcon(190);
  IconHandle ACTION_COMMIT_ALL_ARTIFACTS = IconHandle.smallIcon(191);

  IconHandle EXPAND_DOWN = IconHandle.smallIcon(88);
  IconHandle COLLAPSE_UP = IconHandle.smallIcon(89);

  IconHandle ACTION_RUN_QUERY = IconHandle.smallIcon(91);
  IconHandle ACTION_STOP_QUERY = IconHandle.smallIcon(92);

  IconHandle ATTACHMENT = IconHandle.icon(95, 10, 10);
  IconHandle DB_STATE_COLUMN_HEADER = IconHandle.icon(96, 11, 11);
  IconHandle POINTING_TRIANGLE_TRIMMED = IconHandle.icon(97, 5, 9);

  IconHandle ACTION_REMOVE_ARTIFACT = IconHandle.smallIcon(98);

  IconHandle ACTION_ATTACH_FILE = IconHandle.smallIcon(99);
  IconHandle ACTION_ATTACH_SCREENSHOT = IconHandle.smallIcon(101);
  IconHandle ACTION_DOWNLOAD_ATTACHMENT = IconHandle.smallIcon(193);
  IconHandle ACTION_ATTACH_TEXT = IconHandle.smallIcon(207);

  IconHandle DOUBLE_SIGN_ARROW_LEFT = IconHandle.smallIcon(63);
  IconHandle DOUBLE_SIGN_ARROW_RIGHT = IconHandle.smallIcon(100);

  IconHandle IMAGETOOL_CROP = IconHandle.smallIcon(102);
  IconHandle IMAGETOOL_ANNOTATE = IconHandle.smallIcon(114);

  IconHandle ACTION_EXPORT = IconHandle.smallIcon(105);
  IconHandle ACTION_CREATE_DISTRIBUTION = IconHandle.smallIcon(107);

  IconHandle NODE_DISTRIBUTION_FOLDER_OPEN = IconHandle.smallIcon(108);
  IconHandle NODE_DISTRIBUTION_FOLDER_CLOSED = NODE_DISTRIBUTION_FOLDER_OPEN;
  IconHandle NODE_DISTRIBUTION_FOLDER_HIDING = IconHandle.smallIcon(186);
  IconHandle NODE_DISTRIBUTION_GROUP = IconHandle.smallIcon(148);
  IconHandle NODE_DISTRIBUTION_GROUP_HIDING = IconHandle.smallIcon(187);

  IconHandle NODE_DISTRIBUTION_QUERY_PINNED = IconHandle.smallIcon(112);
  IconHandle ACTION_EXPORT_QUICK = IconHandle.smallIcon(115);

  IconHandle LOOKING_GLASS = IconHandle.icon(118, 14, 14);
  IconHandle LOOKING_GLASS_SMALL = IconHandle.icon(201, 10, 10);
  IconHandle LOOKING_GLASS_WITH_ARROW = IconHandle.icon(149, 14, 14);

  IconHandle SEARCH_IN_IDE = IconHandle.smallIcon(120);
  IconHandle WATCH_IN_IDE = IconHandle.smallIcon(122);

  IconHandle NODE_OUTBOX = IconHandle.smallIcon(123);

  IconHandle TRANSPOSE_SUMMARY_TABLE_ACTION = IconHandle.mediumIcon(128);
  IconHandle TRANSPOSE_SUMMARY_TABLE_ACTION_SMALL = IconHandle.smallIcon(143);

  IconHandle TABLE_ROW_SORTED_ASCENDING = IconHandle.smallIcon(129);
  IconHandle TABLE_ROW_SORTED_DESCENDING = POINTING_TRIANGLE_TRIMMED;

  IconHandle TOTAL_SIGMA = IconHandle.smallIcon(130);
  IconHandle OPEN_TABLE_IN_A_WINDOW = IconHandle.smallIcon(138);
  IconHandle ACTION_MOVE = IconHandle.smallIcon(131);
  IconHandle ACTION_ASSIGN = IconHandle.smallIcon(132);
  IconHandle ACTION_SET_USER_ME = ACTION_ASSIGN;

  IconHandle ACTION_EXPAND_ALL_COMMENTS = IconHandle.smallIcon(133);
  IconHandle FILE_VIEW_THUMBNAILS = IconHandle.smallIcon(134);
  IconHandle FILE_VIEW_DETAILS = IconHandle.smallIcon(135);

  IconHandle BLUE_TICK = IconHandle.smallIcon(136);
  IconHandle QUESTION = IconHandle.smallIcon(137);

  IconHandle ACTION_RESOLUTION_FIXED = BLUE_TICK;

  IconHandle STATE_FLAG_NEW = IconHandle.icon(139, 7, 7);
  IconHandle STATE_FLAG_DUMMY = IconHandle.icon(140, 7, 7);
  IconHandle STATE_FLAG_FULL_DOWNLOADED = IconHandle.icon(141, 7, 7);
  IconHandle STATE_FLAG_HALF_DOWNLOADED = IconHandle.icon(142, 7, 7);

  IconHandle RESOLVE_DUPLICATE = IconHandle.smallIcon(147);

  IconHandle SHOW_COMMENTS_TREE = IconHandle.smallIcon(150);

  IconHandle SEARCH_SMALL = IconHandle.icon(151, 9, 9);
  IconHandle GLOBE_SMALL = IconHandle.icon(152, 9, 9);

  IconHandle PRIVATE = IconHandle.smallIcon(154);
  IconHandle NOT_PRIVATE = IconHandle.smallIcon(153);

  // to remove
//  IconHandle NODE_TAG = IconHandle.smallIcon(53);
  //  IconHandle ACTION_ADD_TO_COLLECTION = IconHandle.smallIcon(109);
  //  IconHandle ACTION_REMOVE_FROM_COLLECTION = IconHandle.smallIcon(117);
  IconHandle ARTIFACT_SET = IconHandle.smallIcon(111);
//  IconHandle FAVORITES = IconHandle.smallIcon(121);

  IconHandle TAG_DEFAULT = IconHandle.smallIcon(159);
  IconHandle TAG_FAVORITES = IconHandle.smallIcon(160);
  IconHandle TAG_UNREAD = IconHandle.smallIcon(184);

  IconHandle CREATE_SUBTASK = IconHandle.smallIcon(161);

  IconHandle ARROW_UP_STRESSED = IconHandle.smallIcon(162);
  IconHandle ARROW_DOWN_STRESSED = IconHandle.smallIcon(163);

  IconHandle WATCHED = IconHandle.smallIcon(164);
  IconHandle VOTED = IconHandle.smallIcon(165);
  IconHandle VOTE_ACTION = IconHandle.smallIcon(166);
  IconHandle ADVANCED_VOTE_ACTION = IconHandle.smallIcon(185);
  IconHandle WATCH_ACTION = IconHandle.smallIcon(167);

  IconHandle START_WORK_ACTION = IconHandle.smallIcon(168);
  IconHandle STOP_WORK_ACTION = IconHandle.smallIcon(169);
  IconHandle TIME_TRACKING_STARTED = IconHandle.smallIcon(170);
  IconHandle TIME_TRACKING_PAUSED = IconHandle.smallIcon(179);
  IconHandle PUBLISH_TIME_ACTION = IconHandle.smallIcon(171);
  IconHandle ACTION_REORDER = IconHandle.smallIcon(172);

  IconHandle ACTION_WORKLOG_ADD = IconHandle.smallIcon(174);
  IconHandle ACTION_WORKLOG_EDIT = IconHandle.smallIcon(175);
  IconHandle ACTION_WORKLOG_DELETE = IconHandle.smallIcon(176);
  IconHandle ACTION_WORKLOG_ROLLBACK = ACTION_UNDO;
  IconHandle ACTION_TIME_TRACKING = TIME_TRACKING_STARTED;

  Icon ACTION_TIME_START_LARGE = IconHandle.icon(177, 32, 32);
  IconHandle ACTION_TIME_PAUSE_LARGE = IconHandle.icon(178, 32, 32);
  Icon ACTION_TIME_STOP_LARGE = IconHandle.icon(181, 32, 32);

  Icon OPEN_ARTIFACT_IN_FRAME = IconHandle.smallIcon(180);

  IconHandle APPLICATION_LOGO_ICON_SMALL_STARTED = IconHandle.smallIcon(182);
  IconHandle APPLICATION_LOGO_ICON_SMALL_PAUSED = IconHandle.smallIcon(183);

  IconHandle MAC_TAB_CLOSE_INACTIVE = IconHandle.smallIcon(188);
  IconHandle MAC_TAB_CLOSE_ACTIVE = IconHandle.smallIcon(189);

  IconHandle SWITCH = IconHandle.smallIcon(192);

  IconHandle FLAG = IconHandle.smallIcon(194);

  IconHandle ACTION_COPY = IconHandle.smallIcon(198);
  IconHandle ACTION_PASTE = IconHandle.smallIcon(199);
  IconHandle ACTION_COPY_URL = IconHandle.smallIcon(200);

  IconHandle ACTION_SHOW_NAVIGATION_AREA = IconHandle.smallIcon(202);

  IconHandle JIRA_CLIENT_SMALL = IconHandle.smallIcon(209);

  IconHandle GENERIC_FILTER = IconHandle.smallIcon(210);

  IconHandle VERSION_UNRELEASED = IconHandle.smallIcon(211);
  IconHandle VERSION_RELEASED = IconHandle.smallIcon(212);
  IconHandle VERSION_UNRELEASED_ARCHIVED = IconHandle.smallIcon(213);
  IconHandle VERSION_RELEASED_ARCHIVED = IconHandle.smallIcon(214);
}