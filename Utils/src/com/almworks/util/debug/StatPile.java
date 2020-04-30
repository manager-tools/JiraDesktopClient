package com.almworks.util.debug;

import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.Map;
import java.util.SortedMap;

public class StatPile {
  public static final TypedKey<StatPile> STAT_PILE = TypedKey.create(StatPile.class);

  private final SortedMap<String, Object> myStats = Collections15.treeMap(String.CASE_INSENSITIVE_ORDER);

  public synchronized void increment(String name) {
    Integer v = (Integer) myStats.get(name);
    myStats.put(name, v == null ? 1 : v + 1);
  }

  public static boolean increment(Map map, String name) {
    StatPile statPile = STAT_PILE.getFrom((Map<? extends TypedKey, ?>) map);
    if (statPile != null) {
      statPile.increment(name);
    }
    return true;
  }

  public synchronized void start(String name) {
    getTimeStat(name).start();
  }

  public static boolean start(Map map, String name) {
    StatPile statPile = STAT_PILE.getFrom((Map<? extends TypedKey, ?>) map);
    if (statPile != null) {
      statPile.start(name);
    }
    return true;
  }

  private TimeStat getTimeStat(String name) {
    TimeStat timeStat = (TimeStat) myStats.get(name);
    if (timeStat == null) {
      timeStat = new TimeStat();
      myStats.put(name, timeStat);
    }
    return timeStat;
  }

  public synchronized void stop(String name) {
    getTimeStat(name).stop();
  }

  public static boolean stop(Map map, String name) {
    StatPile statPile = STAT_PILE.getFrom((Map<? extends TypedKey, ?>) map);
    if (statPile != null) {
      statPile.stop(name);
    }
    return true;
  }

  public synchronized void count(String name, int count) {
    getNumberStat(name).count(count);
  }

  public static boolean count(Map<? extends TypedKey, ?> map, String name, boolean bool) {
    return count(map, name, bool ? 1 : 0);
  }

  public static boolean count(Map<? extends TypedKey, ?> map, String name, int count) {
    StatPile statPile = STAT_PILE.getFrom((Map<? extends TypedKey, ?>) map);
    if (statPile != null) {
      statPile.count(name, count);
    }
    return true;
  }



  private NumberStat getNumberStat(String name) {
    NumberStat numberStat = (NumberStat) myStats.get(name);
    if (numberStat == null) {
      numberStat = new NumberStat();
      myStats.put(name, numberStat);
    }
    return numberStat;
  }

  public String toString() {
    return getStats();
  }

  public String getStats() {
    StringBuffer buffer = new StringBuffer();
    int align = 0;
    for (String s : myStats.keySet())
      align = Math.max(align, s.length());
    for (Map.Entry<String, Object> entry : myStats.entrySet()) {
      String key = entry.getKey();
      buffer.append(key);
      for (int i = key.length(); i < align; i++)
        buffer.append(' ');
      buffer.append(" = ");
      buffer.append(entry.getValue().toString());
      buffer.append('\n');
    }
    return buffer.toString();
  }

  private static class TimeStat {
    private int myCount = 0;
    private long myTotalTime = 0;
    private long myLastStart = 0;

    public void start() {
      myLastStart = System.currentTimeMillis();
    }

    public void stop() {
      if (myLastStart <= 0)
        return;
      myCount++;
      myTotalTime += System.currentTimeMillis() - myLastStart;
    }

    public String toString() {
      String s = Integer.toString(myCount);
      if (myCount > 0)
        s += ", " + myTotalTime + "ms total, " + (1F * (1000L * myTotalTime / myCount) / 1000) + "ms avg";
      return s;
    }
  }

  private static class NumberStat {
    private int myCount = 0;
    private int myTotalCount = 0;

    public void count(int value) {
      myCount++;
      myTotalCount += value;
    }

    public String toString() {
      String s = myTotalCount + " / " + myCount;
      if (myCount > 0)
        s += ", " + (1F * (myTotalCount * 1000 / myCount) / 1000) + " ratio";
      return s;
    }
  }
}
