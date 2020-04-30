package com.almworks.items.api;

import com.almworks.integers.IntArray;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongListIterator;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Functional;
import com.almworks.util.commons.LongObjFunction2;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongProcedure;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class DBLiveQueryResolutionTests extends MemoryDatabaseFixture {
  public static final DBAttribute<Integer> NUM = DBAttribute.Int("num", "Number");
  public static final DBAttribute<Boolean> SEPARATOR = DBAttribute.Bool("separator", "Separator");
  public static final BoolExpr<DP> ALL_ITEMS = DPNotNull.create(SEPARATOR);
  public static final BoolExpr<DP> INVARIANT_ITEMS = DPEquals.create(SEPARATOR, true);
  public static final int SUM = 10000;
  public static final int MAX_ABS_NUM = 100;

  private final TestDP predicate = new TestDP();

  public void test() {
    initItems();
    initCheck();
    Runnable[] tests = new Runnable[] {
      changeResolution,
      changeResolutionAndItems,
      changeItems,
      addListener
    };
    boolean[] testRun = new boolean[tests.length];
    int maxn = 10 * tests.length;
    int choiceSlice = 100000;
    int space = choiceSlice * tests.length;
    for (int n = 0; n < maxn || !and(testRun); ++n) {
      Log.debug("Running test " + n);
      // distribution doesn't have to be uniform --- it's sufficient that all choices appear
      int choice = getRandom().nextInt(space);
      int testNum = choice / choiceSlice;
      tests[testNum].run();
      testRun[testNum] = true;
    }
    flushWriteQueue();
    flushReadQueue(DBPriority.FOREGROUND);
  }

  private void initItems() {
    assertEquals(0, SUM % 10);
    writeNoFail(new Procedure<DBWriter>() {
      @Override
      public void invoke(DBWriter writer) {
        int prevSum = 0;
        int partSum = SUM / 10;
        for (int i = 0; i < 100; ++i) {
          long item = writer.nextItem();
          writer.setValue(item, SEPARATOR, i < 50);
          int num;
          if (i < 50 && (i % 5) == 4) {
            num = partSum - prevSum;
            prevSum = 0;
          } else {
            num = createNum();
            prevSum += num;
          }
          writer.setValue(item, NUM, num);
        }
      }
    });
  }

  private int createNum() {
    return getRandom().nextInt(MAX_ABS_NUM * 2) - MAX_ABS_NUM;
  }

  private void initCheck() {
    db.liveQuery(Lifespan.FOREVER, predicate.term(), new InvariantCheckingListener());
//    flushReadQueue(DBPriority.FOREGROUND);
    predicate.doFlip();
    flushReadQueue(DBPriority.FOREGROUND);
  }

  public static boolean and(boolean[] bools) {
    for (boolean b : bools) if (!b) return false;
    return true;
  }

  Runnable changeResolution = new Runnable() {
    @Override
    public void run() {
      Log.debug("Test: changeResolution");
      predicate.doFlip();
    }
  };

  Runnable changeResolutionAndItems = new Runnable() {
    @Override
    public void run() {
      Log.debug("Test: changeResolutionAndItems");
      db.writeForeground(new WriteTransaction() {
        @Override
        public Object transaction(DBWriter writer) throws DBOperationCancelledException {
          changeItems(writer);
          return null;
        }
      }).onFailure(ThreadGate.STRAIGHT, Functional.<DBResult>ignore(failRunnable))
        .onSuccess(ThreadGate.STRAIGHT, new Procedure() {
          @Override
          public void invoke(Object arg) {
            predicate.doFlip();
          }
        });//.waitForCompletion();
    }
  };

  Runnable changeItems = new Runnable() {
    @Override
    public void run() {
      Log.debug("Test: changeItems");
      writeNoFail(new Procedure<DBWriter>() {
        @Override
        public void invoke(DBWriter writer) {
          changeItems(writer);
        }
      });
    }
  };

  private void changeItems(DBWriter writer) {
    LongArray notInv = writer.query(ALL_ITEMS.and(INVARIANT_ITEMS.negate())).copyItemsSorted();
    LongArray inv = writer.query(INVARIANT_ITEMS).copyItemsSorted();
    int delta = 0;
    if (getRandom().nextBoolean()) {
      Log.debug("Add");
      delta = changeItemSet(writer, notInv, true, delta);
    }
    if (getRandom().nextBoolean()) {
      Log.debug("Remove");
      delta = changeItemSet(writer, inv, false, delta);
    }
    changeItemsRestoreInvariant(writer, inv, delta);
    assertEquals(SUM, currentSum(writer));
  }

  private int changeItemSet(DBWriter writer, LongArray set, boolean separator, int delta) {
    for (int i = 0, maxi = set.size() / 3; i < maxi; ++i) {
      int idx = getRandom().nextInt(set.size());
      long item = set.removeAt(idx);
      writer.setValue(item, SEPARATOR, separator);
      delta += (separator ? -1 : +1) * writer.getValue(item, NUM);
    }
    return delta;
  }

  private void changeItemsRestoreInvariant(DBWriter writer, LongArray inv, int delta) {
    int invlen = inv.size();
    int len = invlen / 4;
    IntArray changeIdx = new IntArray(len);
    IntArray restoreIdx = new IntArray(len);
    for (int i = 0; i < len; ++i) {
      changeIdx.add(getRandom().nextInt(invlen));
    }
    changeIdx.sortUnique();
    for (int i = 0; i < len; ++i) {
      int idx;
      do idx = getRandom().nextInt(invlen); while (changeIdx.contains(idx));
      restoreIdx.add(idx);
    }
    // change items and accumulate delta
    for (int i = 0; i < len; ++i) {
      int idx = changeIdx.get(i);
      long item = inv.get(idx);
      int oldNum = writer.getValue(item, NUM);
      int newNum = createNum();
      writer.setValue(item, NUM, newNum);
      delta += oldNum - newNum;
    }
    // restore invariant dividing delta between restoreIdx items
    for (int i = 0; i < len; ++i) {
      int idx = restoreIdx.get(i);
      long item = inv.get(idx);
      int num = writer.getValue(item, NUM) + (delta / len + (i == len - 1 ? delta % len : 0));
      writer.setValue(item, NUM, num);
    }
  }

  private static int currentSum(final DBReader reader) {
    return reader.query(INVARIANT_ITEMS).fold(0, new LongObjFunction2<Integer>() {
      @Override
      public Integer invoke(long a, Integer b) {
        return reader.getValue(a, NUM) + b;
      }
    });
  }

  Runnable addListener = new Runnable() {
    @Override
    public void run() {
      boolean changeFlip = getRandom().nextBoolean();
      Log.debug("Test: add listener. flip change: " + changeFlip);
      db.liveQuery(Lifespan.FOREVER, predicate.term(), new InvariantCheckingListener());
      if (changeFlip) predicate.doFlip();
    }
  };


  private class TestDP extends DP {
    private boolean myFlipped = false;
    private final FireEventSupport<ChangeListener> myEventSupport = FireEventSupport.create(ChangeListener.class);

    @Override
    public boolean accept(long item, DBReader reader) {
      fail();
      return false;
    }

    @Override
    protected boolean equalDP(DP other) {
      return other.getClass() == TestDP.class;
    }

    @Override
    protected int hashCodeDP() {
      return 1237;
    }

    public void doFlip() {
      synchronized (this) {
        myFlipped = !myFlipped;
      }
      myEventSupport.getDispatcher().onChange();
    }

    public synchronized boolean flipP() {
      return myFlipped;
    }

    @Override
    public BoolExpr<DP> resolve(DBReader reader, @Nullable ResolutionSubscription subscription) {
      if (subscription != null) {
        myEventSupport.addListener(subscription.getLife(), ThreadGate.STRAIGHT, subscription);
      }
      return DPEquals.create(SEPARATOR, !flipP());
    }
  }

  private class InvariantCheckingListener implements DBLiveQuery.Listener {
    private final TLongLongHashMap myMap = new TLongLongHashMap();

    @Override
    public void onICNPassed(long icn) {
    }

    @Override
    public synchronized void onDatabaseChanged(DBEvent event, final DBReader reader) {
      updateMap(event, reader);
      boolean flip = isFlipped(reader);
      TLongLongHashMap map = selectMap(flip, reader);
      checkInvariant(map);
    }

    private void checkInvariant(TLongLongHashMap map) {
      final int[] sum = new int[] {0};
      map.forEachValue(new TLongProcedure() {
        @Override
        public boolean execute(long value) {
          sum[0] += value;
          return true;
        }
      });
      assertEquals(SUM, sum[0]);
      Log.debug("Checked invariant");
    }

    private TLongLongHashMap selectMap(boolean flip, final DBReader reader) {
      TLongLongHashMap map;
      if (!flip) {
        map = myMap;
      } else {
        map = reader.query(ALL_ITEMS).fold(new TLongLongHashMap(), new LongObjFunction2<TLongLongHashMap>() {
          @Override
          public TLongLongHashMap invoke(long item, TLongLongHashMap flippedMap) {
            if (!myMap.containsKey(item)) flippedMap.put(item, reader.getValue(item, NUM));
            return flippedMap;
          }
        });
      }
      return map;
    }

    private boolean isFlipped(final DBReader reader) {
      final Boolean[] flip = new Boolean[] {null};
      final String mapState = Arrays.toString(myMap.keys()) + " x " + Arrays.toString(myMap.getValues());
      myMap.forEachKey(new TLongProcedure() {
        @Override
        public boolean execute(long item) {
          Boolean sepValue = reader.getValue(item, SEPARATOR);
          assertNotNull(sepValue);
          if (flip[0] != null) {
            assertEquals(mapState, (Boolean)flip[0], (Boolean)!sepValue);
          }
          flip[0] = !sepValue;
          return true;
        }
      });
      return flip[0];
    }

    private void updateMap(DBEvent event, DBReader reader) {
      for (LongListIterator i = event.getAddedAndChangedSorted().iterator(); i.hasNext();) {
        long item = i.nextValue();
        myMap.put(item, reader.getValue(item, NUM));
      }
      for (LongListIterator i = event.getRemovedSorted().iterator(); i.hasNext();) {
        myMap.remove(i.nextValue());
      }
    }
  }
}
