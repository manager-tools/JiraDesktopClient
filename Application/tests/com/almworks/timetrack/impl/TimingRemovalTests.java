package com.almworks.timetrack.impl;

import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Const;

import java.util.List;

public class TimingRemovalTests extends BaseTestCase {
  private static final TimeTrackerTask T0 = new TimeTrackerTask(0);
  private static final long BASETIME = 10000000000L;

  private TimeTrackerState myState;
  private long myNow;
  private static final int HOUR = 100000;

  public TimingRemovalTests() {
    super(HEADLESS_AWT_IMMEDIATE_GATE);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myState = new TimeTrackerState() {
      @Override
      protected long now() {
        return myNow;
      }
    };
  }

  public void testRemove() {
    log(10, 20);
    checkTimings(10, 20);
    remove(15, 16);
    checkTimings(10, 15, 16, 20);
    remove(20, 21);
    checkTimings(10, 15, 16, 20);
    remove(21, 22);
    checkTimings(10, 15, 16, 20);
    remove(9, 10);
    checkTimings(10, 15, 16, 20);
    remove(15, 16);
    checkTimings(10, 15, 16, 20);
    remove(16, 16);
    checkTimings(10, 15, 16, 20);
    remove(14, 16);
    checkTimings(10, 14, 16, 20);
    remove(14, 17);
    checkTimings(10, 14, 17, 20);
    remove(13, 18);
    checkTimings(10, 13, 18, 20);
    remove(15, 22);
    checkTimings(10, 13);
    remove(9, 11);
    checkTimings(11, 13);
    remove(11, 13);
    checkTimings();
  }

  public void testSmallChanges() {
    log(1, 2);
    long h = getHour(10);
    myNow = h;
    myState.setTrackingAndCurrentTask(true, T0);
    myNow += 30 * Const.SECOND;
    myState.setTracking(false);
    checkTimings(1, 2);

    myNow = h;
    myState.setTracking(true);
    myNow += HOUR;
    myState.setTracking(false);
    checkTimings(1, 2, 10, 11);
    myState.setTracking(true);
    myState.setTracking(false);
    checkTimings(1, 2, 10, 11);
    myNow += 30 * Const.SECOND;
    myState.setTracking(true);
    myNow = h + 2 * HOUR;
    myState.setTracking(false);
    checkTimings(1, 2, 10, 12);
  }

  private void remove(int from, int to) {
    myState.removeTimingsNoUpdate(getHour(from), getHour(to));
  }

  private void checkTimings(int... d) {
    checkTimings(myState.getTaskTimings(T0), d);
  }

  private void checkTimings(List<TaskTiming> timings, int... d) {
    if (d.length == 0) {
      assertTrue(String.valueOf(timings), timings == null || timings.isEmpty());
    } else {
      assertEquals(d.length / 2, timings.size());
      for (int i = 0; i < timings.size(); i++) {
        assertEquals(getHour(d[i * 2]), timings.get(i).getStarted());
        assertEquals(getHour(d[i * 2 + 1]), timings.get(i).getStopped());
      }
    }
  }

  private void log(int startHour, int endHour) {
    assert endHour >= startHour || endHour == 0;
    setHour(startHour);
    myState.setTrackingAndCurrentTask(true, T0);
    if (endHour > 0) {
      setHour(endHour);
      myState.setTrackingAndCurrentTask(false, null);
    }
  }

  private void setHour(int hour) {
    myNow = getHour(hour);
  }

  private long getHour(int hour) {
    return hour <= 0 ? 0 : BASETIME + hour * HOUR;
  }

  private void clear() {
    myState.clear();
  }
}
