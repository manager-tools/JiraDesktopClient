package com.almworks.util.model;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Concurrent collection model. Supported changes: elements added or removed. Replacement of an element with
 * equal does not produces change.<br><br>
 * Listeners are notified without external (model) locks held. After a listener is added it is notified with
 * current model state (like it was added to an empty model and all elements appeared right after it started
 * listen model).<br><br>
 * Model and event has version, listener should hold last processed version and actualize event each time it
 * arrives. If the listener is thread confined by thread gate (AWT gate) it need not use locks, but if
 * the listener is not thread confined (straight listener) it should actualize event and update processed version
 * under its own lock to avoid processing of concurrent events.
 * <pre>
 * public void onSetChanged(Event event) {
 *   synchronized (this.lock) { // locks are not needed for thread confined listeners
 *     this.version = event.{@link com.almworks.util.model.SetHolder.Event#actualize(long) actualize}(this.version);
 *     if (event.{@link com.almworks.util.model.SetHolder.Event#isEmpty() isEmpty}()) return;
 *     // use event
 *   }
 *   // use event
 * }
 * </pre>
 * ATTENTION: if two events are dispatched by different thread event process outside of synchronized block may lead
 * to wrong order.<br>
 * For example let event E1 contains add elements, and E2 contains remove the elements. Thread
 * that processes E1 left synchronized block and execution switches to thread that processes E2. E2 processed up to
 * method end, than execution switches back to E1. Here elements removed by E2 are processed before listener gets to
 * know that elements are added.<br><br>
 * at same time or in wrong order.<br><br>
 * After event is actualized it may contain changes made later or even by other thread.<br>
 * Changes can be concatenated and annihilated. For example if some one added an element and some other removed it
 * and the listener was not notified that element was added it wont be notified at all (like the element was not
 * ever be added).
 * @param <T>
 */
public interface SetHolder<T> {
  // todo sooner than what?
  /**
   * Adds listener. The listener is notified about actual model state sooner.
   * @param life remove listener when ended
   * @param gate thread gate to send all notifications including first
   * @param listener the listener
   */
  void addInitListener(Lifespan life, ThreadGate gate, Listener<T> listener);

  /**
   * Remove the listener
   */
  void removeListener(Listener<?> listener);

  /**
   * Copy current "model" state. Actually copies state one of past states the model.
   * @return elements
   */
  @NotNull
  List<T> copyCurrent();

  boolean isEmpty();

  interface Event<T> {
    /**
     * Actualizes event. After invokation of the method the event contains changes from given version up to
     * {@link #getNewVersion()}.<br>
     * @param fromVersion previously processed model version.<br>
     * Valid values are:<br>
     * 0 return current model state as event<br>
     * positive number - changes starting from given version. Only versions that were not seen yet are valid.
     * @return the new version this event is actual for. The event contains changes up to this version.
     */
    long actualize(long fromVersion);

    long getNewVersion();

    /**
     * Check is event contains no changes. Actualized event may happen to be empty since all changes has been
     * already processed.
     * @return true if nothing was added or removed
     */
    boolean isEmpty();

    @NotNull
    Collection<T> getAdded();

    @NotNull
    Collection<T> getRemoved();

    SetHolder<T> getSet();
  }

  interface Listener<T> {
    /**
     * @param event should not be used after method return
     */
    void onSetChanged(@NotNull Event<T> event);
  }
}
