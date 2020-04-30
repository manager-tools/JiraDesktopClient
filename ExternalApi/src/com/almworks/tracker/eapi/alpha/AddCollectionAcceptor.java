package com.almworks.tracker.eapi.alpha;

/**
 * Implement this interface to receive requests from Deskzilla to
 * add a collection to the list of monitored collections.
 */
public interface AddCollectionAcceptor {
  void acceptAddCollection(CollectionData collection);
}
