package com.almworks.api.syncreg;

import com.almworks.items.api.DBAttribute;
import org.jetbrains.annotations.Nullable;

public interface ItemHypercube extends Hypercube<DBAttribute<?>, Long> {
  ItemHypercube copy();

  boolean isSame(@Nullable ItemHypercube another);

  /**
   * @param cube
   * @return intersection. If intersection is empty and precise cube is requested return null. <br>
   * If not precise cube requested removes axes which has empty intersection.<br>
   * Example:
   *  let C1=(a1: +1,2; a2: +1) and C2=(a1: +1,3; a2: +2) <br>
   *  presize intersection is empty as cubes dont intersect on axis a2. So C1.intersect(C2, true) returns null
   * but C1.intersect(C2, false) returns (a1: +1)
   *
   */
  @Nullable ("if requested precise and intersection is empty for one of axes")
  ItemHypercube intersect(@Nullable ItemHypercube cube, boolean precise);
}
