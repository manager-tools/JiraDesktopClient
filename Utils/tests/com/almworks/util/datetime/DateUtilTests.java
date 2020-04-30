package com.almworks.util.datetime;

import com.almworks.integers.func.IntProcedure;
import com.almworks.util.Pair;
import com.almworks.util.tests.BaseTestCase;
import junit.framework.AssertionFailedError;
import org.almworks.util.Const;
import org.almworks.util.Failure;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.almworks.util.collections.Containers.complement;
import static org.almworks.util.Collections15.*;

/**
 * @author Vasya
 */
public class DateUtilTests extends BaseTestCase {
  private final long D = Const.DAY, H = Const.HOUR, C = 100*365*D, M = 10*C;
  /**
   * In some time-zones one day was skipped due to UTC offset was changed (like -10:00 to +14:00 in Pacific/Fakaofo). This leads to one day has never been in this time-zones<br><br>
   * One example see here:<br> https://wiki.almworks.com/pages/viewpage.action?pageId=12878315 (Вечер с четверга на субботу 30 Dec 2011)
   */
  private static final String[] DAY_SKIPPERS = new String[] {"Africa/Kwajalein", "Kwajalein", "Pacific/Kwajalein", "Pacific/Enderbury", "Pacific/Kiritimati", "Pacific/Apia", "MIT", "Pacific/Fakaofo"};

  @Override
  protected void tearDown() throws Exception {
    DateUtil.clearCaches();
    super.tearDown();
  }

