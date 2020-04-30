package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklog;
import com.almworks.timetrack.gui.timesheet.TimeSheetGrid;
import com.almworks.timetrack.gui.timesheet.WorkPeriod;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author dyoma
 */
class ViewTimeReportAction extends SimpleAction {
  public ViewTimeReportAction() {
    super("View Time Report");
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<LoadedItem> items = context.getSourceCollection(LoadedItem.LOADED_ITEM);
    CantPerformException.ensure(items.size() > 1);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    List<LoadedItem> items = context.getSourceCollection(LoadedItem.LOADED_ITEM);
    FrameBuilder builder = context.getSourceObject(WindowManager.ROLE).createFrame("timeReport");
    builder.setTitle("Time Report");
    TimeSheetGrid grid = createGrid(items);
    grid.setCornerText("");
    builder.setContent(grid.getComponent());
    builder.showWindow();
  }

  @NotNull
  private TimeSheetGrid createGrid(List<LoadedItem> items) throws CantPerformException {
    CantPerformException.ensure(items.size() > 1);
    OrderListModel<WorkPeriod> workList = OrderListModel.create();
    OrderListModel<LoadedItem> artifactsModel = OrderListModel.create();
    artifactsModel.setElements(items);
    LoadedModelKey<List<LoadedWorklog>> key = LoadedWorklog.getWorklogsKey(items.get(0));
    items.forEach(loadedItem -> {
      List<LoadedWorklog> worklogs = loadedItem.getModelKeyValue(key);
      worklogs.forEach(loadedWorklog -> {
        Long startedMillis = loadedWorklog.getStartedMillis();
        Long endMillis = loadedWorklog.getEndMillis();
        if (startedMillis != null && endMillis != null)
          workList.addElement(new WorkPeriod(new TaskTiming(startedMillis, endMillis, loadedWorklog.getComment()), loadedItem));
      });
    });
    TimeSheetGrid grid = new TimeSheetGrid(workList, artifactsModel);
    return grid;
  }
}
