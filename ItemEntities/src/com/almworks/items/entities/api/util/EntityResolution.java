package com.almworks.items.entities.api.util;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.*;

public class EntityResolution {
  public static final EntityKey<EntityResolution> KEY = EntityKey.hint("sys.res.key.resolution", EntityResolution.class);
  private final Collection<Collection<EntityKey<?>>> myIdentities;
  private final Collection<Collection<EntityKey<?>>> mySearchBy;
  private final int myHashCode;
  private final boolean myIncludeConnection;

  private EntityResolution(Collection<Collection<EntityKey<?>>> identities,
    Collection<Collection<EntityKey<?>>> searchBy, boolean includeConnection)
  {
    myIncludeConnection = includeConnection;
    if (searchBy == null) searchBy = Collections15.emptyCollection();
    myIdentities = identities;
    mySearchBy = searchBy;
    myHashCode = calcHashCode(identities, searchBy);
  }

  private static int calcHashCode(Collection<Collection<EntityKey<?>>> identities, Collection<Collection<EntityKey<?>>> searchBy) {
    return calcHash(identities) ^ calcHash(searchBy);
  }

  private static int calcHash(Collection<Collection<EntityKey<?>>> identities) {
    int hash = 0;
    for (Collection<EntityKey<?>> keys : identities) for (EntityKey<?> key : keys) hash = hash ^ key.hashCode();
    return hash;
  }

  public boolean includeConnection() {
    return myIncludeConnection;
  }

  public boolean isSlave() {
    for (Collection<EntityKey<?>> identity : myIdentities) {
      if (!isSlave(identity)) return false;
    }
    return true;
  }

  private boolean isSlave(Collection<EntityKey<?>> identity) {
    for (EntityKey<?> key : identity) if (EntityKeyProperties.isMasterRef(key)) return true;
    return false;
  }

  public Collection<Collection<EntityKey<?>>> getIdentities() {
    return myIdentities;
  }

  public Collection<Collection<EntityKey<?>>> getSearchBy() {
    return mySearchBy;
  }

  @Override
  public int hashCode() {
    return myHashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    EntityResolution other = Util.castNullable(EntityResolution.class, obj);
    if (other == null) return false;
    if (hashCode() != other.hashCode()) return false;
    if (myIdentities.size() != other.myIdentities.size() || mySearchBy.size() != other.mySearchBy.size()) return false;
    return equal(myIdentities, other.myIdentities) && equal(mySearchBy, other.mySearchBy);
  }

  private boolean equal(Collection<Collection<EntityKey<?>>> col1, Collection<Collection<EntityKey<?>>> col2) {
    if (col1 == col2) return true;
    List<Collection<EntityKey<?>>> list = Collections15.arrayList(col1);
    for (Collection<EntityKey<?>> keys2 : col2) {
      int index = findEqual(list, keys2);
      if (index < 0) return false;
      list.set(index, null);
    }
    return true;
  }

  private int findEqual(List<Collection<EntityKey<?>>> list, Collection<EntityKey<?>> value) {
    for (int i = 0; i < list.size(); i++) {
      Collection<EntityKey<?>> keys = list.get(i);
      if (keys == null) continue;
      if (areEqual(keys, value)) return i;
    }
    return -1;
  }

  private boolean areEqual(Collection<EntityKey<?>> keys, Collection<EntityKey<?>> value) {
    if (keys.size() != value.size()) return false;
    if (keys.hashCode() != value.hashCode()) return false;
    List<EntityKey<?>> list = Collections15.arrayList(keys);
    for (EntityKey<?> key : value) {
      int index = list.indexOf(key);
      if (index < 0) return false;
      list.set(index, null);
    }
    return true;
  }

  /**
   * Allows create. Entities are resolved by all values of given keys, i.e. search for (k1=v1 & k2=v2 & ... )
   * @param keys resolution keys
   * @return resolution for find or create new item.
   */
  public static EntityResolution singleIdentity(boolean includeConnection, EntityKey<?> ... keys) {
    List<Collection<EntityKey<?>>> a = toDoubleCollection(keys);
    return new EntityResolution(a, null, includeConnection);
  }

  public static EntityResolution searchable(boolean includeConnection, Collection<? extends EntityKey<?>> search, EntityKey<?> ... identity) {
    return new EntityResolution(toDoubleCollection(identity),
      Collections.singleton((Collection<EntityKey<?>>)Collections15.unmodifiableListCopy(search)), includeConnection);
  }

  public static EntityResolution singleAttributeIdentities(boolean includeConnection, EntityKey<?> ... keys) {
    List<Collection<EntityKey<?>>> identities = Collections15.arrayList();
    for (EntityKey<?> key : keys) identities.add(Collections.<EntityKey<?>>singleton(key));
    return new EntityResolution(identities, null, includeConnection);
  }

  private static List<Collection<EntityKey<?>>> toDoubleCollection(EntityKey<?>[] keys) {
    return Collections15.<Collection<EntityKey<?>>>unmodifiableListCopy(Collections15.unmodifiableListCopy(keys));
  }

  public Collection<EntityKey<?>> getAllKeys() {
    Set<EntityKey<?>> result = Collections15.hashSet();
    for (Collection<EntityKey<?>> keys : myIdentities) result.addAll(keys);
    for (Collection<EntityKey<?>> keys : mySearchBy) result.addAll(keys);
    return result;
  }

  public static String printResolutionValues(Entity entity) {
    StringBuilder builder = new StringBuilder();
    printResolutionValues(builder, entity);
    return builder.toString();
  }

  public static void printResolutionValues(StringBuilder builder, Entity entity) {
    if (entity == null) {
      builder.append("<null>");
      return;
    }
    Entity type = entity.getType();
    if (type == null) {
      builder.append("<no type>");
      return;
    }
    EntityResolution resolution = type.get(KEY);
    if (resolution == null) {
      builder.append("<no resolution>");
      return;
    }
    ArrayList<EntityKey<?>> keys = Collections15.arrayList(resolution.getAllKeys());
    Collections.sort(keys, EntityKey.ID_ORDER);
    builder.append("(");
    String sep = "";
    for (EntityKey<?> key : keys) {
      builder.append(sep).append(key.getId()).append("=").append(entity.get(key));
      sep = ", ";
    }
    builder.append(")");
  }
}
