package com.almworks.jira.provider3.app.connection;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.viewer.textdecorator.TextDecorationParser;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.jira.provider3.gui.actions.source.IssuesByKeyItemSource;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.jira.provider3.services.JiraPatterns;
import com.almworks.util.LogHelper;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.ArrayUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IssueReferenceParser implements TextDecorationParser {
  private static final Pattern REF = Pattern.compile("\\b(" + JiraPatterns.PROJECT_KEY_PATTERN + ")\\-\\d+\\b");
  private final GuiFeaturesManager myManager;

  private IssueReferenceParser(GuiFeaturesManager manager) {
    myManager = manager;
  }

  public void decorate(Context context) {
    EnumTypesCollector.Loaded prjType = myManager.getEnumTypes().getType(Project.ENUM_TYPE);
    if (prjType == null) {
      LogHelper.error("Missing projects");
      return;
    }
    Matcher matcher = REF.matcher(context.getText());
    List<LoadedItemKey> projects = null;
    String[] projectKeys = null;
    while (matcher.find()) {
      String projectKey = matcher.group(1);
      if (projectKeys == null) {
        projects = prjType.getEnumValues(new ItemHypercubeImpl());
        projectKeys = getKeys(projects);
      }
      int index = ArrayUtil.indexOf(projectKeys, projectKey);
      if (index >= 0) {
        LinkArea area = context.addLink(matcher);
        JiraConnection3 connection = projects.get(index).getConnection(JiraConnection3.class);
        if (connection != null) area.setDefaultAction(new ShowIssueAction(connection, area.getText()));
      }
    }
  }

  public String[] getKeys(List<LoadedItemKey> items) {
    String[] keys = new String[items.size()];
    for (int i = 0; i < items.size(); i++) {
      ResolvedItem artifact = items.get(i);
      keys[i] = artifact.getId();
    }
    return keys;
  }

  public static void register(JiraProvider3 provider) {
    provider.getActor(TextDecoratorRegistry.ROLE).addParser(new IssueReferenceParser(provider.getFeaturesManager()));
  }

  private class ShowIssueAction extends SimpleAction {
    private final String myIssueKey;
    private final JiraConnection3 myConnection;

    public ShowIssueAction(JiraConnection3 connection, String issueKey) {
      super("View " + issueKey);
      myIssueKey = issueKey;
      myConnection = connection;
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      ItemSource artifactSource = new IssuesByKeyItemSource(new String[] {myIssueKey}, myConnection);
      ItemCollectionContext itemContext = ItemCollectionContext.createGeneral(myIssueKey, myConnection);
      context.getSourceObject(ExplorerComponent.ROLE).showItemsInTab(artifactSource, itemContext, true);
    }
  }
}
