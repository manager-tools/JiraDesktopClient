package com.almworks.util.tests;

import com.almworks.util.DecentFormatter;
import com.almworks.util.Env;
import com.almworks.util.RunnableE;
import com.almworks.util.TestLog;
import com.almworks.util.exec.AwtImmediateThreadGate;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.MainThreadGroup;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * @author : Dyoma
 */
public abstract class BaseTestCase extends TestCase {
  public static final ThreadGate HEADLESS_AWT_IMMEDIATE_GATE = new ThreadGate() {
    @Override
    protected void gate(Runnable runnable) throws InterruptedException, InvocationTargetException {
      AwtImmediateThreadGate.testSetOurCheckFXThread(false);
      try {
        ThreadGate.AWT_IMMEDIATE.execute(runnable);
      } finally {
        AwtImmediateThreadGate.testSetOurCheckFXThread(true);
      }
    }
  };
  private static final int DEFAULT_TIMEOUT = 20000;

  /** Tests may use (pseudo-)random number generator via {@link #getRandom}. To reproduce failing test, look up the used seed in the logs and set this system property. */
  public static final String RANDGEN_SEED = "tests.seed";

  private TestLog myLogger;
  private Throwable myMainThreadException = null;
  private final List<Throwable> myOtherExceptions = Collections15.arrayList();
  private ThreadGroup myTestGroup;
  private Thread myTestThread;
  private boolean myTimeout;
  private List<Throwable> myCaughtExceptions;
  private final int myTestTimeout;

  private ThreadGate myThreadGate;

  private Random myRandom;

  {
    TestLog.install();
  }

  public BaseTestCase() {
    this(getDefaultTimeout(), ThreadGate.STRAIGHT);
  }

  private static int getDefaultTimeout() {
    return Env.isDebugging() ? 86400000 : DEFAULT_TIMEOUT;
  }

  protected BaseTestCase(ThreadGate threadGate) {
    this(getDefaultTimeout(), threadGate);
  }

  protected BaseTestCase(int timeout) {
    this(timeout, ThreadGate.STRAIGHT);
  }

  public BaseTestCase(int testTimeout, ThreadGate threadGate) {
    assert testTimeout >= 1000 : testTimeout;
    myTestTimeout = testTimeout;
    setGate(threadGate);
    // Too slow with assertions
    getClass().getClassLoader().setPackageAssertionStatus("com.almworks.integers", false);
  }

  public final void setGate(ThreadGate threadGate) {
    assert threadGate != null;
    assert threadGate != ThreadGate.AWT && threadGate != ThreadGate.AWT_QUEUED : "ThreadGate.AWT_IMMEDIATE should be used";
    assert threadGate != ThreadGate.FX && threadGate != ThreadGate.FX_QUEUED : "ThreadGate.FX_IMMEDIATE should be used";
    myThreadGate = threadGate;
  }

  protected void setUp() throws Exception {
    super.setUp();
    AwtImmediateThreadGate.testSetOurCheckFXThread(false);
  }

  private Random initRandom() {
    setWriteToStdout(true);
    long seed = Util.toLong(System.getProperty(RANDGEN_SEED), 0L);
    if (seed != 0L) Log.debug("Using seed from system properties: " + seed);
    else {
      seed = System.currentTimeMillis();
      Log.debug("Using seed " + seed);
    }
    return new Random(seed);
  }

  public synchronized Random getRandom() {
    if (myRandom == null) {
      myRandom = initRandom();
    }
    return myRandom;
  }

  protected void tearDown() throws Exception {
    AwtImmediateThreadGate.testSetOurCheckFXThread(true);
    super.tearDown();
  }

  public final void runBare() throws Throwable {
    init();
    Detach detach = initThread();
    myTestThread.start();

    long killTime = getKillTime();
    while (true) {
      Thread thread = getNonGuiAndNonDaemonThread();
      if (thread == null)
        break;

      long timeLeft = killTime - System.currentTimeMillis();
      if (timeLeft < 10) {
        myTimeout = true;
        break;
      }

      waitForTestsToFinish(thread, timeLeft);
    }

    myCaughtExceptions = null;
    synchronized (myOtherExceptions) {
      myCaughtExceptions = Collections15.arrayList(myOtherExceptions);
    }
    boolean loggedFailure = myLogger.printLog();
    detach.detach();
    stopThreads();
    finish(loggedFailure);
  }

  private static void waitForTestsToFinish(Thread testsThread, long timeLeft) throws InterruptedException {
    long waitTime = 100L;
    long finish = System.currentTimeMillis() + timeLeft;
    while (true) {
      testsThread.join(waitTime);
      if (!testsThread.isAlive()) return;
      if (finish - System.currentTimeMillis() < 10L) return;
    }
  }

  private void finish(boolean loggedFailure) throws Throwable {
    if (myCaughtExceptions.size() > 0) {
      throw new ManyExceptions(myMainThreadException, myCaughtExceptions);
    } else {
      if (myMainThreadException != null)
        throw myMainThreadException;
    }
    myLogger.clearLog();
    if (loggedFailure) {
      fail("Error log not empty");
    }
    assertTrue("timeout " + myTestTimeout + "ms", !myTimeout); // -Dis.debugging=true  to avoid stops when debugging
  }

  private void stopThreads() {
    myTestGroup.interrupt();
  }

  private Thread getNonGuiAndNonDaemonThread() {
    Thread[] threads = new Thread[myTestGroup.activeCount()];
    int count = myTestGroup.enumerate(threads, true);
    if (count == 0)
      return null;
    for (int i = 0; i < count; i++)
      if (!isGuiThread(threads[i]) && !isDaemonThread(threads[i]))
        return threads[i];
    return null;
  }

