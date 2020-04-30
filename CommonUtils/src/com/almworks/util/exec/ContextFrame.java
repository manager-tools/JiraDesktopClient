package com.almworks.util.exec;

import com.almworks.util.Env;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Context frame is a single storage of context data. It must be thread-safe.
 */
final class ContextFrame {
  private static final int MAX_DEPTH = Env.getInteger("context.max.depth", 25);

  private static final ThreadLocal<LinkedHashSet<ContextFrame>> ourCallStack =
    Context.DEBUG ? new ThreadLocal<LinkedHashSet<ContextFrame>>() : null;

  private static final AtomicInteger ourCounter = new AtomicInteger(0);

  private final ContextFrame myParentFrame;
  private final ContextDataProvider myProvider;
  private final boolean myInherit;
  private final String myName;
  private final int myId;

  public ContextFrame(ContextFrame parentFrame, ContextDataProvider provider, boolean inherit, @Nullable String name) {
    myParentFrame = parentFrame;
    myProvider = provider;
    myInherit = inherit;
    myName = name;
    myId = ourCounter.incrementAndGet();
  }

  public boolean isInherit() {
    return myInherit;
  }

  public ContextFrame getParentFrame() {
    return myParentFrame;
  }

  public ContextDataProvider getProvider() {
    return myProvider;
  }

  public int getId() {
    return myId;
  }

  public String toString() {
    return myName == null ? String.valueOf(myId) : myId + "[" + myName + "]";
  }

  public static <T> T getObject(ContextFrame frame, Class<T> objectClass, TypedKey<T> key, int depth) throws
    ContextDepthException
  {
    if (frame == null)
      return null;
    if (depth > MAX_DEPTH)
      throw new ContextDepthException(objectClass + " " + key);
    depth++;
    if (Context.DEBUG) {
      Failure result = traceCallStack(frame, objectClass, key);
      if (result != null) {
        throw result;
      }
    }
    try {
      for (ContextFrame f = frame; f != null; f = f.getParentFrame()) {
        T object;
        if (objectClass != null) {
          object = f.getProvider().getObject(objectClass, depth);
        } else {
          assert key != null;
          object = f.getProvider().getObject(key, depth);
        }
        if (object != null) {
          return object;
        }
        if (!f.isInherit()) {
          break;
        }
      }
      return null;
    } finally {
      if (Context.DEBUG) {
        untraceCallStack(frame);
      }
    }
  }

  private static void untraceCallStack(ContextFrame frame) {
    LinkedHashSet<ContextFrame> frames = ourCallStack.get();
    if (frames == null) {
      Log.error("null frames");
    } else {
      boolean removed = frames.remove(frame);
      if (!removed)
        Log.error("cannot remove frame " + frame);
    }
  }

  private static Failure traceCallStack(ContextFrame frame, Class objectClass, TypedKey key) {
    LinkedHashSet<ContextFrame> frames = ourCallStack.get();
    if (frames == null) {
      frames = Collections15.linkedHashSet();
      ourCallStack.set(frames);
    }
    if (frames.contains(frame)) {
      assert false : "recursive entry into frame for " + objectClass + ":" + key + " [" + frame + "]";
      return new Failure("recursive entry into frame for " + objectClass + ":" + key + " [" + frame + "]");
    }
    frames.add(frame);
    return null;
  }
}
