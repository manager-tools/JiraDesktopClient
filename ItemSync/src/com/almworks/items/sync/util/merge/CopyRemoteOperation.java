package com.almworks.items.sync.util.merge;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.AutoMergeData;
import org.almworks.util.ArrayUtil;

/**
 * Discard edit of specified attributes.<br>
 * This algorithm is intended to be used when some attributes cannot be upload if there is no other changes. So after
 * implication of this algorithm the item becomes synchronized if only specified attributes are edited. Otherwise
 * no changes are made to the item. 
 */
public class CopyRemoteOperation extends SimpleAutoMerge {
  private final DBAttribute<?>[] myAttribute;

  public CopyRemoteOperation(DBAttribute<?> ... attribute) {
    myAttribute = ArrayUtil.arrayCopy(attribute);
  }

  @Override
  public void resolve(AutoMergeData data) {
    for (DBAttribute<?> attribute : myAttribute) data.discardEdit(attribute);
  }
}
