package com.almworks.syncreg;

import com.almworks.util.io.persist.FormatException;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import util.external.CompactChar;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

final class EquidimensionalSet {
  private static final int MAX_CUBES = 10000;
  private static final int MAX_VALUES_COUNT = 100000;

  /**
   * number of dimensions
   */
  private final int myDimensions;

  /**
   * number of hypercubes
   */
  private int myCubes;

  /**
   * number of legitimate elements in myValues. physical array length could be greater
   */
  private int myValuesCount;

  /**
   * contains indexes for values, for each attribute for each cube.
   * offset = (cubeIndex * myDimensions + attributeIndex) * 4
   * [offset + 0] = index of first positive value in myValues, or -1 if no positives
   * [offset + 1] = count of positive values, or -1 if no positives
   * [offset + 2] = index of first negative value in myValues, or -1 if no negatives
   * [offset + 3] = count of negative values, or -1 if no negatives
   */
  private int[] myValuesIndex;

  /**
   * contains tuples of attributes for each cube. for example, if myDimension = 3,
   * contains (a1, a2, a3), (b1, b2, b3), etc.
   * has at least myDimensions * myCubes elements
   */
  private String[] myAttributes;

  /**
   * Storage place for values. Storage is allocated at the end of array.
   * todo deallocation
   * arbitrary-length value tuples that are indexed through myValuesIndex
   * within each tuple, values are sorted
   */
  private long[] myValues;


  public EquidimensionalSet(int dimensions) {
    myDimensions = dimensions;
  }

  public void addCube(NumberedCube numberedCube) {
    // todo find adjacent hypercubes
    int cubeIndex = myCubes++;
    String[] attributes = numberedCube.getAttributes();
    assert attributes.length == myDimensions : numberedCube;
    ensureAttributesLength(myCubes);
    System.arraycopy(attributes, 0, myAttributes, cubeIndex * myDimensions, Math.min(myDimensions, attributes.length));

    for (int attributeIndex = 0; attributeIndex < myDimensions; attributeIndex++) {
      int offset = (cubeIndex * myDimensions + attributeIndex) * 4;
      long[] includedValues = numberedCube.getIncludedValues(attributeIndex);
      long[] excludedValues = numberedCube.getExcludedValues(attributeIndex);
      ensureIndexLength(offset + 4);
      writeIndexAndValues(offset, includedValues);
      writeIndexAndValues(offset + 2, excludedValues);
    }
  }

  private void ensureAttributesLength(int cubesCount) {
    if (myAttributes == null || myAttributes.length < cubesCount * myDimensions) {
      int newSize = ((myAttributes == null ? 0 : myAttributes.length) + myDimensions) * 2;
      String[] array = newStringArray(Math.max(newSize, cubesCount * myDimensions));
      if (myAttributes != null) {
        System.arraycopy(myAttributes, 0, array, 0, myAttributes.length);
      }
      myAttributes = array;
    }
  }

  private void ensureIndexLength(int length) {
    if (myValuesIndex == null || myValuesIndex.length < length) {
      int currentLength = myValuesIndex == null ? 0 : myValuesIndex.length;
      int[] arr = new int[Math.max((currentLength + 4) * 2, length)];
      if (myValuesIndex != null) {
        System.arraycopy(myValuesIndex, 0, arr, 0, myValuesIndex.length);
      }
      myValuesIndex = arr;
    }
  }

  private void writeIndexAndValues(int indexOffset, long[] values) {
    if (values == null || values.length == 0) {
      myValuesIndex[indexOffset] = -1;
      myValuesIndex[indexOffset + 1] = -1;
    } else {
      int length = values.length;
      ensureValuesLength(myValuesCount + length);
      System.arraycopy(values, 0, myValues, myValuesCount, length);
      myValuesIndex[indexOffset] = myValuesCount;
      myValuesIndex[indexOffset + 1] = length;
      myValuesCount += length;
    }
  }

