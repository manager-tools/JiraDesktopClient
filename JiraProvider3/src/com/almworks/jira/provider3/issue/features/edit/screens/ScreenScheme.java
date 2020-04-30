package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.jira.provider3.issue.editor.IssueScreen;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

class ScreenScheme {
  private static final String CREATE_NAME = "admin.issue.operations.create";
  private static final String EDIT_NAME = "admin.issue.operations.edit";
  private static final Integer OP_DEFAULT = 0;
  private static final Integer OP_CREATE = 1;
  private static final Integer OP_EDIT = 2;
  private final Map<Long, Map<Long, Map<Integer, ScreenInfo>>> myProjectTypeOperationToScreen = Collections15.hashMap();

  /**
   * @param project project ID
   * @param type type ID or null for default (all types that are not explicitly configured)
   * @param operationName or empty for default (all operations that are not explicitly configured)
   */
  void addScreen(long project, Long type, String operationName, ScreenInfo screen) {
    int operation;
    if (operationName == null || operationName.isEmpty()) operation = OP_DEFAULT;
    else if (CREATE_NAME.equals(operationName)) operation = OP_CREATE;
    else if (EDIT_NAME.equals(operationName)) operation = OP_EDIT;
    else return;
    addScreen(project, type, operation, screen);
  }

  Map<Long, Map<Long, Map<Integer, ScreenInfo>>> getProjectTypeOperationToScreen() {
    return myProjectTypeOperationToScreen;
  }

  void addScreen(long project, Long type, int operation, ScreenInfo screen) {
    Map<Long, Map<Integer, ScreenInfo>> byType = myProjectTypeOperationToScreen.get(project);
    if (byType == null) {
      byType = Collections15.hashMap();
      myProjectTypeOperationToScreen.put(project, byType);
    }
    Map<Integer, ScreenInfo> byOperation = byType.get(type);
    if (byOperation == null) {
      byOperation = Collections15.hashMap();
      byType.put(type, byOperation);
    }
    ScreenInfo prev = byOperation.put(operation, screen);
    if (prev != null) LogHelper.error("Replacing screen", prev, screen);
  }

  @Nullable
  public ScreenInfo getScreen(long project, Long type, boolean create) {
    Map<Long, Map<Integer, ScreenInfo>> byType = myProjectTypeOperationToScreen.get(project);
    if (byType == null) return null;
    Map<Integer, ScreenInfo> byOperation = byType.get(type != null ? type : null);
    if (byOperation == null) byOperation = byType.get(null);
    if (byOperation == null) { // Should have default
      LogHelper.error("Missing screens for", project, type);
      return null;
    }
    ScreenInfo info = byOperation.get(create ? OP_CREATE : OP_EDIT);
    if (info == null) info = byOperation.get(OP_DEFAULT);
    if (info == null) {
      LogHelper.error("Missing screen for", project, type, create);
      return null;
    }
    return info;
  }

  static class ScreenInfo {
    private final long myId;
    private final String myName;
    private final List<IssueScreen.Tab> myTabs;

    public ScreenInfo(long id, String name, List<IssueScreen.Tab> tabs) {
      myId = id;
      myName = name;
      myTabs = Collections15.unmodifiableListCopy(tabs);
    }

    public String getName() {
      return myName;
    }

    public long getId() {
      return myId;
    }

    public List<IssueScreen.Tab> getTabs() {
      return myTabs;
    }

    @Override
    public String toString() {
      return "Screen[" + myName + "](" + myTabs.size() + ")";
    }
  }
}
