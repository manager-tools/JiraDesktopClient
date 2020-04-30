package com.almworks.util.exec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LongEventQueue {
  private static volatile LongEventQueue ourInstance;

  @NotNull
  public static LongEventQueue instance() {
    LongEventQueue queue = ourInstance;
    if (queue == null) {
      queue = Context.get(LongEventQueue.class);
    }
    if (queue == null) {
      assert false : "no queue in context";
      queue = Dummy.INSTANCE;
    }
    return queue;
  }

  public static void installStatic(LongEventQueue queue) {
    ourInstance = queue;
  }

  public static void installToContext() {
    Context.add(InstanceProvider.instance(new LongEventQueueImpl()), "LongEventQueue.installToContext()");
  }

  public static void removeFromContext() {
    instance().shutdownGracefully();
    Context.pop();
  }

  public ImmediateThreadGate immediate() {
    return getImmediateGate(null);
  }

  public ImmediateThreadGate immediate(@Nullable Object key) {
    return getImmediateGate(key);
  }

  public ThreadGate optimal(@Nullable Object key) {
    return getNonImmediateGate(key, true);
  }

  public ThreadGate optimal() {
    return getNonImmediateGate(null, true);
  }

  public ThreadGate queued(@Nullable Object key) {
    return getNonImmediateGate(key, false);
  }

  public ThreadGate queued() {
    return getNonImmediateGate(null, false);
  }

  public abstract void shutdownGracefully();

  public abstract void shutdownImmediately();

  public abstract boolean isAlive();

  protected abstract ThreadGate getNonImmediateGate(@Nullable Object key, boolean optimal);

  protected abstract ImmediateThreadGate getImmediateGate(@Nullable Object key);

  private static final class Dummy extends LongEventQueue {
    public static final Dummy INSTANCE = new Dummy();

    protected ImmediateThreadGate getImmediateGate(@Nullable Object key) {
      return ThreadGate.STRAIGHT;
    }

    protected ThreadGate getNonImmediateGate(@Nullable Object key, boolean optimal) {
      return ThreadGate.STRAIGHT;
    }

    public boolean isAlive() {
      return true;
    }

    public void shutdownGracefully() {
    }

    public void shutdownImmediately() {
    }
  }
}