  private void ensureValuesLength(int length) {
    if (myValues == null || myValues.length < length) {
      int currentLength = myValues == null ? 0 : myValues.length;
      long[] array = newLongArray(Math.max((currentLength + 1) * 2, length));
      if (myValues != null) {
        System.arraycopy(myValues, 0, array, 0, myValues.length);
      }
      myValues = array;
    }
  }

  public void clear() {
    myCubes = 0;
    myAttributes = Const.EMPTY_STRINGS;
    myValues = Const.EMPTY_LONGS;
    myValuesCount = 0;
    myValuesIndex = Const.EMPTY_INTS;
  }

  /**
   * @return true if this set contains a hypercube with same or more generic constraints
   */
  public boolean encompasses(NumberedCube cube) {
    for (int i = 0; i < myCubes; i++) {
      if (encompasses(i, cube))
        return true;
    }
    return false;
  }

  public void removeEncompassedBy(NumberedCube cube) {
    int i = 0;
    while (i < myCubes) {
      if (encompassedBy(i, cube)) {
        remove(i);
      } else {
        i++;
      }
    }
  }

  /**
   * @return true if the argument encompasses the contained sample (index cubeIndex)
   *         it happens only if:
   *         a) each dimension in sample is represented in my sample
   *         b) each dimension in sample encompasses corresponding dimension in my sample
   */
  private boolean encompassedBy(int cubeIndex, NumberedCube sample) {
    String[] attributes = sample.getAttributes();
    for (int sampleIndex = 0; sampleIndex < attributes.length; sampleIndex++) {
      String attributeID = attributes[sampleIndex];
      int attributeIndex = findAttribute(cubeIndex, attributeID);
      if (attributeIndex == -1) {
        // my cube does not have attribute specified by sample cube
        return false;
      }
      long[] cubeIncluded = sample.getIncludedValues(sampleIndex);
      long[] cubeExcluded = sample.getExcludedValues(sampleIndex);
      int offset4 = (cubeIndex * myDimensions + attributeIndex) * 4;
      int includesStart = myValuesIndex[offset4];
      int includesCount = myValuesIndex[offset4 + 1];
      int excludesStart = myValuesIndex[offset4 + 2];
      int excludesCount = myValuesIndex[offset4 + 3];
      boolean dimensionEncompasses = dimensionEncompasses(cubeIncluded, 0,
        cubeIncluded == null ? 0 : cubeIncluded.length, cubeExcluded, 0, cubeExcluded == null ? 0 : cubeExcluded.length,
        myValues, includesStart, includesCount, myValues, excludesStart, excludesCount);
      if (!dimensionEncompasses) {
        // sample dimension is less generic
        return false;
      }
    }
    return true;
  }

  private int findAttribute(int cubeIndex, String attributeID) {
    int offset = cubeIndex * myDimensions;
    for (int i = 0; i < myDimensions; i++) {
      if (Util.equals(myAttributes[offset + i], attributeID))
        return i;
    }
    return -1;
  }

  /**
   * Returns true if hyperplane called "super" encompasses hyperplane called "sub".
   * superIncluded, superIncludedStart, superIncludedCount - define the array that contains "included" values in super
   * superExcludedXXX, subIncludedXXX, subExcludedXXX - the same
   */
  private boolean dimensionEncompasses(long[] superIncluded, int superIncludedStart, int superIncludedCount,
    long[] superExcluded, int superExcludedStart, int superExcludedCount, long[] subIncluded, int subIncludedStart,
    int subIncludedCount, long[] subExcluded, int subExcludedStart, int subExcludedCount)
  {
    if (superExcludedStart >= 0 && superExcluded != null) {
      if (subIncludedStart >= 0 && subIncluded != null) {
        if (ArrayUtil.hasIntersection(subIncluded, subIncludedStart, subIncludedCount, superExcluded,
          superExcludedStart, superExcludedCount))
        {
          // sample includes some values for attribute that are specifically excluded in my cube
          return false;
        }
      } else {
        if (subExcludedStart >= 0 && subExcluded != null) {
          if (!ArrayUtil.containsAll(subExcluded, subExcludedStart, subExcludedCount, superExcluded,
            superExcludedStart, superExcludedCount))
          {
            // sample does not exclude all attributes that are excluded in my cube
            return false;
          }
        }
      }
    }

    if (superIncludedStart >= 0 && superIncluded != null) {
      if (subIncludedStart >= 0 && subIncluded != null) {
        if (!ArrayUtil.containsAll(superIncluded, superIncludedStart, superIncludedCount, subIncluded,
          subIncludedStart, subIncludedCount))
        {
          // sample has some other allowed attribute value than the values in my cube
          return false;
        }
      } else if (subExcludedStart >= 0 && subExcluded != null) {
        // cannot tell if a set defined by inclusion contains a set defined by exclusion without having the full set
        return false;
      }
    }
    return true;
  }