  public void testFriendlyView() {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    assertEquals("n/a", DateUtil.toFriendlyDateTime(null));
    assertEquals("n/a", DateUtil.toFriendlyDateTime(new Date(0)));
    assertEquals("<1 min ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 59 * 1000), format, true));
    assertEquals("4 min ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 299 * 1000), format, true));
    assertEquals("5 min ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 300 * 1000), format, true));
    assertEquals("5 min ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 301 * 1000), format, true));
    assertEquals("59 min ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 3599900), format, true));
    assertEquals("1 hr ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 3600000), format, true));
    assertEquals("1 hr ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 3600001), format, true));
    assertEquals("1 hr ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 7199900), format, true));
    assertEquals("2 hrs ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 7200000), format, true));
    assertEquals("2 hrs ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 7200001), format, true));
    assertEquals("23 hrs ago",
      DateUtil.toFriendlyDateTime(new Date(now() - 24 * 3600000 + 1000), format, true));
    Date date = new Date(now() - 24 * 3600000);
    assertEquals("on " + format.format(date), DateUtil.toFriendlyDateTime(date, format, true));
//    assertEquals("today at " + DateUtil.LOCAL_TIME.format(date), DateUtil.toFriendlyDateTime(new Date(now()), format, false));
  }

  private long now() {
    return System.currentTimeMillis();
  }

  public void testToDayStart() throws BrokenBarrierException, InterruptedException {
    setWriteToStdout(true);
    forEachDayEachTzMultiThread(-2000, 16000, new IntProcedure() {
      @Override
      public void invoke(int day) {
        doTestToDayStart(day, +1);
      }
    });
  }

  public void testToDayStartJuneau30Oct1983() {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Juneau"));
    doTestToDayStart(5050, +1);
  }
  
  public void testJuneau30Oct1983() {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Juneau"));
    Calendar c = new GregorianCalendar();
    c.clear();
    for (int i = 0; i < 5050 - 1; ++i) {
      c.add(Calendar.DATE, 1);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
    }    
    c.add(Calendar.DATE, 1);
    long dayStart1 = c.getTime().getTime();
    c.set(Calendar.HOUR_OF_DAY, 0);
    long dayStart2 = c.getTime().getTime();
    // This test fails :(
//    assertEquals(dayStart1, dayStart2);
  }

  private static void doTestToDayStart(int day, int inc) {
    Calendar c = new GregorianCalendar();
    c.setTime(DateUtil.toInstantOnDay(day));
    c.add(Calendar.HOUR_OF_DAY, 12);
    long time = c.getTimeInMillis();
    TimeZone tz = TimeZone.getDefault();
    long dayStart = inc > 0 ? DateUtil.toDayStart(time, tz) : DateUtil.toDayEnd(time, tz);
    Date d = new Date(dayStart);
    if (day != DateUtil.toDayNumberFromInstant(d)) // Duplicates assertEquals condition to avoid create message - it users Date.toString() that blocks in threads (on multi-core CPU).
      assertEquals(tz.getID() + " [" + c.getTime() + "] [" + d + ']', day, DateUtil.toDayNumberFromInstant(d));
    Date s = new Date(dayStart - inc);
    if (day -inc != DateUtil.toDayNumberFromInstant(s)) // Duplicates assertEquals condition to avoid create message - it users Date.toString() that blocks in threads (on multi-core CPU).
      assertEquals(tz.getID() + " [" + c.getTime() + "] [" + s + ']', day - inc, DateUtil.toDayNumberFromInstant(s));
  }

  public void testToDayEnd() throws BrokenBarrierException, InterruptedException {
    setWriteToStdout(true);
    forEachDayEachTzMultiThread(-2000, 16000, new IntProcedure() {
      @Override
      public void invoke(int day) {
        doTestToDayStart(day, -1);
      }
    });
  }

  /** Examples of various pitfalls for simpler approaches. */
  public void testToInstantOnDay() {
    String[] availableIDs = TimeZone.getAvailableIDs();
    int[] days = {0, 1, 15400};
    String[][] strs =                         
      {{"01.01.1970", "02.01.1970", "01.03.2012"}, {"00:00:00", "00:00:00", "00:00:00"}};  
    Map<String, String[][]> excpts = hashMap();
    // Kwajalein, Enderbury have skipped over a day as per tzdata moving across the date line in 1990-s; Apia quite recently, on Dec 30, 2011
    // See also https://wiki.almworks.com/pages/viewpage.action?pageId=12878315 (Вечер с четверга на субботу 30 Dec 2011)
    for (String s: DAY_SKIPPERS)
      excpts.put(s, new String[][]
        {{"01.01.1970", "02.01.1970", "02.03.2012"}, {"00:00:00", "00:00:00", "00:00:00"}});
    
    // In these time zones offset change has occurred in 1970, as tzdata reports (without day and hour). Java thinks it happened on Jan 1 at midnight.
    // See more here: http://wiki.almworks.com/display/process/java.util.GregorianCalendar.clear%28%29+may+set+HOUR_OF_DAY+and+HOUR+to+non-zero
    for (String s : new String[]{"America/Hermosillo", "America/Mazatlan", "Mexico/BajaSur", "America/Bahia_Banderas"})
      excpts.put(s, new String[][]
      {{"01.01.1970", "02.01.1970", "01.03.2012"}, {"01:00:00", "00:00:00", "00:00:00"}});
   
    DateFormat[] fmts = {new SimpleDateFormat("dd.MM.yyyy"), new SimpleDateFormat("HH:mm:ss")};
    for (String tzId : Arrays.copyOf(availableIDs, availableIDs.length)) {
      if ("Pacific/Fakaofo".equals(tzId)) continue; // TimeZone data is updated in java6.0_33, but not yet updated in java6.0_31 (used on Build server). This timeZone excluded from tests.
      TimeZone tz = TimeZone.getTimeZone(tzId);
      TimeZone.setDefault(tz);
      String[][] expected = Util.NN(excpts.get(tzId), strs);
      for (DateFormat fmt : fmts) fmt.setTimeZone(tz);
      for (int i = 0; i < days.length; ++i) {
        int day = days[i];
        Date date = DateUtil.toInstantOnDay(day);
        String msg = day + " " + date + " " + tzId;
        for (int j = 0; j < fmts.length; ++j)
          assertEquals(msg, expected[j][i], fmts[j].format(date));
      }
    }
  }
  
  public void testInstantToDayNumber() throws ParseException, InterruptedException {
    Set<String> availableIDs = hashSet();
    availableIDs.addAll(arrayList(TimeZone.getAvailableIDs()));
    availableIDs.removeAll(complement(arrayList(DAY_SKIPPERS), arrayList("Pacific/Apia")));

    int nThreads = Runtime.getRuntime().availableProcessors();
    final BlockingQueue<Pair<String, Integer>> queue = new ArrayBlockingQueue<Pair<String, Integer>>(availableIDs.size());
    final Pair<String, Integer> killTask = Pair.nullNull();
    final CountDownLatch latch = new CountDownLatch(nThreads);

    final String[] instants = {"01.01.1970 00:00:00.000", "02.06.1969 23:12:32.232", "13.08.1998 12:00:00.000", "29.02.2012 16:09:12.332", "31.12.2000 23:59:59.999"};
    final int[] days = {0, -213, 10451, 15399, 11322};
    final int[] apia = {0, -213, 10451, 15398, 11322};
    final int sz = availableIDs.size() * instants.length;
    final AtomicInteger done = new AtomicInteger(0);
    for (int i = 0; i < nThreads; ++i) new Thread(new Runnable() {
      final SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
      @Override
      public void run() {
        try {
          Pair<String, Integer> task;
          while (!killTask.equals(task = queue.take())) {
            String tzId = task.getFirst();
            TimeZone tz = TimeZone.getTimeZone(tzId);
            Integer i = task.getSecond();            
            fmt.setTimeZone(tz);
            Date instant = fmt.parse(instants[i]);
            int[] expectedDays = "Pacific/Apia".equals(tzId) ? apia : days;
            int day = DateUtil.toDayNumberFromInstant(instant, tz);
            assertEquals(tzId + " [" + i + ']', expectedDays[i], day);
            System.out.format("%d%%\r", (int)Math.round(100d*(done.incrementAndGet())/sz));
          }
          System.out.println(Thread.currentThread().getName() + " exiting");
          latch.countDown();
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        } catch (ParseException e) {
          fail(e.getMessage());
        }
      }
    }).start();    
    
    for (String tzId : availableIDs) {
      for (int i = 0; i < instants.length; ++i) {
        queue.put(Pair.create(tzId, i));
      }
    }
    for (int i = 0; i < nThreads; ++i) queue.put(killTask);
    latch.await();
  }

  public void testInstantToDayNumberRoundtrip() throws InterruptedException, BrokenBarrierException {
    setWriteToStdout(true, Level.WARNING);
    forEachDayEachTzMultiThread(-2000, 16000, new IntProcedure() {
      @Override
      public void invoke(int day) {
        checkInstantToDayNumberRoundtrip(day, day, new Random());
      }
    });
  }

  /** @param minDay day number, inclusive; e.g., 15400 is Mar 1 2012 */
  private static void forEachDayEachTzMultiThread(int minDay, int maxDay, IntProcedure check) throws InterruptedException, BrokenBarrierException {
    int span = maxDay - minDay;
    int nThreads = Runtime.getRuntime().availableProcessors();
    int step = span / nThreads;
    BlockingQueue<Pair<Integer, Integer>> queue = new ArrayBlockingQueue<Pair<Integer, Integer>>(nThreads);
    CyclicBarrier barrier = new CyclicBarrier(nThreads + 1);
    for (int i = 0; i < nThreads; ++i) new Thread(new DayRangeConditionChecker(queue, barrier, check)).start();
    String[] availableIDs = TimeZone.getAvailableIDs();
    availableIDs = Arrays.copyOf(availableIDs, availableIDs.length);
    for (int i = 0; i < availableIDs.length; i++) {
      TimeZone tz = TimeZone.getTimeZone(availableIDs[i]);
      TimeZone.setDefault(tz);

      for (int j = 0; j < nThreads; ++j) queue.add(Pair.create(minDay + step * j, Math.min(minDay + step * (j + 1), maxDay)));
      barrier.await();

      System.out.format("%d%%\r", (int)Math.round(100d*(++i)/availableIDs.length));
    }
    for (int j = 0; j < nThreads; ++j) queue.add(DayRangeConditionChecker.KILL_TASK);
    barrier.await();
  }

  private static void checkInstantToDayNumberRoundtrip(int i, int dayNumber, Random r) {
    String tzId = TimeZone.getDefault().getID();
    // Should be mignight or occasionally not-midnight on offset changes
    Date instant = DateUtil.toInstantOnDay(dayNumber);
    int roundtrip = DateUtil.toDayNumberFromInstant(instant);
    assertEquals(tzId + " [" + i + ']', dayNumber, roundtrip);

    Calendar c = GregorianCalendar.getInstance();
    c.setTime(instant);
    c.roll(Calendar.HOUR_OF_DAY, r.nextInt(24));
    c.roll(Calendar.MINUTE, r.nextInt(60));
    c.roll(Calendar.SECOND, r.nextInt(60));
    c.roll(Calendar.MILLISECOND, r.nextInt(1000));
    instant = c.getTime();
    roundtrip = DateUtil.toDayNumberFromInstant(instant);
    if (dayNumber != roundtrip) // Duplicates assertEquals condition to avoid create message - it users Date.toString() that blocks in threads (on multi-core CPU).
      assertEquals(tzId + " [" + i + "] [" + instant + ']', dayNumber, roundtrip);

/*
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 999);
    instant = c.getTime();
    roundtrip = DateUtil.toDayNumberFromInstant(instant);
    assertEquals(tzId + " [" + i + "] [" + instant + ']', dayNumber, roundtrip);
*/
  }
  
  private void checkInstantToDayNumberRoundtrip(int i, int dayNumber, String tzId) {
    TimeZone tz = TimeZone.getTimeZone(tzId);
    TimeZone.setDefault(tz);
    checkInstantToDayNumberRoundtrip(i, dayNumber, getRandom());
  }

  private void checkInstantToDayNumberRoundtrip(int i, int dayNumber) {
    checkInstantToDayNumberRoundtrip(i, dayNumber, getRandom());
  }
  
  public void testPacificApiaDay15338() {
    // Guys have skipped Dec 30 2011
    checkInstantToDayNumberRoundtrip(0, 15338, "Pacific/Apia");    
    checkInstantToDayNumberRoundtrip(0, 15337, "Pacific/Apia");    
    checkInstantToDayNumberRoundtrip(0, 15339, "Pacific/Apia");    
  }

  /** There was no 20 Aug 1993 in Kwajalein. */
  public void testKwajalein20Aug1993() throws ParseException {
    TimeZone.setDefault(TimeZone.getTimeZone("Kwajalein"));
    SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy");
    assertEquals(8631, DateUtil.toDayNumberFromInstant(fmt.parse("19.08.1993")));
    assertEquals(8632, DateUtil.toDayNumberFromInstant(fmt.parse("20.08.1993")));
    assertEquals(8632, DateUtil.toDayNumberFromInstant(fmt.parse("21.08.1993")));
    checkInstantToDayNumberRoundtrip(0, 8631);
    checkInstantToDayNumberRoundtrip(0, 8632);
    checkInstantToDayNumberRoundtrip(0, 8633);
  }
  
  public void testRussiaNow() {
    checkInstantToDayNumberRoundtrip(0, 15400, "Europe/Moscow");
  }

  public void testAlgiers479() {
    TimeZone.setDefault(TimeZone.getTimeZone("Africa/Algiers"));
    Calendar c = new GregorianCalendar();
    c.setTime(DateUtil.toInstantOnDay(479));
    c.set(Calendar.HOUR_OF_DAY, 23);
    c.set(Calendar.MINUTE, 59);
    c.set(Calendar.SECOND, 59);
    c.set(Calendar.MILLISECOND, 999);
    assertEquals(480, DateUtil.toDayNumberFromInstant(c.getTime()));
    
    c.setTime(DateUtil.toInstantOnDay(479));
    c.roll(Calendar.HOUR_OF_DAY, 23);
    c.roll(Calendar.MINUTE, 59);
    c.roll(Calendar.SECOND, 59);
    c.roll(Calendar.MILLISECOND, 999);
    Date d479 = c.getTime();
    System.out.println(d479);
    assertEquals(479, DateUtil.toDayNumberFromInstant(d479));
  }


  private static class DayRangeConditionChecker implements Runnable {
    private static final Pair<Integer, Integer> KILL_TASK = Pair.create(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private final BlockingQueue<Pair<Integer, Integer>> myTaskQueue;
    private final CyclicBarrier myBarrier;
    private final IntProcedure myCheck;

    public DayRangeConditionChecker(BlockingQueue<Pair<Integer, Integer>> taskQueue, CyclicBarrier barrier, IntProcedure check) {
      myTaskQueue = taskQueue;
      myBarrier = barrier;
      myCheck = check;
    }

    @Override
    public void run() {
      try {
        Pair<Integer, Integer> task;
        while (!KILL_TASK.equals(task = myTaskQueue.take())) {
          int start = task.getFirst();
          int end = task.getSecond();
          for (int i = start; i < end; ++i) {
            myCheck.invoke(i);
          }
//          System.out.println(Thread.currentThread().getName() + " [" + start + "; " + end + "] finished");
          myBarrier.await();
        }
        System.out.println(Thread.currentThread().getName() + " exiting");
        myBarrier.await();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      } catch (BrokenBarrierException e) {
        throw new Failure(e);
      } catch (AssertionFailedError ex) {
        myBarrier.reset();
        throw ex;
      }
    }
  }
}

