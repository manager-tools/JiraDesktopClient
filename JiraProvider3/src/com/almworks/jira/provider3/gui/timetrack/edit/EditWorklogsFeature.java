package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.MultiplexerEditFeature;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EditWorklogsFeature extends MultiplexerEditFeature {
  public static final EditFeature INSTANCE = new EditWorklogsFeature();
  public static final String DEFAULT_UNIT = "h";

  @Override
  protected EditFeature chooseEdit(ActionContext context, @Nullable UpdateRequest updateRequest) throws CantPerformException {
    if (updateRequest != null) updateRequest.watchRole(LoadedWorklog.WORKLOG);
    List<LoadedWorklog> worklogs = context.getSourceCollection(LoadedWorklog.WORKLOG);
    if (worklogs.isEmpty()) return null;
    return worklogs.size() == 1 ? EditSingleWorklogFeature.INSTANCE : EditManyWorklogsFeature.INSTANCE;
  }
}
