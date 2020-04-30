package com.almworks.jira.provider3.services.upload;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class JiraUploadContext {
  private static final LocalizedAccessor.Value A_PREPARING = JiraUploadComponent.I18N.getFactory("stage.upload.activity.prepare");
  private final ProgressInfo myProgress;
  private final RestSession mySession;
  private final List<UploadUnit> myAllUnits;
  private final UploadContextImpl myContext;

  public JiraUploadContext(ProgressInfo progress, RestSession session, List<UploadUnit> units, UploadContextImpl context) {
    myProgress = progress;
    mySession = session;
    myAllUnits = Collections15.arrayList(units);
    myContext = context;
  }

  public void perform() throws CancelledException {
    preUpload(myProgress.spawn(0.2));
    performUpload(myProgress.spawnAll());
  }

  private void preUpload(ProgressInfo progress) throws CancelledException {
    progress.startActivity(A_PREPARING.create());
    EntityTransaction transaction = loadServerState();
    checkServerState(transaction);
    progress.setDone();
  }

  private void checkServerState(EntityTransaction transaction) {
    for (UploadUnit unit : Collections15.arrayList(myAllUnits)) {
      if (myContext.isFailed(unit) || unit.isDone()) {
        myAllUnits.remove(unit);
        continue;
      }
      UploadProblem problem = unit.onInitialStateLoaded(transaction, myContext);
      if (problem != null) {
        myAllUnits.remove(unit);
        LogHelper.debug("Check state detected problem", unit, problem);
        myContext.addProblem(unit, problem);
      }
    }
  }

  private EntityTransaction loadServerState() throws CancelledException {
    EntityTransaction transaction = myContext.createTransaction();
    for (UploadUnit unit : Collections15.arrayList(myAllUnits)) {
      if (myContext.isFailed(unit) || unit.isDone()) {
        myAllUnits.remove(unit);
        continue;
      }
      myProgress.checkCancelled();
      Map<UploadUnit, ConnectorException> problems;
      try {
        problems = unit.loadServerState(mySession, transaction, myContext, UploadUnit.BEFORE_UPLOAD);
      } catch (ConnectorException e) {
        problems = Collections.singletonMap(unit, e);
      }
      if (problems != null && !problems.isEmpty())
        for (Map.Entry<UploadUnit, ConnectorException> entry : problems.entrySet()) {
          UploadUnit u = entry.getKey();
          UploadProblem e = UploadProblem.exception(entry.getValue());
          LogHelper.warning("Pre load server state failed", u, e);
          myContext.addProblem(u, e);
        }
    }
    return transaction;
  }

  private void performUpload(ProgressInfo progress) throws CancelledException { // todo progress JCO-1416
    UploadOrder uploadOrder = UploadOrder.prepare(myAllUnits, progress);
    int prevCompleted = uploadOrder.getCompleteCount() -1;
    while (prevCompleted < uploadOrder.getCompleteCount()) {
      prevCompleted = uploadOrder.getCompleteCount();
      for (int b = 0; b < uploadOrder.getBlockCount(); b++) {
        Collection<UploadUnit> units = uploadOrder.startBlock(b);
        uploadUnits(units, uploadOrder, progress);
      }
      if (prevCompleted < uploadOrder.getCompleteCount()) continue;
      uploadUnits(uploadOrder.startOtherUnits(), uploadOrder, progress);
    }
    uploadOrder.logNotDone(myContext);
    progress.setDone();
  }
  private void uploadUnits(Collection<UploadUnit> units, UploadOrder uploadOrder, ProgressInfo wholeProgress) throws CancelledException {
    int prevComplete = uploadOrder.getCompleteCount() - 1;
    while (prevComplete < uploadOrder.getCompleteCount()) {
      prevComplete = uploadOrder.getCompleteCount();
      for (UploadUnit unit : units) {
        wholeProgress.checkCancelled();
        if (myContext.isFailed(unit) || unit.isDone()) {
          uploadOrder.onUnitComplete(unit);
          continue;
        }
        Collection<? extends UploadProblem> problems = uploadSingleUnit(unit);
        if (problems == null || problems.isEmpty()) {
          LogHelper.assertError(unit.isDone() || unit.isSurelyFailed(myContext), "Not done without problem", unit);
          uploadOrder.onUnitComplete(unit);
          Collection<Pair<Long,String>> masterItems = unit.getMasterItems();
          if (masterItems.size() == 1) myContext.onChangeUploaded(masterItems.iterator().next().getFirst());
        } else {
          boolean failed = myContext.isFailed(unit);
          for (UploadProblem problem : problems) {
            if (problem.isTemporary()) {
              if (!failed) uploadOrder.onUnitWaits(unit, problem);
            } else {
              myContext.addProblem(unit, problem);
              failed = true;
            }
          }
          if (failed) uploadOrder.onUnitComplete(unit);
        }
      }
    }
  }

  private Collection<? extends UploadProblem> uploadSingleUnit(UploadUnit unit) {
    Collection<? extends UploadProblem> problems;
    try {
      problems = unit.perform(mySession, myContext);
    } catch (UploadProblem.Thrown thrown) {
      problems = Collections.singleton(thrown.getProblem());
      LogHelper.assertError(problems != null, "Null problem", thrown);
    } catch (ConnectorException e) {
      problems = UploadProblem.exception(e).toCollection();
      LogHelper.warning("Upload failure", unit, e);
    }
    return problems;
  }
}
