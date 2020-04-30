package com.almworks.items.entities.dbwrite;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.util.text.TextUtil;

import java.util.Arrays;

/**
 * Represent a entity resolution problem. Such problem may be caused by:<br>
 * 1. Wrong entity data - such entities cannot be written in any case<br>
 * 2. Unlucky DB state - an entity can not be written because of current DB state and it surely can be written in other DB
 * state. In this case the problematic entity is provided with the exception.
 */
public class ResolutionException extends Exception {
  /**
   * The entity that can be written to DB because of current DB state. More data is required for the entity to perform write.
   */
  private final Entity myBadEntity;


  public ResolutionException(String message, Entity badEntity) {
    super(message);
    myBadEntity = badEntity;
  }

  public Entity getBadEntity() {
    return myBadEntity;
  }

  public static ResolutionException error(Object ... description) {
    String message = description != null ? TextUtil.separateToString(Arrays.asList(description), " ") : "";
    return new ResolutionException(message, null);
  }

  public static ResolutionException insufficientData(Entity entity, String comment) {
    return new ResolutionException(comment + " " + entity + " " + EntityResolution.printResolutionValues(entity), entity);
  }
}
