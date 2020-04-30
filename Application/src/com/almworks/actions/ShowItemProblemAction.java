package com.almworks.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.api.sync.SynchronizationWindow;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.SetHolder;
import com.almworks.util.model.SetHolderUtils;
import com.almworks.util.ui.actions.*;

import static com.almworks.util.collections.Functional.*;

/**
 * @author dyoma
 */
class ShowItemProblemAction extends SimpleAction {
  protected ShowItemProblemAction() {
    super(L.actionName("View Synchronization &Problems"), Icons.STATUSBAR_SYNCHRONIZATION_PROBLEMS);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, L.tooltip("View synchronization problems for selected $(" + Terms.key_artifact + ")"));
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    SynchronizationWindow window = context.getSourceObject(SynchronizationWindow.ROLE);
    Iterable<ItemSyncProblem> problems = findProblems(context);
    ItemSyncProblem problem = first(problems);
    boolean onlyOneProblem = !hasNth(problems, 1);
    if (problem != null && onlyOneProblem)
      window.showProblem(problem);
    else
      window.show();
  }

  protected void customUpdate(final UpdateContext context) throws CantPerformException {
    ItemActionUtils.basicUpdate(context, false);
    final UpdateService updateService = context.getUpdateRequest().getUpdateService();
    SetHolder<SyncProblem> allProblems = context.getSourceObject(Engine.ROLE).getSynchronizer().getProblems();
    allProblems.addInitListener(context.getUpdateRequest().getLifespan(), ThreadGate.AWT, SetHolderUtils.fromChangeListener(new ChangeListener() {
      public void onChange() {
        updateService.requestUpdate();
      }
    }));
    context.getSourceObject(SynchronizationWindow.ROLE);
    context.setEnabled(isEmpty(findProblems(context)) ? EnableState.INVISIBLE : EnableState.ENABLED);
  }

  // todo #822 (http://bugzilla/main/show_bug.cgi?id=822) problem as data role?
  private Iterable<ItemSyncProblem> findProblems(ActionContext context) throws CantPerformException {
    long item = context.getSourceObject(ItemWrapper.ITEM_WRAPPER).getItem();
    return context.getSourceObject(Engine.ROLE).getSynchronizer().getItemProblems(item);
  }
}
