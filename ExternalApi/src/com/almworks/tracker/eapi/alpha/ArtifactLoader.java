package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.EventSource;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;

/**
 * This interface allows to request artifact information from Deskzilla.
 * <p>
 * Subscribe to events() to receive ArtifactInfoEvents.
 * <p>
 * ArtifactLoader accumulates subscriptions from different entities in your code, which are identified by keys.
 * Subscription in Deskzilla is maintained while there's at least one active key.
 * <p>
 * You can subscribe any number of times. If you subscribe to the same URLs with different options, ArtifactLoader will
 * check if there are additional options to be requested from Deskzilla, and act correspondingly.
 * <p>
 * When there's an active subscription to an artifact, multiple events regarding this artifact will come to <b>all</b>
 * events() subscribers.
 *
 * @see ArtifactInfo
 */
public interface ArtifactLoader {
  /**
   * Subscribe to artifacts. Updates will go to all subscribers for events().
   *
   * @param key any object that could be associated with subscription. Needs to have correct equals() and hashCode().
   * @param life life span for subscription. When it ends, these keys/artifacts will be unsubscribed. (Even if there
   *   were multiple subscriptions for the same key with different life spans!)
   * @param urls Artifact URLs to be retrieved.
   * @param options Options for Deskzilla processing. Use preset arrays in ArtifactLoadOption.
   *
   * @see ArtifactLoadOption
   */
  void subscribeArtifacts(Object key, Lifespan life, Collection<String> urls, ArtifactLoadOption[] options);

  /**
   * Unsubscribe from artifacts. Events for an artifact will stop to come when <b>all</b> keys are unsubscribed.
   *
   * @param key a key previously used to subscribe to these artifacts.
   * @param urls artifact URLs to be unsubscribed from.
   */
  void unsubscribeArtifacts(Object key, Collection<String> urls);

  /**
   * Constant event source for artifact events. Listeners will receive events for all artifacts that have been
   * requested.
   */
  EventSource<ArtifactInfoEvent> events();
}
