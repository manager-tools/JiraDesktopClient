package com.almworks.jira.provider3.services.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.impl.RemoteMetaConfig;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.LongSet;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.model.SetHolderModel;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class UploadContextImpl implements UploadContext {
  private final HashMap<Long, UploadUnit.Factory> myFactories;
  private final JiraConnection3 myConnection;
  private final MessageCollector myMessages;
  private final Map<Long, UserDataHolder> myItemCache = Collections15.hashMap();
  private final UserDataHolder myUserData = new UserDataHolder();
  private final Set<UploadUnit> myFailedUnits = Collections15.hashSet();
  private final LongSet myInitialRequest;
  private final RemoteMetaConfig myRemoteMetaConfig;
  private final LongSet myUploadedPrimary = new LongSet();
  /**
   * Item to be requested to retry upload after merge
   */
  private final LongSet myPrimaryConflicts = new LongSet();
  /**
   * Items to cancel upload due to conflict
   * @see {@link com.almworks.items.sync.UploadDrain#finishUpload(long, java.util.Collection, int)} (long)}
   */
  private final LongSet myConflicts = new LongSet();

  private UploadContextImpl(HashMap<Long, UploadUnit.Factory> factories, JiraConnection3 connection, SetHolderModel<SyncProblem> problems, LongList initialRequest) {
    myFactories = factories;
    myConnection = connection;
    myMessages = new MessageCollector(this, problems);
    myInitialRequest = LongSet.copy(initialRequest);
    myRemoteMetaConfig = connection.getCustomFields().createIssueConversion();
  }

  @Override
  @NotNull
  public JiraConnection3 getConnection() {
    return myConnection;
  }

  @Override
  @NotNull
  public UserDataHolder getUserData() {
    return myUserData;
  }

  @Override
  @NotNull
  public UserDataHolder getItemCache(long item) {
    UserDataHolder cache = myItemCache.get(item);
    if (cache == null) {
      cache = new UserDataHolder();
      myItemCache.put(item, cache);
    }
    return cache;
  }

  @Override
  public boolean isFailed(UploadUnit unit) {
    if (unit == null) {
      LogHelper.error("Null unit");
      return true;
    }
    return myFailedUnits.contains(unit) || unit.isSurelyFailed(this);
  }

  @Override
  public Map<String, FieldKind> getCustomFieldKinds() {
    return myRemoteMetaConfig.getFieldKinds();
  }

  LongList getUploadedPrimary() {
    return myUploadedPrimary;
  }

  @Nullable
  UploadUnit.Factory getFactory(ItemVersion item) {
    Long type = item.getValue(DBAttribute.TYPE);
    return type != null ? myFactories.get(type) : null;
  }

  static UploadContextImpl prepare(DBReader reader, Map<DBItemType, UploadUnit.Factory> factories, JiraConnection3 connection, SetHolderModel<SyncProblem> problems, LongList initialRequest) {
    HashMap<Long, UploadUnit.Factory> loaded = Collections15.hashMap();
    for (Map.Entry<DBItemType, UploadUnit.Factory> entry : factories.entrySet()) {
      long type = reader.findMaterialized(entry.getKey());
      if (type <= 0) LogHelper.warning("Upload: not materialized", entry.getKey());
      loaded.put(type, entry.getValue());
    }
    return new UploadContextImpl(loaded, connection, problems, initialRequest);
  }

  EntityTransaction createTransaction() {
    return getConnection().getServerInfo().createTransaction();
  }

  void addProblem(UploadUnit unit, UploadProblem problem) {
    myFailedUnits.add(unit);
    addMessage(unit, problem);
    if (isConflict(problem)) {
      long item = problem.getActualItem();
      if (item > 0) myConflicts.add(item);
      else LogHelper.error("No actual item associated with a problem", unit, problem);
      Collection<Pair<Long, String>> masters = unit.getMasterItems();
      for (Pair<Long, String> pair : masters) {
        Long masterItem = pair.getFirst();
        if (isInitiallyRequested(masterItem)) myPrimaryConflicts.add(masterItem);
        else LogHelper.debug("Conflict detected for not requested masterItem", pair, unit);
      }
    }
  }

  private static boolean isConflict(UploadProblem problem) {
    return problem.getCause() == ItemSyncProblem.Cause.UPLOAD_CONFLICT;
  }

  @Override
  public void addMessage(UploadUnit unit, UploadProblem message) {
    myMessages.addMessage(unit, message);
  }

  @Nullable
  List<Pair<UploadUnit, UploadProblem>> getConflicts(long item) {
    if (!myConflicts.contains(item)) return null;
    return myMessages.selectConflicts(item);
  }

  public boolean isInitiallyRequested(long item) {
    return myInitialRequest.contains(item);
  }

  public LongList getMandatoryConflicts() {
    return myPrimaryConflicts;
  }

  void addProblems(Map<UploadUnit, ConnectorException> problems) {
    if (problems == null) return;
    for (Map.Entry<UploadUnit, ConnectorException> entry : problems.entrySet()) addProblem(entry.getKey(), entry.getValue());
  }

  void addProblem(UploadUnit unit, ConnectorException e) {
    if (unit != null && e != null) addProblem(unit, UploadProblem.exception(e));
  }

  void onChangeUploaded(long masterItem) {
    myUploadedPrimary.add(masterItem);
  }

  private static class MessageCollector {
    private final UploadContextImpl myContext;
    private final SetHolderModel<SyncProblem> myProblems;
    private final List<Pair<UploadUnit, UploadProblem>> myUnitProblems = Collections15.arrayList();
    private final LongSet myProblematicMasters = new LongSet();

    private MessageCollector(UploadContextImpl context, SetHolderModel<SyncProblem> problems) {
      myContext = context;
      myProblems = problems;
    }

    public void addMessage(UploadUnit unit, UploadProblem message) {
      Collection<SyncProblem> problems = message.toSyncProblems(myContext, unit);
      for (SyncProblem p : problems) {
        ItemSyncProblem problem = Util.castNullable(ItemSyncProblem.class, p);
        if (problem == null || !myProblematicMasters.addValue(problem.getItem())) continue;
        removeProblems(problem.getItem());
      }
      myProblems.add(problems);
      myUnitProblems.add(Pair.create(unit, message));
    }

    private void removeProblems(long item) {
      List<SyncProblem> problems = myProblems.copyCurrent();
      ArrayList<SyncProblem> toRemove = Collections15.arrayList();
      for (SyncProblem p : problems) {
        ItemSyncProblem problem = Util.castNullable(ItemSyncProblem.class, p);
        if (problem != null && item == problem.getItem()) toRemove.add(p);
      }
      myProblems.remove(toRemove);
    }

    public List<Pair<UploadUnit, UploadProblem>> selectConflicts(long item) {
      ArrayList<Pair<UploadUnit, UploadProblem>> result = Collections15.arrayList();
      for (Pair<UploadUnit, UploadProblem> pair : myUnitProblems) {
        UploadProblem problem = pair.getSecond();
        if (problem.getActualItem() != item) continue;
        if (!isConflict(problem)) continue;
        result.add(pair);
      }
      return result;
    }
  }
}
