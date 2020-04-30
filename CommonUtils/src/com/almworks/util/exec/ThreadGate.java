package com.almworks.util.exec;

import com.almworks.util.Env;
import com.almworks.util.threads.InterruptableRunnable;
import org.almworks.util.ExceptionUtil;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public abstract class ThreadGate {
  // if not set, ThreadGate.execute will not inherit context frames by default
  public static final boolean TRANSFER_CONTEXT_FRAMES = Env.getBoolean("transfer.context.frames");

  public static final ImmediateThreadGate STRAIGHT = StraightGate.INSTANCE;

  public static final ImmediateThreadGate AWT_IMMEDIATE = new AwtImmediateThreadGate();
  public static final ThreadGate AWT_QUEUED = new AwtNonImmediateThreadGate(Type.QUEUED);
  public static final ThreadGate AWT_OPTIMAL = new AwtNonImmediateThreadGate(Type.OPTIMAL);
  public static final ThreadGate AWT = AWT_OPTIMAL;

  public static final ImmediateThreadGate FX_IMMEDIATE = new FXImmediateThreadGate(false);
  public static final ThreadGate FX_QUEUED = new FXNotImmediateThreadGate(Type.QUEUED);
  public static final ThreadGate FX_OPTIMAL = new FXNotImmediateThreadGate(Type.OPTIMAL);
  public static final ThreadGate FX = FX_OPTIMAL;

  public static final ImmediateThreadGate LONG_IMMEDIATE = new LongImmediateThreadGate();
  public static final ThreadGate LONG_QUEUED = new LongNonImmediateThreadGate(Type.QUEUED);
  public static final ThreadGate LONG_OPTIMAL = new LongNonImmediateThreadGate(Type.OPTIMAL);
  public static final ThreadGate LONG = LONG_OPTIMAL;

  public static final ThreadGate NEW_THREAD = new NewThreadGate();

  public static ImmediateThreadGate LONG_IMMEDIATE(Object key) {
    return LongEventQueue.instance().immediate(key);
  }

  public static ThreadGate LONG_QUEUED(Object key) {
    return LongEventQueue.instance().queued(key);
  }

  public static ThreadGate LONG_OPTIMAL(Object key) {
    return LongEventQueue.instance().optimal(key);
  }

  public static ThreadGate LONG(Object key) {
    return LONG_OPTIMAL(key);
  }

  protected abstract void gate(Runnable runnable) throws InterruptedException, InvocationTargetException;

  protected Target getTarget() {
    return Target.OTHER;
  }

  protected Type getType() {
    return Type.UNKNOWN;
  }

  public final void execute(boolean transferContext, @Nullable Gateable gateable) {
    if (gateable != null) {
      gateable.enterGate(transferContext ? ContextFrameDataProvider.create(Context.getTopFrame()) : null);
      doExecute(gateable);
    }
  }

  public final void execute(boolean transferContext, @Nullable Runnable runnable) {
    if (runnable != null) {
      if (transferContext) {
        Gateable gateable = runnable instanceof Gateable ? (Gateable) runnable : new GateableAdapter(runnable);
        execute(transferContext, gateable);
      } else {
        doExecute(runnable);
      }
    }
  }

  public final void execute(@Nullable Runnable runnable) {
    execute(TRANSFER_CONTEXT_FRAMES, runnable);
  }

  private void doExecute(Runnable runnable) {
    try {
      gate(runnable);
    } catch (Exception e) {
      onException(e);
    }
  }

  protected void onException(Throwable e) {
    ExceptionUtil.rethrowNullable(e);
  }

  public static void executeLong(Object key, final InterruptableRunnable runnable) {
    LONG(key).execute(new InterruptableRunnable.Wrapper(runnable));
  }

  public static void executeLong(InterruptableRunnable runnable) {
    LONG.execute(new InterruptableRunnable.Wrapper(runnable));
  }

  /**
   * @param gate
   * @return true if gating isn't requered in current thread
   */
  public static boolean isRightNow(ThreadGate gate) {
    if (gate == STRAIGHT)
      return true;
    if (gate == AWT || gate == AWT_IMMEDIATE)
      return Context.isAWT();
    return false;
  }

  public static boolean notAWT(ThreadGate gate) {
    return gate != AWT && gate != AWT_IMMEDIATE && gate != AWT_OPTIMAL && gate != AWT_QUEUED;
  }

  private static class GateableAdapter extends Gateable {
    private final Runnable myRunnable;

    public GateableAdapter(Runnable runnable) {
      myRunnable = runnable;
    }

    public void runGated() {
      myRunnable.run();
    }
  }

  public static enum Target {
    AWT,
    FX,
    LONG,
    STRAIGHT,
    OTHER,
  }


  public static enum Type {
    IMMEDIATE,
    QUEUED,
    OPTIMAL,
    UNKNOWN
  }


  private static class LongNonImmediateThreadGate extends DelegatingThreadGate {
    private final Type myType;

    public LongNonImmediateThreadGate(Type type) {
      myType = type;
    }

    protected ThreadGate getDelegate() {
      LongEventQueue queue = LongEventQueue.instance();
      return myType == Type.OPTIMAL ? queue.optimal() : queue.queued();
    }
  }

  private static class LongImmediateThreadGate extends DelegatingImmediateThreadGate {
    protected ImmediateThreadGate getDelegate() {
      return LongEventQueue.instance().immediate();
    }
  }
}