  public void removeEncompassing(NumberedCube numberedCube) {
    int i = 0;
    while (i < myCubes) {
      if (encompasses(i, numberedCube)) {
        remove(i);
      } else {
        i++;
      }
    }
  }

  /**
   * @return true if the contained cube (index cubeIndex) encompasses the argument
   *         it happens only if:
   *         a) each dimension in my cube is represented in sample
   *         b) each dimension in my cube encompasses corresponding dimension in sample
   */
  private boolean encompasses(int cubeIndex, NumberedCube sample) {
    for (int attributeIndex = 0; attributeIndex < myDimensions; attributeIndex++) {
      int offset = cubeIndex * myDimensions + attributeIndex;
      String attributeID = myAttributes[offset];
      int sampleIndex = sample.getAttributeIndex(attributeID);
      if (sampleIndex == -1) {
        // sample does not have this attribute; thus we are more specific than the sample
        return false;
      }
      long[] sampleIncluded = sample.getIncludedValues(sampleIndex);
      long[] sampleExcluded = sample.getExcludedValues(sampleIndex);
      int offset4 = offset * 4;
      int includesStart = myValuesIndex[offset4];
      int includesCount = myValuesIndex[offset4 + 1];
      int excludesStart = myValuesIndex[offset4 + 2];
      int excludesCount = myValuesIndex[offset4 + 3];

      boolean dimensionEncompasses = dimensionEncompasses(myValues, includesStart, includesCount, myValues,
        excludesStart, excludesCount, sampleIncluded, 0, sampleIncluded == null ? 0 : sampleIncluded.length,
        sampleExcluded, 0, sampleExcluded == null ? 0 : sampleExcluded.length);

      if (!dimensionEncompasses) {
        // this dimension is less generic
        return false;
      }
    }
    return true;
  }

  private void remove(int cubeIndex) {
    assert cubeIndex < myCubes;
    int offset = cubeIndex * myDimensions;
    for (int attributeIndex = 0; attributeIndex < myDimensions; attributeIndex++) {
      int offset4 = (offset + attributeIndex) * 4;
      removeValues(offset4);
      removeValues(offset4 + 2);
    }
    int moveCount = (myCubes - 1 - cubeIndex) * myDimensions;
    System.arraycopy(myValuesIndex, (offset + myDimensions) * 4, myValuesIndex, offset * 4, moveCount * 4);
    System.arraycopy(myAttributes, offset + myDimensions, myAttributes, offset, moveCount);
    myCubes--;
  }

  private void removeValues(int ii) {
    // todo do nothing here; add compactization
  }

  public void save(DataOutput out) throws IOException {
    CompactInt.writeInt(out, myDimensions);
    CompactInt.writeInt(out, myCubes);
    CompactInt.writeInt(out, myValuesCount);
    int attributeCount = myCubes * myDimensions;
    for (int i = 0; i < attributeCount; i++)
      CompactChar.writeString(out, myAttributes[i]);
    int indexCount = attributeCount * 4;
    for (int i = 0; i < indexCount; i++)
      CompactInt.writeInt(out, myValuesIndex[i]);
    for (int i = 0; i < myValuesCount; i++)
      CompactInt.writeLong(out, myValues[i]);
  }

