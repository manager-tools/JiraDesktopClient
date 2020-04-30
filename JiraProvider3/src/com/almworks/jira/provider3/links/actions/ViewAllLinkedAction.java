package com.almworks.jira.provider3.links.actions;

import com.almworks.api.actions.ItemTableUtils;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ReplaceTabKey;
import com.almworks.api.engine.Connection;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.source.IssuesByKeyItemSource;
import com.almworks.jira.provider3.links.LoadedLink;
import com.almworks.jira.provider3.links.LoadedLink2;
import com.almworks.util.collections.LongSet;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.List;
import java.util.Set;

class ViewAllLinkedAction extends SimpleAction {
  public static final AnAction ALL_LINKS = new ViewAllLinkedAction();
  private static final ReplaceTabKey TAB_KEY = new ReplaceTabKey("Links");

  private ViewAllLinkedAction() {
    super((String)null, Icons.ACTION_VIEW_LINKED_ISSUE);
    watchRole(ItemWrapper.ITEM_WRAPPER);
    setDefaultPresentation(PresentationKey.NAME, "View All Linked Issues");
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    getAllTargetLinks(context, issue);
  }

  public static List<LoadedLink2> getAllTargetLinks(ActionContext context, ItemWrapper issue) throws CantPerformException {
    GuiFeaturesManager features = context.getSourceObject(GuiFeaturesManager.ROLE);
    LoadedModelKey<List<LoadedLink2>> linksKey = LoadedLink2.getLinksKey(features);
    return CantPerformException.ensureNotEmpty(issue.getModelKeyValue(linksKey));
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    showAllLinked(context);
  }

  public static void showAllLinked(ActionContext context) throws CantPerformException {
    ItemWrapper issue = context.getSourceObject(ItemWrapper.ITEM_WRAPPER);
    List<LoadedLink2> links = getAllTargetLinks(context, issue);
    showLinked(context, "All Linked", issue.getConnection(), links, issue.getItem());
  }

  public static void showLinked(ActionContext context, String queryName, Connection connection,
    List<LoadedLink2> links, long additional) throws CantPerformException
  {
    JiraConnection3 jiraConnection = CantPerformException.cast(JiraConnection3.class, connection);
    CantPerformException.ensureNotEmpty(links);
    ItemSource source = createArtifactSource(links, jiraConnection, additional);
    ItemTableUtils.showArtifactSource(context, queryName, connection, source, TAB_KEY);
  }

  private static ItemSource createArtifactSource(List<LoadedLink2> links, final JiraConnection3 connection, long addSource)
    throws CantPerformException
  {
    LongSet issues = new LongSet();
    if (addSource > 0) issues.add(addSource);
    Set<String> missingOpposites = Collections15.hashSet();
    for (LoadedLink2 link : links) {
      long oppositeIssue = link.getOppositeIssue();
      if (oppositeIssue > 0) issues.add(oppositeIssue);
      String oppositeKey = link.getOppositeString(LoadedLink.KEY);
      if (!Util.NN(oppositeKey).trim().isEmpty()) missingOpposites.add(oppositeKey);
    }
    return IssuesByKeyItemSource.create("Linked Issues", connection, issues, missingOpposites);
  }
}
