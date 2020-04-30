package com.almworks.sync;

import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.engine.SyncTask;
import com.almworks.api.engine.Synchronizer;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogSingleton;
import com.almworks.util.L;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.model.SetHolder;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAbstractAction;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import static com.almworks.util.model.SetHolderUtils.actualizingSequentialListener;
import static com.almworks.util.model.SetHolderUtils.fromChangeListener;

class SyncWindow extends DialogSingleton {
  private final SyncForm myForm;
  private final Configuration myConfiguration;
  private final Synchronizer mySynchronizer;
  private final OrderListModel<SyncTask> myTaskListModel = new OrderListModel();
  private final Comparator<SyncTask> myComparator = new Comparator<SyncTask>() {
    public int compare(SyncTask task1, SyncTask task2) {
      return String.CASE_INSENSITIVE_ORDER.compare(task1.getTaskName(), task2.getTaskName());
    }
  };
  private final Map<SyncTask, Detach> myDetaches = Collections15.hashMap();
  private final AListModel<SyncTask> mySortedTasks = SortedListDecorator.create(myTaskListModel, myComparator);

  private ScalarModel.Consumer<SyncTask.State> myProblemsListener = null;

  public SyncWindow(DialogManager dialogManager, Configuration configuration, Synchronizer synchronizer,
    ConfigAccessors.Bool showOnSync, TextDecoratorRegistry textDecoratorRegistry)
  {
    super(dialogManager, "sync");
    myConfiguration = configuration;
    mySynchronizer = synchronizer;
    myForm = new SyncForm(myConfiguration, showOnSync, textDecoratorRegistry);
    myForm.setTaskListModel(mySortedTasks);
  }

  public boolean show() {
    boolean appeared = super.show();
    myForm.setTaskListModel(mySortedTasks);
    myForm.revalidate();
    if (appeared)
      myForm.focusOnTasks();
    return appeared;
  }

  protected void setupDialog(DialogBuilder builder) {
    builder.setContent(myForm);
    builder.setBottomLineComponent(myForm.getBottomLineComponent());
    builder.setTitle(L.dialog("Synchronization"));
    builder.setCancelAction(new AnAbstractAction(L.actionName("Close Window")) {
      public void perform(ActionContext context) throws CantPerformException {
//        WindowController.CLOSE_WINDOW.perform(context);
      }
    });
    builder.setPreferredSize(UIUtil.getRelativeDimension(myForm.getComponent(), 52, 30));
    if(Aqua.isAqua()) {
      builder.setBorders(false);
      builder.setBottomBevel(false);
    }
  }

  protected void attach() {
    myTaskListModel.clear();
    myForm.clearModels();
    subscribeForActions();
    subscribeForTaskList();
    subscribeForProblemsList();
  }

  protected void detach() {
    super.detach();
    for (Iterator<Detach> ii = myDetaches.values().iterator(); ii.hasNext();) {
      ii.next().detach();
      ii.remove();
    }
  }

  private void subscribeForActions() {
    myProblemsListener = new ScalarModel.Adapter<SyncTask.State>() {
      public void onScalarChanged(ScalarModelEvent<SyncTask.State> event) {
        myForm.updateActions();
      }
    };

    mySynchronizer.getProblems().addInitListener(lifespan(), ThreadGate.AWT, fromChangeListener(new ChangeListener() {
      @Override
      public void onChange() {
        myForm.updateActions();
      }
    }));
  }

  private void subscribeForProblemsList() {
    mySynchronizer.getProblems().addInitListener(lifespan(), ThreadGate.AWT, actualizingSequentialListener(new SetHolder.Listener<SyncProblem>() {
      @Override
      public void onSetChanged(@NotNull SetHolder.Event<SyncProblem> event) {
        myForm.addProblems(event.getAdded());
        myForm.removeProblems(event.getRemoved());
      }
    }));
  }

  private void subscribeForTaskList() {
    Threads.assertAWTThread();
    mySynchronizer.getTasks().addInitListener(lifespan(), ThreadGate.AWT, actualizingSequentialListener(new SetHolder.Listener<com.almworks.api.engine.SyncTask>() {
      @Override
      public void onSetChanged(@NotNull SetHolder.Event<SyncTask> event) {
        for (SyncTask task : event.getAdded()) {
          addTask(task);
        }
        removeTasks(event.getRemoved());
      }
    }));
  }

  private void addTask(SyncTask task) {
    myTaskListModel.addElement(task);
    DetachComposite detach = new DetachComposite();
    detach.add(myTaskListModel.listenElement(task));
    task.getState().getEventSource().addAWTListener(detach, myProblemsListener);
    myDetaches.put(task, detach);
  }

  private void removeTasks(final Collection<SyncTask> removed) {
    myTaskListModel.removeAll(new Condition<SyncTask>() {
      public boolean isAccepted(SyncTask task) {
        for (SyncTask removedTask : removed)
          if (removedTask.equals(task)) {
            Util.NN(myDetaches.remove(task), Detach.NOTHING).detach();
            return true;
          }
        return false;
      }
    });
    myForm.updateActions();
  }

  public void focusOnProblem(SyncProblem problem) {
    myForm.focusOnProblem(problem);
  }
}