  public void load(DataInput in) throws IOException {
    int dims = CompactInt.readInt(in);
    if (dims != myDimensions)
      throw new FormatException(myDimensions + " " + dims);
    int cubes = CompactInt.readInt(in);
    if (cubes < 0 || cubes >= MAX_CUBES)
      throw new FormatException("" + cubes);
    int valuesCount = CompactInt.readInt(in);
    if (valuesCount < 0 || valuesCount >= MAX_VALUES_COUNT)
      throw new FormatException("" + valuesCount);
    int attributeCount = cubes * myDimensions;
    String[] attributes = newStringArray(attributeCount);
    for (int i = 0; i < attributeCount; i++)
      attributes[i] = CompactChar.readString(in);
    int indexCount = attributeCount * 4;
    int[] index = new int[indexCount];
    for (int i = 0; i < indexCount; i++)
      index[i] = CompactInt.readInt(in);
    long[] values = newLongArray(valuesCount);
    for (int i = 0; i < valuesCount; i++)
      values[i] = CompactInt.readLong(in);
    myCubes = cubes;
    myValuesCount = valuesCount;
    myAttributes = attributes;
    myValuesIndex = index;
    myValues = values;
  }

  private long[] newLongArray(int valuesCount) {
    return valuesCount == 0 ? Const.EMPTY_LONGS :  new long[valuesCount];
  }

  private String[] newStringArray(int valuesCount) {
    return valuesCount == 0 ? Const.EMPTY_STRINGS :  new String[valuesCount];
  }

  /**
   * needed for testing. we don't override equals() because this class is not used for
   * hashable objects
   */
  boolean equalSet(EquidimensionalSet that) {
    if (myDimensions != that.myDimensions)
      return false;
    if (myCubes != that.myCubes)
      return false;
    if (myValuesCount != that.myValuesCount)
      return false;
    int attributes = myDimensions * myCubes;
    if (!ArrayUtil.equals(myAttributes, 0, attributes, that.myAttributes, 0, attributes))
      return false;
    // todo strictly saying, myValuesIndex and myValues must not be array-equal; we can check that actual values set for an attribute are equal
    int indexes = attributes * 4;
    if (!ArrayUtil.equals(myValuesIndex, 0, indexes, that.myValuesIndex, 0, indexes))
      return false;
    if (!ArrayUtil.equals(myValues, 0, myValuesCount, that.myValues, 0, that.myValuesCount))
      return false;
    return true;
  }

  void dump() {
    Log.debug("=== D-" + myDimensions + " {");
    Log.debug("===    cubes: " + myCubes);
    for (int cubeIndex = 0; cubeIndex < myCubes; cubeIndex++) {
      StringBuffer spec = new StringBuffer();
      for (int attributeIndex = 0; attributeIndex < myDimensions; attributeIndex++) {
        if (attributeIndex != 0)
          spec.append("; ");
        int offset = cubeIndex * myDimensions + attributeIndex;
        String attr = myAttributes[offset];
        if (attr != null) {
          spec.append(attr);
        }
        int offset4 = offset * 4;
        dumpValues(spec, offset4, '+');
        dumpValues(spec, offset4 + 2, '-');
      }
      Log.debug("===    " + cubeIndex + ": " + spec.toString());
    }
    Log.debug("=== }");
  }

  private void dumpValues(StringBuffer spec, int offset, char sign) {
    int pos = myValuesIndex[offset];
    int count = myValuesIndex[offset + 1];
    if (pos != -1 && count > 0) {
      spec.append(" ").append(sign);
      for (int k = 0; k < count; k++) {
        if (k != 0)
          spec.append(',');
        spec.append(myValues[pos + k]);
      }
    }
  }
}
