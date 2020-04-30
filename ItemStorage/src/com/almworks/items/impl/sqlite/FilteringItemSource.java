package com.almworks.items.impl.sqlite;

import com.almworks.integers.*;
import com.almworks.items.api.DP;
import com.almworks.items.impl.dbadapter.Countable;
import com.almworks.items.impl.dbadapter.DBFilterInvalidException;
import com.almworks.items.impl.sqlite.filter.ExternalExtractionRequestSink;
import com.almworks.items.impl.sqlite.filter.ExtractFromPhysicalTableFunction;
import com.almworks.items.impl.sqlite.filter.ExtractIterableFunction;
import com.almworks.items.impl.sqlite.filter.ExtractionFunction;
import com.almworks.sqlite4java.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.Reductions;
import com.almworks.util.collections.CollectionRemove;
import com.almworks.util.commons.Condition;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class FilteringItemSource implements ItemSource, ExternalExtractionRequestSink {
  private static final FilterMaster DUMB_FILTER_MASTER = new FilterMaster() {
    public void updateFilter(FilteringItemSource source, TransactionContext context) {
    }
  };

  private static final String INSTANCE_TABLES_INFIX =
    new SimpleDateFormat("_yyMMdd_HHmmss_", Locale.US).format(new Date());
  static final String TEMP_TABLE_COLUMN = "id";
  private static final AtomicInteger ourTableCount = new AtomicInteger(0);
  private static final TypedKey<int[]> BUILDEVENT_INDEXES = TypedKey.create("BE_INDEX");
  private static final TypedKey<long[]> BUILDEVENT_PAST = TypedKey.create("BE_PAST");

  /**
   * Query processors to which this filtering item source may be attached � as a result of {@link #addListener}.
   * {@link this.addListener} will attach to all processors in the list to which we have not attached before.
   * One filtering item source can be attached to many query processors. This means a slight overhead in the overall
   * amount of jobs per transaction, because we add as many jobs to the database queue as the number of processors in the list.
   * But the actual processing will be done only once for each change of ICN, in a single job, while others will end at once.
   */
  private final QueryProcessor myQueryProcessor;
  private final FilterMaster myFilterMaster;
  private final BoolExpr<DP> myFullConstraint;
  private final Lifecycle myActiveCycle = new Lifecycle(false);
  private final Lifecycle myFilterCycle = new Lifecycle(false);
  private final Counter myCounter = new Counter();

  private final PrioritizedListeners<ClientInfo> myListeners = new PrioritizedListeners<ClientInfo>();
  private final QueryProcessor.Client myQueryProcessorClient = new QueryProcessor.Client() {
    public DatabaseJob createJob() {
      return FilteringItemSource.this.createJob();
    }

    @Override
    public String toString() {
      return "J: " + FilteringItemSource.this.toString();
    }
  };

  // --- protected with myLock ---
  private final Object myLock = new Object();
  private FilterJob myLastJob;
  private int myPriority = Integer.MIN_VALUE;
  @Nullable
  private FilteringItemSource myParent;
  @Nullable
  private BoolExpr<DP> myWorkingFilter;
  @Nullable
  private ExtractionProcessor myExtractionProcessor;

  // not protected, caching
  private Listener myParentListener;

  // confined to DB thread:
  private String myTableName;
  private long myTableIcn;
  private SQLParts mySelectCurrentSqlParts;
  private SQLParts mySelectCountSqlParts;
  private String myLastParentTableName;
  private boolean myParentRequestedReload;
  private ItemSourceUpdateEvent myParentUpdateEvent;
  private boolean myParentUpdateForced;
  private long myParentUpdateIcn;

  // processing externally generated updates
  private final Object myExternalRequestLock = new Object();
  private boolean myExtProcessRequested;
  private boolean myExtProcessRebuild;
  private final DatabaseContext myDatabaseContext;


  private FilteringItemSource(QueryProcessor queryProcessor, FilterMaster filterMaster,
    BoolExpr<DP> fullConstraint, DatabaseContext databaseContext)
  {
    myQueryProcessor = queryProcessor;
    myFilterMaster = filterMaster;
    myFullConstraint = fullConstraint;
    myDatabaseContext = databaseContext;
  }

  @Override
  public String toString() {
    ExtractionProcessor processor = myExtractionProcessor;
    FilteringItemSource parent = myParent;
    if (processor == null)
      return String.valueOf(myFullConstraint);
    return myTableName + "@" + myTableIcn + " [" + processor + (parent == null ? "]" : "] <= " + parent);
  }

  public static FilteringItemSource create(QueryProcessor processor, FilterMaster filterMaster,
    BoolExpr<DP> fullConstraint, DatabaseContext databaseContext) throws DBFilterInvalidException
  {
    return new FilteringItemSource(processor, filterMaster, fullConstraint, databaseContext);
  }

  public static FilteringItemSource createDebug(QueryProcessor processor, BoolExpr<DP> fullConstraint, DatabaseContext databaseContext, TransactionContext context) throws DBFilterInvalidException
  {
    FilteringItemSource r = new FilteringItemSource(processor, DUMB_FILTER_MASTER, fullConstraint, databaseContext);
    r.setWorkingFilter(null, fullConstraint, context);
    return r;
  }

  private DatabaseJob createJob() {
    FilterJob job = new FilterJob();
    int priority;
    synchronized (myLock) {
      priority = myPriority;
      myLastJob = job;
    }
    job.setPriority(priority);
    return job;
  }

  public void addListener(Lifespan life, int priority, Listener listener) {
    if (listener == null || life.isEnded())
      return;
    final ClientInfo ci = new ClientInfo(listener);
    boolean started;
    synchronized (myLock) {
      myListeners.add(ci, priority);
      started = myActiveCycle.cycleStart();
    }
    life.add(new Detach() {
      protected void doDetach() throws Exception {
        boolean noclients;
        FilterJob job;
        synchronized (myLock) {
          noclients = !myListeners.remove(ci);
          job = myLastJob;
        }
        if (noclients) {
          myActiveCycle.cycleEnd();
          if (job != null) {
            job.cancel();
          }
        }
      }
    });
    updatePriority();
    if (started) {
      Lifespan lifespan = myActiveCycle.lifespan();
      startFilterCycle();
      myQueryProcessor.attach(lifespan, myQueryProcessorClient);
    }
    myQueryProcessor.processClient(myQueryProcessorClient);
  }

  @ThreadSafe
  public void setPriority(int priority, final Listener listener) {
    myListeners.setPriority(priority, new Condition<ClientInfo>() {
      public boolean isAccepted(ClientInfo value) {
        return value.listener == listener;
      }
    });
    updatePriority();
  }

  private void updatePriority() {
    int priority = myListeners.getTotalPriority();
    FilterJob job = null;
    synchronized (myLock) {
      if (myPriority != priority) {
        myPriority = priority;
        job = myLastJob;
      }
    }
    if (job != null) {
      job.setPriority(priority);
    }
    // todo update priority in parent
  }

  public void setWorkingFilter(@Nullable FilteringItemSource parent, @Nullable BoolExpr<DP> filter, TransactionContext context) {
    // todo assert database thread
    assert parent != null || filter.equals(myFullConstraint) : myFullConstraint + " " + filter + " " + parent;
    assert
      parent == null || Reductions.simplify(filter.and(parent.myFullConstraint)).equals(myFullConstraint) :
      this + " " + myFullConstraint + " " + filter + " " + parent.myWorkingFilter;
    synchronized (myLock) {
      if (Util.equals(myParent, parent) && Util.equals(myWorkingFilter, filter))
        return;
      myFilterCycle.cycleEnd();
      myParent = parent;
      myWorkingFilter = filter;
      myExtractionProcessor = ExtractionProcessor.create(filter, context);
    }
  }

  // todo child FIS should listen to parent FIS
  // todo justification: parent FIS may listen not only to primary items it returns, but other stuff too

  public void process(TransactionContext context) throws SQLiteException {
    boolean success = true;
    //noinspection CatchGenericClass
    try {
      if (!myActiveCycle.isCycleStarted())
        return;
      myFilterMaster.updateFilter(this, context);

      FilteringItemSource parent;
      ExtractionProcessor processor;
      synchronized (myLock) {
        parent = myParent;
        processor = myExtractionProcessor;
      }
      if (processor == null) {
        assert false : this;
        return;
      }

      startFilterCycle();

      long icn = context.getIcn();

      boolean extProcess;
      boolean extRebuild;
      synchronized (myExternalRequestLock) {
        extProcess = myExtProcessRequested;
        extRebuild = myExtProcessRebuild;
        myExtProcessRequested = false;
        myExtProcessRebuild = false;
      }

      if (!extProcess && !isUpdateNeeded(icn) && !myParentRequestedReload && !myParentUpdateForced) {
        return;
      }

      if (parent != null && icn != myParentUpdateIcn) {
        parent.process(context);
      }

      // todo settle where myParentTableName is set, or whether we need it as a field at all
      myLastParentTableName = parent == null ? null : parent.myTableName;

      boolean parentRequestedReload = myParentRequestedReload;
      ItemSourceUpdateEvent parentUpdateEvent = myParentUpdateEvent;
      boolean parentUpdateForced = myParentUpdateForced;
      myParentRequestedReload = false;
      myParentUpdateEvent = null;
      myParentUpdateForced = false;

      long lastIcn = myTableIcn;
      ItemSourceUpdateEvent updateEvent = null;
      boolean rebuild = (extProcess && extRebuild) || myTableName == null || parentRequestedReload;
      if (!rebuild) {
        if (lastIcn != icn || extProcess || parentUpdateEvent != null) {
          updateEvent =
            buildUpdate(context, lastIcn, icn, extProcess, parentUpdateEvent, parentUpdateForced, processor);
          if (updateEvent == null) {
            rebuild = true;
          } else {
            changeTable(context, updateEvent);
          }
        }
      }
      if (rebuild) {
        rebuildTable(context, processor);
      }
      myTableIcn = icn;
      updateCount(context, updateEvent, rebuild);
      notifyClients(context, lastIcn, icn, rebuild, updateEvent, extProcess);
    } catch (SQLiteException e) {
      success = false;
      if (e instanceof SQLiteInterruptedException) {
        Log.debug(this + " dropping result [cancelled]");
      } else {
        Log.debug(this + " dropping result", e);
        throw e;
      }
    } catch (RuntimeException e) {
      success = false;
      Log.warn(this + " dropping state", e);
      throw e;
    } catch (Error e) {
      success = false;
      Log.warn(this + " dropping state", e);
      throw e;
    } finally {
      if (!success) {
        cleanUp(context);
      }
      assert myParentUpdateEvent == null : this + " " + myParentUpdateEvent;
      assert !myParentRequestedReload : this;
      assert !myParentUpdateForced : this;
    }
  }

  private void startFilterCycle() {
    if (myFilterCycle.cycleStart()) {
      FilteringItemSource parent;
      ExtractionProcessor processor;
      int priority;
      synchronized (myLock) {
        parent = myParent;
        processor = myExtractionProcessor;
        priority = myPriority;
      }
      if (processor == null)
        return;
      Lifespan life = myActiveCycle.lifespan();
      life.add(myFilterCycle.getCurrentCycleDetach(false));
//      processor.attach(life, this);
      if (parent != null) {
        parent.addListener(life, priority, getParentListener());
      }
    }
  }

  private void cleanUp(TransactionContext context) throws SQLiteException {
    String table = myTableName;
    if (table != null) {
      try {
        context.execCancellable("DROP TABLE IF EXISTS " + table);
      } catch (SQLiteException e) {
        // ignore
      }
    }
    myTableName = null;
    myTableIcn = 0;
    myLastParentTableName = null;
    myParentUpdateForced = false;
    myParentUpdateEvent = null;
    myParentRequestedReload = false;
  }

  private Listener getParentListener() {
    if (myParentListener == null) {
      myParentListener = new Listener() {
        public void reload(TransactionContext context, String idTableName) throws SQLiteException {
          if (!myActiveCycle.isCycleStarted())
            return;
          myParentRequestedReload = true;
          myParentUpdateEvent = null;
          myParentUpdateIcn = context.getIcn();
        }

        public void update(TransactionContext context, ItemSourceUpdateEvent event, String idTableName, boolean forced)
          throws SQLiteException
        {
          if (!myActiveCycle.isCycleStarted())
            return;
          if (myParentRequestedReload)
            return;
          assert myParentUpdateEvent == null : myParentUpdateEvent + " " + event;
          myParentUpdateEvent = event;
          myParentUpdateForced = forced;
          myParentUpdateIcn = context.getIcn();
        }
      };
    }
    return myParentListener;
  }

  private void changeTable(TransactionContext context, ItemSourceUpdateEvent event) throws SQLiteException {
    assert myTableName != null;
    deleteRemoved(context, event.getRemovedItemsSorted());
    insertAdded(context, event.getAddedItemsSorted());
  }

  private void deleteRemoved(TransactionContext context, LongList removed) throws SQLiteException {
    if (removed.isEmpty())
      return;
    SQLiteLongArray array = null;
    SQLiteStatement st = null;
    try {
      array = context.useArray(removed);
      st = context.prepare(
        context.sql().append("DELETE FROM ").append(myTableName).append(" WHERE id IN ").append(array.getName()));
      context.addCancellable(st);
      st.stepThrough();
    } finally {
      context.removeCancellable(st);
      if (st != null)
        st.dispose();
      if (array != null)
        array.dispose();
    }
  }

  private void insertAdded(TransactionContext context, LongList added) throws SQLiteException {
    if (added.isEmpty())
      return;
    context.checkCancelled();
    SQLiteLongArray array = null;
    SQLiteStatement stmt = null;
    try {
      array = context.useArray(added);
      SQLParts sql = context.sql()
        .append("INSERT OR IGNORE INTO ")
        .append(myTableName)
        .append(" (id) SELECT value FROM ")
        .append(array.getName());
      stmt = context.prepare(sql);
      stmt.stepThrough();
    } finally {
      if (stmt != null) {
        stmt.dispose();
      }
      if (array != null) {
        array.dispose();
      }
    }
  }

  private void rebuildTable(TransactionContext context, ExtractionProcessor processor) throws SQLiteException {
    String name = myTableName;
    if (name == null) {
      myTableName = name = "sr" + INSTANCE_TABLES_INFIX + ourTableCount.incrementAndGet();
      mySelectCountSqlParts = null;
    }

    context.execCancellable("DROP TABLE IF EXISTS " + name);
    context.execCancellable("CREATE TEMP TABLE " + name + " (" + TEMP_TABLE_COLUMN + " INTEGER NOT NULL PRIMARY KEY)");
    ExtractionFunction input = myLastParentTableName == null ? null :
      new ExtractFromPhysicalTableFunction(myLastParentTableName, TEMP_TABLE_COLUMN, null);
    processor.insertInto(context, name, TEMP_TABLE_COLUMN, input);
  }

  /**
   * If null is returned, rebuild is required
   */
  @Nullable
  private ItemSourceUpdateEvent buildUpdate(TransactionContext context, long lastIcn, long newIcn, boolean extProcess,
    ItemSourceUpdateEvent parentUpdateEvent, boolean parentUpdateForced, ExtractionProcessor processor)
    throws SQLiteException
  {
    if (lastIcn >= newIcn && !extProcess && !parentUpdateForced) {
      assert false : lastIcn + " " + newIcn;
      return null;
    }
    assert newIcn == context.getIcn();
    LongList changed = LongList.EMPTY;
    if (parentUpdateEvent != null) {
      changed = parentUpdateEvent.getUpdatedItemsSorted();
    } else if (lastIcn < newIcn) {
      changed = context.getChangedItemsSorted(lastIcn);
      if (changed.isEmpty())
        return null;
    }

    // todo pass in cutoff number of issues - will stop loading and go for rebuild if the number is greater
    // todo use reusable array from context
    LongSetBuilder additionalBuilder = new LongSetBuilder();
    boolean ok = true;//processor.loadIndirectlyUpdated(context, changed, additionalBuilder, myLastParentTableName);
    if (!ok) {
      // rebuild called 
      return null;
    }
    if (extProcess) {
      ok = true;//processor.loadExternallyUpdated(context, additionalBuilder, myLastParentTableName);
      if (!ok)
        return null;
    }
    if (parentUpdateEvent != null) {
      // todo when designed more effective buildAffectingFilter for additional items, revisit here
      LongList parentAdded = parentUpdateEvent.getAddedItemsSorted();
      LongList parentRemoved = parentUpdateEvent.getRemovedItemsSorted();
      additionalBuilder.mergeFromSortedCollection(parentAdded);
      additionalBuilder.mergeFromSortedCollection(parentRemoved);
    }

    LongList additional = additionalBuilder.isEmpty() ? null : additionalBuilder.commitToArray();

    // there are three sources of updates: parent FIS (or change record) for updates of direct changes
    // change record for updates of indirect changes
    // external subscription - indirect changes not related to the database

//    LongList bitmaps = change.getBitmaps();

    /// todo fix building non-affecting changes
    // see testCFLoadMultiSelect
//    bitmaps = null;

//    @Nullable List<ChangeSpec> specList = change.getChangeSpecs();
//    long mask = specList == null ? -1L : processor.getUpdateMask(context, specList);
    IntIterator affecting;
    IntIterator nonAffecting;
//    if (mask == 0L) {
//      // noone is interested in any changes
//      affecting = IntIterator.EMPTY;
//      nonAffecting = new IntProgression.ArithmeticIterator(0, 1, changed.size());
//    } else if (bitmaps == null || mask == -1L) {
//      // changes are applied to every item OR at least someone is interested in every change
    affecting = new IntProgression.ArithmeticIterator(0, 1, changed.size());
    nonAffecting = IntIterator.EMPTY;
//    } else {
//      // something in between
//      assert bitmaps.size() == changed.size() : bitmaps.size() + " " + changed.size();
//      affecting = new MaskedIndexIterator(bitmaps, mask, false);
//      nonAffecting = new MaskedIndexIterator(bitmaps, mask, true);
//    }

    // todo inspect if it is more effective to go to rebuild

    Map sessionCache = context.getSessionContext().getSessionCache();
    ItemSourceUpdateEventBuilder eventBuilder = ItemSourceUpdateEventBuilder.getFrom(sessionCache);
//    eventBuilder.setSpecList(specList);
    // if table is relatively small, it's better be fully loaded
    @Nullable LongArray loaded = maybeLoadCurrentTable(context, eventBuilder);
    if (loaded != null)
      loaded.sortUnique();
    // todo replace with arrays
    int[] indexBuf = getTempStorage(BUILDEVENT_INDEXES, sessionCache, /*SQLUtil.BIND_PARAMS_COUNT*/ 40);
    long[] pastIdsBuf = getLongTempStorage(BUILDEVENT_PAST, sessionCache, /*SQLUtil.BIND_PARAMS_COUNT*/ 40);
    buildAffectingFilter(context, changed, affecting, eventBuilder, loaded, indexBuf, pastIdsBuf, processor);
    if (additional != null) {
      // todo exclude from additional items from affecting
      IntListIterator ii = IntProgression.arithmetic(0, additional.size()).iterator();
      buildAffectingFilter(context, additional, ii, eventBuilder, loaded, indexBuf, pastIdsBuf, processor);
    }
    // todo exclude from nonAffecting items from additional
//    buildNotAffectingFilter(context, bitmaps, changed, specList, nonAffecting, eventBuilder, loaded, indexBuf, pastIdsBuf);
    ItemSourceUpdateEvent r = eventBuilder.createEvent();
    eventBuilder.cleanUp();
    return r;
  }

/*
  private boolean isRebuildPreferredToUpdate(ItemChange change) {
    // todo presuming that myParent will be refactored and confined to this thread
    FilteringItemSource parent = myParent;
    if (parent == null)
      return false;
    int parentCount = parent.getCountable().getCount();
    if (parentCount < 0)
      return false;
    return change.getChangedItemsSorted().size() > parentCount * REBUILD_RATIO;
  }
*/

/*
  private void buildNotAffectingFilter(TransactionContext context, LongList bitmaps, IntList ids,
    List<ChangeSpec> specList, IntIterator notAffectingIndexes, ItemSourceUpdateEventBuilder eventBuilder,
    IntList currentIds, int[] indexes, int[] sortedPastIds) throws SQLiteException
  {
    if (!notAffectingIndexes.hasNext())
      return;
    SQLiteStatement loadCurrent = SQLiteStatement.DISPOSED;
    try {
      if (currentIds == null)
        loadCurrent = context.prepare(getSelectCurrentSqlParts());
      ItemSourceUpdateEventBuilder.ChangedCollector chcol = eventBuilder.getChangedCollector();
      while (notAffectingIndexes.hasNext()) {
        loadIndexesFrom(notAffectingIndexes, indexes);
        loadCurrentIdsSorted(ids, indexes, currentIds, loadCurrent, sortedPastIds);
        // future = past, since filter is not affected
        IntList sortedFutureId = new IntArray(sortedPastIds);
        // todo since future = past, use a more effective method than addChanges
        addChanges(sortedPastIds, sortedFutureId, eventBuilder, ids, indexes, bitmaps, specList, chcol);
      }
      eventBuilder.mergeCollector(chcol);
    } finally {
      loadCurrent.dispose();
    }
  }
*/

  private void loadIndexesFrom(IntIterator ii, int[] target) {
    for (int i = 0; i < target.length; i++) {
      target[i] = ii.hasNext() ? ii.nextValue() : -1;
    }
  }

  private void buildAffectingFilter(TransactionContext context, LongList ids, IntIterator affectingIndexes,
    ItemSourceUpdateEventBuilder eventBuilder, LongList currentIds, int[] indexes, long[] sortedPastIds,
    ExtractionProcessor processor) throws SQLiteException
  {
    if (!affectingIndexes.hasNext())
      return;
    SQLiteStatement loadCurrent = SQLiteStatement.DISPOSED;
    try {
      if (currentIds == null)
        loadCurrent = context.prepare(getSelectCurrentSqlParts());
      LongList sortedFutureIds;
      LongSetBuilder futureBuilder = new LongSetBuilder();
      LongIterable indexedIds = new IndexedLongs(ids, indexes);
      ExtractionFunction input = myLastParentTableName == null ? new ExtractIterableFunction(indexedIds) :
        new ExtractFromPhysicalTableFunction(myLastParentTableName, TEMP_TABLE_COLUMN, indexedIds);
      while (affectingIndexes.hasNext()) {
        // todo current implementation has fixed memory footprint
        // todo research: it may be faster and worth extra mem to load everything into memory and compare,
        // todo not cycling through bind
        loadIndexesFrom(affectingIndexes, indexes);
        loadCurrentIdsSorted(ids, indexes, currentIds, loadCurrent, sortedPastIds);
        futureBuilder.clear(true);
        processor.loadItems(context, futureBuilder, input);
        sortedFutureIds = futureBuilder.toList();
        addChanges(sortedPastIds, sortedFutureIds, eventBuilder);
        if (loadCurrent != SQLiteStatement.DISPOSED)
          loadCurrent.reset();
      }
    } finally {
      loadCurrent.dispose();
    }
  }

  private static int[] getTempStorage(TypedKey<int[]> key, Map context, int size) {
    int[] r = key.getFrom(context);
    if (r == null || r.length != size) {
      r = new int[size];
      key.putTo(context, r);
    }
    return r;
  }

  private static long[] getLongTempStorage(TypedKey<long[]> key, Map context, int size) {
    long[] r = key.getFrom(context);
    if (r == null || r.length != size) {
      r = new long[size];
      key.putTo(context, r);
    }
    return r;
  }

  private static void loadCurrentIdsSorted(LongList ids, int[] indexes, LongList currentIds, SQLiteStatement statement,
    long[] buffer) throws SQLiteException
  {
    assert buffer.length >= indexes.length;
    assert currentIds == null || currentIds.isSortedUnique();
    Arrays.fill(buffer, -1);
    int j = 0;
    for (int i = 0; i < indexes.length; i++) {
      int ind = indexes[i];
      if (ind == -1) {
        break;
      }
      long itemId = ids.get(ind);
      if (currentIds != null) {
        // todo may be more optimal than contains(), as itemId are sorted - search from the position of the last occurence; but IntSet is likely to be replaced
        if (currentIds.binarySearch(itemId) >= 0) {
          buffer[j++] = itemId;
        }
      } else {
        statement.bind(++j, itemId);
      }
    }
    if (currentIds == null) {
      while (j < buffer.length)
        statement.bind(++j, -1);
      int loaded = statement.loadLongs(0, buffer, 0, buffer.length);
      // todo here - load longs
      Arrays.sort(buffer, 0, loaded);
      statement.reset();
    }
  }

  @Nullable
  private LongArray maybeLoadCurrentTable(TransactionContext context, ItemSourceUpdateEventBuilder eventBuilder) {
    // todo if the current table is small enough, get reusable temp storage from eventBuilder and load all ids into it
    return null;
  }

  private void addChanges(long[] sortedPastIds, LongList sortedFutureId, ItemSourceUpdateEventBuilder builder) {
    assert isSortedWithTail(sortedPastIds);
    assert isSortedWithTail(sortedFutureId.toNativeArray());
    int nextP = 0, nextF = 0, nextI = 0;
    int pastLength = sortedPastIds.length;
    while (pastLength > 0 && sortedPastIds[pastLength - 1] < 0)
      pastLength--;
    int futureLength = sortedFutureId.size();
    while (nextP < pastLength || nextF < futureLength) {
      long pid = nextP < pastLength ? sortedPastIds[nextP] : -1;
      long fid = nextF < futureLength ? sortedFutureId.get(nextF) : -1;
      if (pid < 0 && fid < 0) {
        break;
      } else if (pid >= 0 && (fid < 0 || pid < fid)) {
        builder.addRemoved(pid);
        nextP++;
      } else if (fid >= 0 && (pid < 0 || fid < pid)) {
        builder.addInserted(fid);
        nextF++;
      } else {
        assert pid == fid && pid >= 0 && fid >= 0 : pid + " " + fid + " " + nextP + " " + nextF;
        builder.addChanged(pid);
        nextP++;
        nextF++;
      }
    }
  }

  private boolean isSortedWithTail(long[] sortedPastIds) {
    int i = ArrayUtil.indexOf(sortedPastIds, -1);
    if (i < 0)
      return LongCollections.isSorted(sortedPastIds);
    else
      return LongCollections.isSorted(sortedPastIds, 0, i);
  }

  private void notifyClients(TransactionContext context, long lastIcn, long newIcn, boolean rebuild,
    ItemSourceUpdateEvent updateEvent, boolean extProcess) throws SQLiteException
  {
    // currently listeners are notified in the order of priority, as seen at the beginning of this method
    // but while one listener is running, priorities may change and listeners may appear/disappear
    // todo notify the most important listener, then calculate most important listener among all not notified yet, etc
    for (ClientInfo ci : myListeners) {
      notifyClient(ci, context, lastIcn, newIcn, rebuild, updateEvent, extProcess);
    }
  }

  private void notifyClient(ClientInfo ci, TransactionContext context, long lastIcn, long newIcn, boolean rebuild,
    ItemSourceUpdateEvent updateEvent, boolean extProcess) throws SQLiteException
  {
    try {
      long clientIcn = ci.lastIcn;
      if (clientIcn != lastIcn || rebuild) {
        ci.listener.reload(context, myTableName);
      } else if (clientIcn != newIcn || extProcess) {
        assert updateEvent != null;
        assert clientIcn == lastIcn;
        // updateEvent is sent only if listener ICN is in sync with this ICN, and if we have the update event
        if (!updateEvent.isEmpty()) {
          ci.listener.update(context, updateEvent, myTableName, extProcess);
        }
      } else {
        assert lastIcn == newIcn;
      }
      ci.lastIcn = myTableIcn;
    } catch (SQLiteInterruptedException e) {
      // the client cancels some selects
      // todo what if client cancelled an UPDATE, transaction is ruined then
      ci.lastIcn = 0;
    }
  }

  private boolean isUpdateNeeded(long icn) {
    for (ClientInfo ci : myListeners) {
      if (ci.lastIcn != icn)
        return true;
    }
    return false;
  }

  public SQLParts getSelectCurrentSqlParts() {
    assert myTableName != null;
    if (mySelectCurrentSqlParts == null) {
      mySelectCurrentSqlParts = createSelectCurrentSqlParts();
    }
    return mySelectCurrentSqlParts;
  }

  private SQLParts createSelectCurrentSqlParts() {
    return new SQLParts().append("SELECT id FROM ")
      .append(myTableName)
      .append(" WHERE id IN (")
      .appendParams(40)
//      .appendParams(SQLUtil.BIND_PARAMS_COUNT) // todo replace with arrays
      .append(")");
  }

  /**
   * Updates the item count during the current transaction.
   */
  private void updateCount(@NotNull TransactionContext context, ItemSourceUpdateEvent updateEvent, boolean rebuild)
    throws SQLiteException
  {
    int count;
    int oldCount = myCounter.getCount();
    if (oldCount < 0 || rebuild) {
      count = calcCount(context);
    } else if (updateEvent != null) {
      count = oldCount - updateEvent.getRemovedItemsSorted().size() + updateEvent.getAddedItemsSorted().size();
    } else {
      // no changes
      return;
    }
    myCounter.setCount(count);
  }

  private int calcCount(TransactionContext context) throws SQLiteException {
    int count;
    SQLParts countParts = getSelectCountSqlParts();
    SQLiteStatement statement = context.prepare(countParts);
    try {
      boolean success = statement.step();
      assert success : "count statement failed";
      assert statement.columnCount() == 1;
      count = statement.columnInt(0);
    } finally {
      statement.dispose();
    }
    return count;
  }

  private SQLParts getSelectCountSqlParts() {
    assert myTableName != null : myTableName;
    if (mySelectCountSqlParts == null) {
      mySelectCountSqlParts = new SQLParts("SELECT COUNT(*) FROM " + myTableName);
    }
    return mySelectCountSqlParts;
  }

  @NotNull
  public Countable getCountable() {
    return myCounter;
  }

  public void requestProcessing(boolean rebuild) {
    synchronized (myExternalRequestLock) {
      if (!myExtProcessRequested || (!myExtProcessRebuild && rebuild)) {
        myExtProcessRequested = true;
        myExtProcessRebuild = rebuild;
        externalJob();
      }
    }
  }

  private void externalJob() {
    myQueryProcessor.processClient(myQueryProcessorClient);
  }

  private static class ClientInfo {
    private final Listener listener;
    private long lastIcn;

    public ClientInfo(Listener listener) {
      this.listener = listener;
    }
  }


  private class FilterJob extends DatabaseJob {
    protected void dbrun(TransactionContext context) throws Exception {
      process(context);
    }

    protected void handleFinished(boolean success) {
      super.handleFinished(success);
      synchronized (myLock) {
        if (myLastJob == this) {
          myLastJob = null;
        }
      }
    }

    public TransactionType getTransactionType() {
      return TransactionType.READ_COMMIT;
    }
  }


  /**
   * Contains current count value for the items in this filtering item source.
   */
  private class Counter implements Countable {
    /**
     * Number of items in this filtering source.
     */
    private int myCount = -1;
    /**
     * Maps countable listeners to the FilteringItemSource listeners, from which they get the events.
     */
    private Map<CountableListener, ItemSource.Listener> myListenersMap;

    public synchronized int getCount() {
      return myCount;
    }

    public synchronized void setCount(int count) {
      myCount = count;
    }

    public void addListener(Lifespan life, int priority, final CountableListener listener) {
      if (life.isEnded())
        return;
      ItemSource.Listener itemSourceListener = new ItemSource.Listener() {
        public void reload(TransactionContext context, String idTableName) {
          listener.onCountChanged();
        }

        public void update(TransactionContext context, ItemSourceUpdateEvent event, String idTableName,
          boolean forced)
        {
          listener.onCountChanged();
        }
      };
      synchronized (this) {
        if (myListenersMap == null) {
          myListenersMap = new HashMap<CountableListener, Listener>();
        }
        myListenersMap.put(listener, itemSourceListener);
        life.add(CollectionRemove.create(myListenersMap.keySet(), listener, null));
      }
      FilteringItemSource.this.addListener(life, priority, itemSourceListener);
    }

    public void setPriority(int priority, CountableListener listener) {
      if (myListenersMap != null) {
        Listener itemSourceListener = getItemSourceListener(listener);
        FilteringItemSource.this.setPriority(priority, itemSourceListener);
      }
    }

    private synchronized ItemSource.Listener getItemSourceListener(CountableListener countableListener) {
      return myListenersMap.get(countableListener);
    }
  }


  private static class IndexedLongs implements LongIterable {
    private LongList myIds;
    private int[] myIndexes;

    public IndexedLongs(LongList ids, int[] indexes) {
      myIds = ids;
      myIndexes = indexes;
    }

    public LongIterator iterator() {
      return new AbstractLongIterator() {
        private int myNext;
        private boolean myIterated;

        public boolean hasNext() throws ConcurrentModificationException, NoSuchElementException {
          return myNext < myIndexes.length && myIndexes[myNext] >= 0;
        }

        @Override
        public boolean hasValue() {
          return myIterated;
        }

        public LongIterator next() throws ConcurrentModificationException, NoSuchElementException {
          if (!hasNext())
            throw new NoSuchElementException();
          myNext++;
          myIterated = true;
          return this;
        }

        public long value() throws NoSuchElementException {
          if (!myIterated)
            throw new NoSuchElementException();
          return myIds.get(myIndexes[myNext-1]);
        }
      };
    }
  }
}
