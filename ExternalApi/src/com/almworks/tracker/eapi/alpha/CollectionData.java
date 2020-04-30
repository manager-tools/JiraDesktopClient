package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.TypedKey;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents collection, which can be a Deskzilla query, bug set, etc.
 */
public class CollectionData {
  public static final TypedKey<String> COLLECTION_NAME = new TypedKey<String>("COLLECTION_NAME");
  public static final TypedKey<Icon> COLLECTION_ICON = new TypedKey<Icon>("COLLECTION_ICON");

  private final String myCollectionId;
  private final Map<TypedKey<?>, ?> myCollectionProperties;
  private final boolean myValid;

  public CollectionData(String collectionId, boolean valid, Map collectionProperties) {
    myCollectionId = collectionId;
    myValid = valid;
    myCollectionProperties = collectionProperties == null ? null : new HashMap<TypedKey<?>, Object>(collectionProperties);
  }

  /**
   * Collection ID is not a displayable property. Not null.
   */
  public String getCollectionId() {
    return myCollectionId;
  }

  /**
   * Used to get user-level properties about a collection.
   * Nullable.
   */
  public <T> T getProperty(TypedKey<T> key) {
    return myCollectionProperties == null ? null : key.getFromMap(myCollectionProperties);
  }

  /**
   * Returns false if this collection is not valid, i.e. not existing or failing otherwise.
   * No artifact URLs are expected to come from this collection. 
   */
  public boolean isValid() {
    return myValid;
  }
}
