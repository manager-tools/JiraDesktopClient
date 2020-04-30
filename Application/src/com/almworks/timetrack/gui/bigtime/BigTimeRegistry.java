package com.almworks.timetrack.gui.bigtime;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The singleton that keeps available {@link BigTime}
 * implementations.
 * These could be pluggable, but everything is
 * hardcoded for now.
 */
public class BigTimeRegistry {
  private static final BigTime[] ourBigTimes = {
    new TimeSpentByMe(),
    new TotalTimeSpent(),
    new WallClockTime(),
  };

  /**
   * Find a {@link BigTime} instance by its ID.
   * @param id The Big Time ID (as returned by {@link BigTime#getId()}).
   * @return The {@link BigTime} instance with this ID, or {@code null}.
   */
  @Nullable
  public static BigTime getBigTime(String id) {
    for(final BigTime bigTime : ourBigTimes) {
      if(bigTime.getId().equals(id)) {
        return bigTime;
      }
    }
    return null;
  }

  /**
   * @return The list of all available {@link BigTime}s to choose from.
   */
  public static List<BigTime> getAvailableBigTimes() {
    return Collections15.arrayList(ourBigTimes);
  }

  /**
   * @return Whether there are any {@link BigTime} implementations
   * available.
   */
  public static boolean hasAvailableBigTimes() {
    return true;
  }

  /**
   * @return The default {@link BigTime} implementation, if any.
   */
  @Nullable
  public static BigTime getDefaultBigTime() {
    return ourBigTimes[0];
  }
}
