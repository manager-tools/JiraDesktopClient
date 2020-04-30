package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.EventSource;
import org.almworks.util.detach.Lifespan;

/**
 * Allows to subscribe to a collection in Deskzilla (a query or a bug set, or other node).
 * <p>
 * When watchCollection is called, Deskzilla begins to monitor all bugs that belong
 * to this collection. When bug set changes, this interface calls registered listeners
 * with CollectionUpdateEvents. Each event contains full set of URLs for the connection.
 * <p>
 * All bugs that are currently in the collection are also automatically subscribed to.
 * That is, you don't have to ask ArtifactLoader to subscribe to URLs received with CollectionUpdateEvent.
 * But you have to subscribe to ArtifactLoader events to receive artifact data.
 */
public interface ArtifactCollectionMonitor {
  /**
   * Watch collection, identified by ID. If the id is invalid, one CollectionUpdateEvent
   * will be received with "valid" property set to false.
   */
  void watchCollection(Object key, Lifespan life, String collectionId);

  /**
   * Source of CollectionUpdateEvents. When collection becomes unavailable
   * (for example, user deletes it in Deskzilla), an update event will come
   * with "valid" property set to false.
   */
  EventSource<CollectionUpdateEvent> events();
}
