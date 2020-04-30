package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.issue.editor.IssueScreen;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.commons.Factory;
import com.almworks.util.images.Icons;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class RemoteScreen extends EditIssueScreen implements Comparable<RemoteScreen>  {
  private final JiraScreens myScreens;
  private final RelevantFields myRelevantFields;
  private final long myJiraId;

  RemoteScreen(JiraScreens screens, RelevantFields relevantFields, String name, long jiraId) {
    super(Factory.Const.newConst(name), "j" + jiraId, Icons.ACTION_OPEN_IN_BROWSER);
    myScreens = screens;
    myRelevantFields = relevantFields;
    myJiraId = jiraId;
  }

  public static void collectRemotes(VersionSource source, EditItemModel model, @Nullable JiraScreens screens, Collection<EditIssueScreen> target) {
    if (screens == null) return;
    RelevantFields relevantFields = RelevantFields.ensureLoaded(source, model);
    ArrayList<RemoteScreen> result = Collections15.arrayList();
    for (ScreenScheme.ScreenInfo screen : screens.getAllScreens()) result.add(new RemoteScreen(screens, relevantFields, screen.getName(), screen.getId()));
    Collections.sort(result);
    target.addAll(result);
  }

  @Override
  public String getTooltip() {
    return getName() + " " + EditIssueFeature.I18N.getString("edit.screens.generated.remote.tooltip");
  }

  @Override
  public int compareTo(RemoteScreen o) {
    return getName().compareTo(o.getName());
  }

  public List<Tab> getTabs(EditModelState model) {
    ScreenScheme.ScreenInfo screen = myScreens.getScreen(myJiraId);
    if (screen == null) return Collections.emptyList();
    if (myRelevantFields == null) return screen.getTabs();
    ArrayList<Tab> tabs = Collections15.arrayList();
    List<String> applicable = myRelevantFields.getFieldIds(model);
    for (Tab tab : screen.getTabs()) {
      ArrayList<String> filtered = Collections15.arrayList(tab.getFieldIds());
      filtered.retainAll(applicable);
      if (filtered.isEmpty()) continue;
      if (filtered.size() == tab.getFieldIds().size()) tabs.add(tab);
      else tabs.add(new Tab(tab.getName(), filtered));
    }
    return tabs;
  }

  @Nullable
  @Override
  public Object checkModelState(EditModelState model, @Nullable Object prevState) {
    return myRelevantFields != null ? myRelevantFields.checkModelState(model, prevState) : null;
  }

  @Nullable
  public static EditIssueScreen choose(@Nullable ScreenScheme resolvedScheme, Iterable<? extends IssueScreen> screens, EditModelState model, boolean create) {
    if (resolvedScheme == null) return null;
    Long project = model.getSingleEnumValue(Issue.PROJECT);
    Long type = model.getSingleEnumValue(Issue.ISSUE_TYPE);
    if (project == null) return null;
    ScreenScheme.ScreenInfo info = resolvedScheme.getScreen(project, type, create);
    if (info == null) return null;
    for (IssueScreen s : screens) {
      RemoteScreen screen = Util.castNullable(RemoteScreen.class, s);
      if (screen == null) continue;
      if (screen.myJiraId == info.getId()) return screen;
    }
    return null;
  }
}
