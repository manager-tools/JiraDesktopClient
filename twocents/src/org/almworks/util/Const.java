package org.almworks.util;

/**
 * This class holds various utility constants
 */
public class Const {
  // Time constants
  public static final long SECOND = 1000;
  public static final long MINUTE = 60 * SECOND;
  public static final long HOUR = 60 * MINUTE;
  public static final long DAY = 24 * HOUR;

  public static final long KIBIBYTE = 1L << 10;
  public static final long MEBIBYTE = 1L << 20;

  // Empty
  public static final Object[] EMPTY_OBJECTS = {};
  public static final Object[][] EMPTY_OBJECTS2D = {};
  public static final Class[] EMPTY_CLASSES = {};
  public static final String[] EMPTY_STRINGS = {};
  public static final byte[] EMPTY_BYTES = {};
  public static final byte[][] EMPTY_BYTES2D = {};
  public static final char[] EMPTY_CHARS = {};
  public static final int[] EMPTY_INTS = {};
  public static final long[] EMPTY_LONGS = {};
  public static final double[] EMPTY_DOUBLES = {};
  public static final Runnable EMPTY_RUNNABLE = EmptyRunnable.INSTANCE;
  public static final Integer[] EMPTY_INTEGER = new Integer[0];

  public static final Class[] CLASSES_INT = {int.class};
  public static final Class[] CLASSES_STRING = {String.class};
  public static final Class[] CLASSES_STRING_THROWABLE = {String.class, Throwable.class};

  private Const() {}

  private static final class EmptyRunnable implements Runnable {
    public static final Runnable INSTANCE = new EmptyRunnable();
    public void run() {
    }
  }
}
