package com.almworks.timetrack.gui;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.DBDataRoles;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.Engine;
import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.edit.EditLifecycleImpl;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.gui.timesheet.BasicPublishTimeForm;
import com.almworks.timetrack.gui.timesheet.TimesheetForm;
import com.almworks.timetrack.gui.timesheet.TimesheetFormData;
import com.almworks.timetrack.gui.timesheet.WorkPeriod;
import com.almworks.timetrack.impl.TaskRemainingTime;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.Terms;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PublishTimeAction extends SimpleAction {
  private static final String SELECTED_TAB_INDEX = "selectedTab";
  
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final AtomicBoolean myWorking = new AtomicBoolean(false);

  public PublishTimeAction() {
    super("Edit and &Publish Time\u2026", Icons.PUBLISH_TIME_ACTION);
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, "Edit time records and publish into " + Terms.ref_ConnectionType);
    watchRole(TimeTracker.TIME_TRACKER);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    TimeTracker tt = context.getSourceObject(TimeTracker.TIME_TRACKER);
    context.updateOnChange(tt.getModifiable());
    context.updateOnChange(myModifiable);
    if (!DBDataRoles.isAnyConnectionAllowsUpload(context)) {
      context.setEnabled(EnableState.INVISIBLE);
      context.updateOnChange(context.getSourceObject(Engine.ROLE).getConnectionManager().getConnectionsModifiable());
      return;
    }

    if (myWorking.get()) {
      context.setEnabled(EnableState.DISABLED);
    } else if (!tt.isEmpty()) {
      context.setEnabled(EnableState.ENABLED);
    } else {
      context.setEnabled(EnableState.INVISIBLE);
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    final TimeTracker tt = context.getSourceObject(TimeTracker.TIME_TRACKER);
    final Map<TimeTrackerTask, List<TaskTiming>> timings = tt.getRecordedTimings();
    final Map<TimeTrackerTask, TaskRemainingTime> remainings = tt.getRemainingTimes();
    final Map<TimeTrackerTask, Integer> timeDeltas = tt.getSpentDeltas();

    final DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createBuilder("publishTime");
    final ItemModelRegistry registry = context.getSourceObject(ItemModelRegistry.ROLE);

    CantPerformException.ensure(lockWorking());
    context.getSourceObject(Database.ROLE).readForeground(new ReadTransaction<Map<TimeTrackerTask, LoadedItem>>() {
      @Override
      public Map<TimeTrackerTask, LoadedItem> transaction(DBReader reader) throws DBOperationCancelledException {
        return loadArtifacts(
          CollectionUtil.setUnion(timings.keySet(), remainings.keySet(), timeDeltas.keySet()),
          reader, registry);
      }
    }).onSuccess(ThreadGate.AWT, new Procedure<Map<TimeTrackerTask, LoadedItem>>() {
      @Override
      public void invoke(Map<TimeTrackerTask, LoadedItem> items) {
        performLoaded(items, timings, remainings, timeDeltas, builder);
      }
    });
  }

  private Map<TimeTrackerTask, LoadedItem> loadArtifacts(
    Collection<TimeTrackerTask> tasks, DBReader reader, ItemModelRegistry registry)
  {
    final LinkedHashMap<TimeTrackerTask, LoadedItem> result = Collections15.linkedHashMap();
    for(final TimeTrackerTask task : tasks) {
      final LoadedItem loaded = task.load(registry, reader);
      if(loaded != null) {
        result.put(task, loaded);
      }
    }
    return result;
  }

  private void setNotWorking() {
    myWorking.set(false);
    myModifiable.fireChanged();
  }

  private boolean lockWorking() {
    boolean changed = myWorking.compareAndSet(false, true);
    if (!changed) return false;
    myModifiable.fireChanged();
    return true;
  }

  private void performLoaded(
    Map<TimeTrackerTask, LoadedItem> items,
    Map<TimeTrackerTask, List<TaskTiming>> timings,
    Map<TimeTrackerTask, TaskRemainingTime> remainings,
    Map<TimeTrackerTask, Integer> timeDeltas,
    DialogBuilder builder)
  {
    if(items == null || items.isEmpty()) {
      setNotWorking();
      return;
    }

    final TimesheetFormData data = new TimesheetFormData(
      Collections15.mergeMaps2(items, timings),
      Collections15.mergeMaps2(items, remainings),
      Collections15.mergeMaps2(items, timeDeltas));

    final Configuration config = builder.getConfiguration();

    final TimesheetForm form = new TimesheetForm(data, config);
    form.init();

    final BasicPublishTimeForm basicForm = new BasicPublishTimeForm(data);

    final JTabbedPane tabs = new JTabbedPane();
    Aqua.makeBorderlessTabbedPane(tabs);
    Aero.makeBorderlessTabbedPane(tabs);

    builder.setBottomBevel(false);
    if(Aqua.isAqua()) {
      builder.setBorders(false);
    } else {
      form.getComponent().setBorder(UIUtil.BORDER_5);
      basicForm.getComponent().setBorder(UIUtil.BORDER_5);
    }

    tabs.addTab("Summary", null, basicForm.getComponent());
    tabs.addTab("Timesheet", null, form.getComponent());
    tabs.setSelectedIndex(config.getIntegerSetting(SELECTED_TAB_INDEX, 0));
    builder.setContent(tabs);

    builder.setTitle("Edit and Publish Time");
    builder.setModal(false);
    builder.setEmptyCancelAction();
//    builder.setIgnoreStoredSize(true);
    SynchronizedBoolean commitImmediately = new SynchronizedBoolean(false);
    JCheckBox checkbox =
      ItemActionUtils.createCommitImmediatelyCheckbox(builder.getConfiguration(), commitImmediately);
    builder.setBottomLineComponent(checkbox);

    builder.setOkAction(createPublishAction(commitImmediately, data));
    builder.addAction(createSaveAction(data));

    EditLifecycleImpl.install(builder, null);

    builder.detachOnDispose(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        setNotWorking();
        config.setSetting(SELECTED_TAB_INDEX, tabs.getSelectedIndex());
      }
    });

    builder.showWindow();
  }

  private AnAction createPublishAction(final SynchronizedBoolean flag, final TimesheetFormData data) {
    return new SimpleAction("&Publish") {
      protected void doPerform(ActionContext context) throws CantPerformException {
        final Map<LoadedItem, List<TaskTiming>> timeMap = data.getTimeMapForPublish();
        final Map<LoadedItem, TaskRemainingTime> remMap = data.getRemainingTimesForPublish();
        final Map<LoadedItem, Integer> deltas = data.getSpentDeltasForPublish();

        final TimeTrackingCustomizer ttc = context.getSourceObject(TimeTrackingCustomizer.ROLE);
        ttc.publishTime(context, timeMap, remMap, deltas, flag.get());
        saveUserAdjustments(data);
        removePublishedData(timeMap, remMap, deltas);
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(data.getModifiable());
        context.setEnabled(data.hasDataForPublish());
      }
    };
  }
  
  private AnAction createSaveAction(final TimesheetFormData data) {
    return new SimpleAction("&Save Changes") {
      protected void doPerform(ActionContext context) throws CantPerformException {
        saveUserAdjustments(data);
        try {
          WindowController.CLOSE_ACTION.perform(context);
        } catch (CantPerformException e) {
          // ignore
        }
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(data.getModifiable());
        context.setEnabled(data.hasDataForSave());
      }
    };
  }

  private void saveUserAdjustments(TimesheetFormData data) {
    final TimeTracker tt = Context.get(TimeTracker.class);
    if(tt == null) {
      return;
    }

    for(final WorkPeriod period : data.getDeleted()) {
      tt.replaceTiming(toTask(period.getArtifact()), period.getTiming(), null);
    }

    final Map<WorkPeriod, WorkPeriod> map = data.getEdited();
    while(!map.isEmpty()) {
      final WorkPeriod from = map.keySet().iterator().next();

      WorkPeriod to = map.remove(from);
      while(map.containsKey(to)) {
        to = map.remove(to);
      }

      if(to == null) {
        assert false;
        continue;
      }

      if(from.getTiming().isCurrent()) {
        final TaskTiming toTiming = to.getTiming();
        final TaskTiming replacement = new TaskTiming(toTiming.getStarted(), -1, toTiming.getComments());
        final TrackerSimplifier ts = new TrackerSimplifier(tt);
        tt.replaceTiming(ts.task, ts.getLastTiming(), replacement);
      } else if(Util.equals(from.getArtifact(), to.getArtifact())) {
        tt.replaceTiming(toTask(from.getArtifact()), from.getTiming(), to.getTiming());
      } else {
        tt.replaceTiming(toTask(from.getArtifact()), from.getTiming(), null);
        tt.addTiming(toTask(to.getArtifact()), to.getTiming());
      }
    }

    for(final WorkPeriod period : data.getAdded()) {
      tt.addTiming(toTask(period.getArtifact()), period.getTiming());
    }

    for(final Map.Entry<LoadedItem, TaskRemainingTime> e : data.getAddedRemainingTimes().entrySet()) {
      tt.setRemainingTime(toTask(e.getKey()), e.getValue());
    }

    for(final Map.Entry<LoadedItem, Integer> e : data.getAddedSpentDeltas().entrySet()) {
      tt.setSpentDelta(toTask(e.getKey()), e.getValue());
    }
  }

  private void removePublishedData(
    Map<LoadedItem, List<TaskTiming>> timeMap,
    Map<LoadedItem, TaskRemainingTime> remMap,
    Map<LoadedItem, Integer> deltas)
  {
    final TimeTracker tt = Context.get(TimeTracker.class);
    if (tt == null) {
      return;
    }

    for(final Map.Entry<LoadedItem, List<TaskTiming>> e : timeMap.entrySet()) {
      for(final TaskTiming timing : e.getValue()) {
        final TimeTrackerTask task = toTask(e.getKey());
        final boolean replaced = tt.replaceTiming(task, timing, null);
        if (!replaced) {
          tt.removePeriod(task, timing.getStarted(), timing.getStopped());
        }
      }
    }

    for(final Map.Entry<LoadedItem, TaskRemainingTime> e : remMap.entrySet()) {
      tt.setRemainingTime(toTask(e.getKey()), null);
    }

    for(final Map.Entry<LoadedItem, Integer> e : deltas.entrySet()) {
      tt.setSpentDelta(toTask(e.getKey()), null);
    }
  }

  private TimeTrackerTask toTask(LoadedItem a) {
    return new TimeTrackerTask(a.getItem());
  }
}