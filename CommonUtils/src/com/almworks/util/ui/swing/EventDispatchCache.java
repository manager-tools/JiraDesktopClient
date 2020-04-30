package com.almworks.util.ui.swing;

import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.exec.Context;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides cache for single AWT event. When using the cache make sure no modal dialog can be open during event processing. In such case cache life may become too long
 * (till dialog is closed, and original event processing finishes).
 * @see #startEventDispatch()
 * @see #stopEventDispatch(Object)
 */
public class EventDispatchCache {
  /**
   * &lt;Yes, No&gt; sets
   */
  private static final TypedKey<Pair<HashSet<Component>, HashSet<Component>>> FOCUS_OWNERS = TypedKey.create("focusOwner");
  
  private final Map<TypedKey<?>, ?> myCache = new ConcurrentHashMap<TypedKey<?>, Object>();
  private volatile boolean myStopped = false;
  
  private EventDispatchCache() {
  }
  
  private static final Object ourLock = new Object();
  private static final List<EventDispatchCache> ourCaches = Collections15.arrayList();

  /**
   * Installs cache for.
   * @return mark for the installed cache, so later the cache can be uninstalled. If there is already any cache active than it deactivated and cleared.
   */
  public static Object startEventDispatch() {
    if (!Context.isAWT()) return null;
    synchronized (ourLock) {
      for (EventDispatchCache cache : ourCaches) {
        cache.stop();
      }
      EventDispatchCache cache = new EventDispatchCache();
      ourCaches.add(cache);
      return cache;
    }
  }

  /**
   * Uninstall cache
   * @param mark
   */
  public static void stopEventDispatch(Object mark) {
    if (mark == null) return;
    synchronized (ourLock) {
      if (ourCaches.isEmpty()) {
        LogHelper.error("No events during dispatch", mark);
        return;
      }
      if (ourCaches.get(ourCaches.size() - 1) != mark) {
        LogHelper.error("Wrong mark", mark, ourCaches);
        ourCaches.clear();
        return;
      }
      ourCaches.remove(ourCaches.size() - 1).stop();
    }
  }
  
  @Nullable
  public static <T> T getValue(TypedKey<T> key) {
    EventDispatchCache cache = getCurrentCache();
    return cache != null ? cache.priGetValue(key) : null;
  }

  public static <T> void setValue(TypedKey<T> key, T value) {
    EventDispatchCache cache = getCurrentCache();
    if (cache != null) cache.priPutValue(key, value);
  }

  /**
   * Check is any cache is available. The cache may be deactivated.
   * @return
   */
  public static boolean hasCache() {
    return getCurrentCache() != null;
  }
  
  @Nullable
  private static EventDispatchCache getCurrentCache() {
    synchronized (ourLock) {
      if (ourCaches.isEmpty()) return null;
      return ourCaches.get(ourCaches.size() - 1);
    }
  }
  
  private <T> T priGetValue(TypedKey<T> key) {
    if (myStopped) return null;
    return key.getFrom(myCache);
  }
  
  private <T> void priPutValue(TypedKey<T> key, T value) {
    if (myStopped) return;
    key.putTo(myCache, value);
    if (myStopped) myCache.clear();
  }

  private void stop() {
    myStopped = true;
    myCache.clear();
  }

  /**
   * Cached call to {@link java.awt.Component#isFocusOwner()}
   */
  public static boolean isFocusOwner(Component component) {
    if (!hasCache()) return component.isFocusOwner();
    Pair<HashSet<Component>, HashSet<Component>> pair = getValue(FOCUS_OWNERS);
    if (pair != null) {
      if (pair.getFirst().contains(component)) return true;
      if (pair.getSecond().contains(component)) return false;
    }
    if (pair == null) {
      pair = Pair.create(Collections15.<Component>hashSet(), Collections15.<Component>hashSet());
      setValue(FOCUS_OWNERS, pair);
    }
    boolean focusOwner = component.isFocusOwner();
    (focusOwner ? pair.getFirst() : pair.getSecond()).add(component);
    return focusOwner;
  }
}
