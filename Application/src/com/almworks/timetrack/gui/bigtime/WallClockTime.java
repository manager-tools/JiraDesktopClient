package com.almworks.timetrack.gui.bigtime;

import com.almworks.items.api.Database;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.i18n.Local;
import org.almworks.util.Const;

import java.util.Date;

/**
 * The {@link BigTime} implementation that provides
 * the wall clock time when last work was done.
 */
public class WallClockTime extends BigTimeImpl {
  public WallClockTime() {
    super(
      "Last work time",
      Local.parse("Wall clock time when last work was done on any " + Terms.ref_artifact),
      "LastWorkTime");
  }

  public void getBigTimeText(Database db, TimeTracker tt, Procedure<String> proc) {
    final boolean trackingActive = tt.isTracking();
    final long now = System.currentTimeMillis();

    final long time;
    if(trackingActive) {
      time = now;
    } else {
      final Pair<TimeTrackerTask,TaskTiming> preceding = tt.getPrecedingTiming(now);
      if(preceding == null) {
        proc.invoke(EMPTY_VALUE);
        return;
      } else {
        time = preceding.getSecond().getStopped();
      }
    }

    final StringBuilder b = new StringBuilder();
    final Date d = new Date(time);
    if (!DateUtil.isSameDay(time, now)) {
      b.append(DateUtil.LOCAL_DATE.format(d)).append(' ');
    }
    final String s = DateUtil.LOCAL_TIME.format(d);

    int i;
    for (i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!Character.isLetterOrDigit(c)) {
        break;
      }
    }

    int l = b.length();
    b.append(s);

    if (trackingActive && ((now / Const.SECOND) & 1L) != 1L) {
      if (i < s.length()) {
        b.setCharAt(l + i, ' ');
      }
    }

    proc.invoke(b.toString());
  }
}
