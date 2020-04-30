package com.almworks.items.entities.api.util;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import org.almworks.util.Util;

public class EntityKeyProperties {
  public static final int LINK_KIND_GENERIC = 0;
  public static final int LINK_KIND_PROPAGATE = 1;
  public static final int LINK_KIND_MASTER = 2;

  private static final EntityKey<Integer> LINK_KIND = EntityKey.hint("sys.res.key.linkKind", Integer.class);
  private static final EntityKey<Boolean> SHADOWABLE = EntityKey.hint("sys.res.key.shadowable", Boolean.class);


  public static boolean isShadowable(Entity key) {
    return key != null && Boolean.TRUE.equals(key.get(SHADOWABLE));
  }

  public static boolean isShadowable(EntityKey<?> key) {
    return isShadowable(key.toEntity());
  }

  public static boolean isMasterRef(Entity key) {
    int kind = getLinkKind(key);
    return kind == LINK_KIND_MASTER;
  }

  public static boolean isMasterRef(EntityKey<?> key) {
    return isMasterRef(key.toEntity());
  }

  public static int getLinkKind(Entity key) {
    return Util.NN(key.get(LINK_KIND), 0);
  }

  /**
   * Method to construct master reference.
   * @return value for second (description) parameter of EntityKey constructor methods
   * @see com.almworks.items.entities.api.EntityKey#bool(String, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#bytes(String, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#collection(String, Class, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#date(String, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#entity(String, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#entityCollection(String, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#integer(String, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#longInt(String, com.almworks.items.entities.api.Entity)
   * @see com.almworks.items.entities.api.EntityKey#string(String, com.almworks.items.entities.api.Entity)
   */
  public static Entity master() {
    Entity key = EntityKey.buildKey();
    key.put(LINK_KIND, LINK_KIND_MASTER);
    return key;
  }

  /**
   * Method to construct link to another entity and propagate changes
   * @see #master()
   */
  public static Entity propagateChange(boolean shadowable) {
    Entity key = EntityKey.buildKey();
    key.put(LINK_KIND, LINK_KIND_PROPAGATE);
    if (shadowable) key.put(SHADOWABLE, true);
    return key;
  }

  /**
   * Method to construct shadowable attributes
   * @return value for second (description) parameter of EntityKey constructor methods
   * @see EntityKeyProperties#master()
   */
  public static Entity shadowable() {
    Entity key = EntityKey.buildKey();
    key.put(SHADOWABLE, true);
    return key;
  }
}
