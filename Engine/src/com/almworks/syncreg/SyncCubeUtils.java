package com.almworks.syncreg;

import com.almworks.api.syncreg.Hypercube;
import com.almworks.items.api.DBAttribute;

import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;

class SyncCubeUtils {
  public static NumberedCube convert(Hypercube<DBAttribute<?>, Long> cube) {
    Set<DBAttribute<?>> axes = cube.getAxes();
    int axisCount = axes.size();
    String[] attributes = new String[axisCount];
    long[][] included = new long[axisCount][];
    long[][] excluded = new long[axisCount][];
    int i = 0;
    for (DBAttribute<?> attribute : axes) {
      attributes[i] = attribute.getId();
      fillArray(i, included, cube.getIncludedValues(attribute));
      fillArray(i, excluded, cube.getExcludedValues(attribute));
      i++;
    }
    return new NumberedCube(attributes, included, excluded);
  }

  public static void fillArray(int axisIndex, long[][] targetArray, SortedSet<Long> values) {
    if (values != null && values.size() > 0) {
      targetArray[axisIndex] = new long[values.size()];
      int j = 0;
      for (Long value : values)
        targetArray[axisIndex][j++] = value;
      Arrays.sort(targetArray[axisIndex]);
    }
  }
}
