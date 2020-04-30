package com.almworks.syncreg;

import org.almworks.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

final class NumberedCube {
  private final String[] myAttributes;
  private final long[][] myIncludedValues;
  private final long[][] myExcludedValues;

  public NumberedCube(String[] attributes, long[][] includedValues, long[][] excludedValues) {
    myAttributes = attributes;
    myIncludedValues = includedValues;
    myExcludedValues = excludedValues;
  }

  public int getAttributeIndex(String attrID) {
    return ArrayUtil.indexOf(myAttributes, attrID);
  }

  @Nullable
  public long[] getIncludedValues(int attributeIndex) {
    return myIncludedValues[attributeIndex];
  }

  @Nullable
  public long[] getExcludedValues(int attributeIndex) {
    return myExcludedValues[attributeIndex];
  }

  public String[] getAttributes() {
    return myAttributes;
  }
}