  private boolean isDaemonThread(Thread thread) {
    return thread.isDaemon();
  }

  private long getKillTime() {
    return Env.isDebugging() ? Long.MAX_VALUE : System.currentTimeMillis() + myTestTimeout;
  }

  private Detach initThread() {
    MainThreadGroup main = MainThreadGroup.getOrCreate();
    myTestGroup = new ThreadGroup(main, "test");
    Detach detach = main.addListener(new MainThreadGroup.ExceptionListener() {
      public void onException(Thread thread, Throwable e) {
        if (e instanceof ThreadDeath)
          return;
        synchronized (myOtherExceptions) {
          myOtherExceptions.add(e);
        }
      }
    });

    myTestThread = new Thread(myTestGroup, new Runnable() {
      public void run() {
        try {
          runInTestThread();
        } catch (Throwable throwable) {
          myMainThreadException = throwable;
        }
      }
    });
    return detach;
  }

  private void init() {
    myLogger = TestLog.getInstance();
    myLogger.clearLog();
    myLogger.setWriteToStdout(false);
    myMainThreadException = null;
    myOtherExceptions.clear();
    myTimeout = false;
    myCaughtExceptions = null;
  }

  private boolean isGuiThread(Thread thread) {
    // :kludge: jdk-dependent
    return thread != null && thread.getName().startsWith("AWT-");
  }

  protected void setWriteToStdout(boolean value) {
    setWriteToStdout(value, null);
  }

  protected void setWriteToStdout(boolean value, Level level) {
    myLogger.setWriteToStdout(value);
    if (level != null)
      myLogger.setStdoutLevel(level);
  }
  
  protected void setTestFailLevel(Level level) {
    myLogger.setTestFailThreshold(level);
  }

  protected void runInTestThread() throws Throwable {
    final Throwable[] exception = new Throwable[] {null};
    myThreadGate.execute(new Runnable() {
      public void run() {
        try {
          junitRunBare();
        } catch (Throwable throwable) {
          exception[0] = throwable;
        }
      }
    });
    if (exception[0] != null)
      throw exception[0];
  }

  protected final void junitRunBare() throws Throwable {
    super.runBare();
  }

  protected void allowDebugOutput(Level level) {
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(level);
    handler.setFormatter(new DecentFormatter());
    Log.replaceHandlers(Log.getApplicationLogger(), handler);
  }

  public static void assertEquals(Object object1, Object object2) {
    TestCase.assertEquals(object1, object2);
    if (object1 != null)
      assertEquals("hashCode", object1.hashCode(), object2.hashCode());
  }

  public static void assertEqualsBothWays(Object object1, Object object2) {
    TestCase.assertEquals(object1, object2);
    TestCase.assertEquals(object2, object1);
    if (object1 != null)
      assertEquals("hashCode", object1.hashCode(), object2.hashCode());
  }

  public static void assertAsync(long timeout, Object lock, Asserter asserter) throws InterruptedException {

    final int COUNT = 10;
    long period = timeout / COUNT;
    Object localLock = lock == null ? new Object() : lock;
    synchronized (localLock) {
      for (int i = 0; i < COUNT; i++) {
        try {
          asserter.assertAttempt();
          return;
        } catch (AssertionFailedError e) {
          localLock.wait(period);
        }
      }
    }
    asserter.assertAttempt();
  }

  protected File createFileName() throws IOException {
    String className = getClass().getName();
    File fileName = File.createTempFile(className.substring(className.lastIndexOf('.') + 1), ".tmp");
    assertTrue(fileName.delete());
    fileName.deleteOnExit();
    return fileName;
  }

  protected File createTempDir() throws IOException {
    File name = createFileName();
    assertTrue(name.mkdirs());
    return name;
  }

  @SuppressWarnings({"SimplifiableJUnitAssertion"})
  public static void assertNotEqual(int expected, int actual) {
    assertNotEqual(null, expected, actual);
  }

  @SuppressWarnings({"SimplifiableJUnitAssertion"})
  public static void assertNotEqual(String message, int expected, int actual) {
    if (message == null)
      message = "";
    assertFalse((message.length() > 0 ? message + ": " : "") + expected + "!=" + actual, expected == actual);
  }

  protected static byte[] createTestData(int count, int seed) {
    byte[] result = new byte[count];
    for (int i = 0; i < result.length; i++)
      result[i] = (byte) (i * (i + seed));
    return result;
  }

  protected static <E extends Throwable> void mustThrow(RunnableE<E> runnable) {
    mustThrow(null, "exception was not thrown", runnable);
  }

  protected static <E extends Throwable> void mustThrow(String message, RunnableE<E> runnable) {
    mustThrow(null, message, runnable);
  }

  protected static <E extends Throwable> void mustThrow(Class<? extends E> clazz, RunnableE<E> runnable) {
    mustThrow(clazz, clazz + " was not thrown", runnable);
  }

  protected static <E extends Throwable> void mustThrow(Class<? extends E> clazz, String message, RunnableE<E> runnable) {
    if (runnable == null)
      throw new NullPointerException("runnable");
    try {
      runnable.run();
      fail(message);
    } catch (Throwable e) {
      if (clazz != null && !clazz.isInstance(e)) {
        if (e instanceof Error)
          throw (Error) e;
        if (e instanceof RuntimeException)
          throw (RuntimeException) e;
        fail(message + " [threw " + e + " instead of " + clazz + "]");
      }
    }
  }

  public static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  protected static interface Asserter {
    void assertAttempt() throws AssertionFailedError;
  }
}
