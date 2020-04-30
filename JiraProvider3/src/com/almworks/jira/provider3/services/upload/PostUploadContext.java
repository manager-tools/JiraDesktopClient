package com.almworks.jira.provider3.services.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.recentitems.RecentItemsService;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.UserDataHolder;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PostUploadContext implements UploadContext {
  private final ProgressInfo myProgress;
  private final RestSession mySession;
  private final List<UploadUnit> myUnits;
  private final UploadContextImpl myConfig;
  private final TLongObjectHashMap<Done> myDoneUpload = new TLongObjectHashMap<>();

  public PostUploadContext(ProgressInfo progress, RestSession session, List<UploadUnit> units, UploadContextImpl config) {
    myProgress = progress;
    mySession = session;
    myUnits = units;
    myConfig = config;
  }

  FinishUploadTransaction perform() {
    EntityTransaction transaction = loadServerState(myProgress.spawn(0.9));
    for (UploadUnit unit : myUnits) unit.finishUpload(transaction, this);
    RecentItemsService recentService = myConfig.getConnection().getActor(RecentItemsService.ROLE);
    return new FinishUploadTransaction(myDoneUpload, transaction, myConfig, recentService);
  }

  private EntityTransaction loadServerState(ProgressInfo progress) {
    EntityTransaction transaction = myConfig.createTransaction();
    for (int i = 0, myUnitsSize = myUnits.size(); i < myUnitsSize; i++) {
      UploadUnit unit = myUnits.get(i);
      try {
        Map<UploadUnit, ConnectorException> problems = unit.loadServerState(mySession, transaction, myConfig, UploadUnit.AFTER_UPLOAD);
        myConfig.addProblems(problems);
      } catch (ConnectorException e) {
        myConfig.addProblem(unit, e);
      }
      progress.spawn(1.0 / (myUnitsSize - i)).setDone();
    }
    progress.setDone();
    return transaction;
  }

  @Override
  @NotNull
  public JiraConnection3 getConnection() {
    return myConfig.getConnection();
  }

  @Override
  @NotNull
  public UserDataHolder getUserData() {
    return myConfig.getUserData();
  }

  @Override
  @NotNull
  public UserDataHolder getItemCache(long item) {
    return myConfig.getItemCache(item);
  }

  @Override
  public boolean isFailed(UploadUnit unit) {
    return myConfig.isFailed(unit);
  }

  @Override
  public Map<String, FieldKind> getCustomFieldKinds() {
    return myConfig.getCustomFieldKinds();
  }

  @Override
  public void addMessage(UploadUnit unit, UploadProblem message) {
    myConfig.addMessage(unit, message);
  }

  /**
   * Report upload of single attribute of an item
   * @see com.almworks.items.sync.UploadDrain#finishUpload(long, java.util.Collection, int)
   */
  public void reportUploaded(long item, DBAttribute<?> attribute) {
    getOrAddDone(item).add(attribute);
  }

  @NotNull
  private Done getOrAddDone(long item) {
    Done done = myDoneUpload.get(item);
    if (done == null) {
      done = new Done();
      myDoneUpload.put(item, done);
    }
    return done;
  }

  /**
   * Reports upload of single history step for an item
   */
  public void reportHistory(long item, int stepIndex, UploadUnit historyUnit) {
    getOrAddDone(item).addStep(stepIndex, historyUnit);
  }

  static class Done {
    private final List<DBAttribute<?>> myAttributes = Collections15.arrayList(4);
    private final IntArray myStepIndexes = new IntArray();
    private final List<UploadUnit> myUnits = Collections15.arrayList();
    private int myStepCount = -1;

    public void add(DBAttribute<?> attribute) {
      myAttributes.add(attribute);
    }

    public List<DBAttribute<?>> getAttributes() {
      return myAttributes;
    }

    public int getStepsCount() {
      if (myStepCount < 0) {
        myStepIndexes.sortUnique();
        myStepCount = 0;
        for (IntIterator cursor : myStepIndexes) {
          int step = cursor.value();
          if (step != myStepCount) LogHelper.error("Missing done step", myStepCount, step, myStepIndexes);
          myStepCount++;
        }
      }
      return myStepCount;
    }

    public void addStep(int stepIndex, UploadUnit historyUnit) {
      if (myStepIndexes.contains(stepIndex)) LogHelper.error("Step already done", stepIndex, myStepIndexes);
      else {
        myStepIndexes.add(stepIndex);
        myUnits.add(historyUnit);
        myStepCount = -1;
      }
    }

    public List<UploadUnit> getHistoryUnits() {
      return Collections.unmodifiableList(myUnits);
    }
  }
}
