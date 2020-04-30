package util.external;

import org.almworks.util.Const;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * This class implements a vector of bits that grows as needed. Each
 * component of the bit set has a <code>boolean</code> value. The
 * bits of a <code>BitSet2</code> are indexed by nonnegative integers.
 * Individual indexed bits can be examined, set, or cleared. One
 * <code>BitSet2</code> may be used to modify the contents of another
 * <code>BitSet2</code> through logical AND, logical inclusive OR, and
 * logical exclusive OR operations.
 * <p/>
 * By default, all bits in the set initially have the value
 * <code>false</code>.
 * <p/>
 * Every bit set has a current size, which is the number of bits
 * of space currently in use by the bit set. Note that the size is
 * related to the implementation of a bit set, so it may change with
 * implementation. The length of a bit set relates to logical length
 * of a bit set and is defined independently of implementation.
 * <p/>
 * Unless otherwise noted, passing a null parameter to any of the
 * methods in a <code>BitSet2</code> will result in a
 * <code>NullPointerException</code>.
 * <p/>
 * A <code>BitSet2</code> is not safe for multithreaded use without
 * external synchronization.
 *
 * @author Arthur van Hoff
 * @author Michael McCloskey
 * @version 1.60, 02/19/04
 * @since JDK1.0
 */
public class BitSet2 implements Cloneable, java.io.Serializable {
  /*
   * BitSets are packed into arrays of "units."  Currently a unit is a long,
   * which consists of 64 bits, requiring 6 address bits.  The choice of unit
   * is determined purely by performance concerns.
   */
  private final static int ADDRESS_BITS_PER_UNIT = 6;
  private final static int BITS_PER_UNIT = 1 << ADDRESS_BITS_PER_UNIT;
  private final static int BIT_INDEX_MASK = BITS_PER_UNIT - 1;

  /* Used to shift left or right for a partial word mask */
  private static final long WORD_MASK = 0xffffffffffffffffL;


  private static byte[] WORD_BIT_COUNTS;

  /**
   * The bits in this BitSet2.  The ith bit is stored in bits[i/64] at
   * bit position i % 64 (where bit position 0 refers to the least
   * significant bit and 63 refers to the most significant bit).
   * INVARIANT: The words in bits[] above unitsInUse-1 are zero.
   *
   * @serial
   */
  private long bits[];  // this should be called unit[]

  /**
   * The number of units in the logical size of this BitSet2.
   * INVARIANT: unitsInUse is nonnegative.
   * INVARIANT: bits[unitsInUse-1] is nonzero unless unitsInUse is zero.
   */
  private transient int unitsInUse = 0;

  /* use serialVersionUID from JDK 1.0.2 for interoperability */
  private static final long serialVersionUID = 6997698588986878754L;

  /**
   * Given a bit index return unit index containing it.
   */
  private static int unitIndex(int bitIndex) {
    return bitIndex >> ADDRESS_BITS_PER_UNIT;
  }

  /**
   * Given a bit index, return a unit that masks that bit in its unit.
   */
  private static long bit(int bitIndex) {
    return 1L << (bitIndex & BIT_INDEX_MASK);
  }

  /**
   * Set the field unitsInUse with the logical size in units of the bit
   * set.  WARNING:This function assumes that the number of units actually
   * in use is less than or equal to the current value of unitsInUse!
   */
  private void recalculateUnitsInUse() {
    // Traverse the bitset until a used unit is found
    int i;
    for (i = unitsInUse - 1; i >= 0; i--)
      if (bits[i] != 0)
        break;

    unitsInUse = i + 1; // The new logical size
  }

  /**
   * Creates a new bit set. All bits are initially <code>false</code>.
   */
  public BitSet2() {
    this(BITS_PER_UNIT);
  }

  /**
   * Creates a bit set whose initial size is large enough to explicitly
   * represent bits with indices in the range <code>0</code> through
   * <code>nbits-1</code>. All bits are initially <code>false</code>.
   *
   * @param nbits the initial size of the bit set.
   * @throws NegativeArraySizeException if the specified initial size
   *                                    is negative.
   */
  public BitSet2(int nbits) {
    // nbits can't be negative; size 0 is OK
    if (nbits < 0)
      throw new NegativeArraySizeException("nbits < 0: " + nbits);

    bits = new long[(unitIndex(nbits - 1) + 1)];
  }

  public BitSet2(BitSet2 copyFrom) {
    unitsInUse = copyFrom.unitsInUse;
    bits = new long[copyFrom.bits.length];
    System.arraycopy(copyFrom.bits, 0, bits, 0, unitsInUse);
  }

  /**
   * Ensures that the BitSet2 can hold enough units.
   *
   * @param	unitsRequired the minimum acceptable number of units.
   */
  private void ensureCapacity(int unitsRequired) {
    if (bits.length < unitsRequired) {
      // Allocate larger of doubled size or required size
      int request = Math.max(2 * bits.length, unitsRequired);
      long newBits[] = new long[request];
      System.arraycopy(bits, 0, newBits, 0, unitsInUse);
      bits = newBits;
    }
  }

  /**
   * Sets the bit at the specified index to the complement of its
   * current value.
   *
   * @param bitIndex the index of the bit to flip.
   * @throws IndexOutOfBoundsException if the specified index is negative.
   * @since 1.4
   */
  public void flip(int bitIndex) {
    if (bitIndex < 0)
      throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

    int unitIndex = unitIndex(bitIndex);
    int unitsRequired = unitIndex + 1;

    if (unitsInUse < unitsRequired) {
      ensureCapacity(unitsRequired);
      bits[unitIndex] ^= bit(bitIndex);
      unitsInUse = unitsRequired;
    } else {
      bits[unitIndex] ^= bit(bitIndex);
      if (bits[unitsInUse - 1] == 0)
        recalculateUnitsInUse();
    }
  }

  /**
   * Sets each bit from the specified fromIndex(inclusive) to the
   * specified toIndex(exclusive) to the complement of its current
   * value.
   *
   * @param fromIndex index of the first bit to flip.
   * @param toIndex   index after the last bit to flip.
   * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
   *                                   or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
   *                                   larger than <tt>toIndex</tt>.
   * @since 1.4
   */
  public void flip(int fromIndex, int toIndex) {
    if (fromIndex < 0)
      throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
    if (toIndex < 0)
      throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
    if (fromIndex > toIndex)
      throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
        " > toIndex: " + toIndex);

    // Increase capacity if necessary
    int endUnitIndex = unitIndex(toIndex);
    int unitsRequired = endUnitIndex + 1;

    if (unitsInUse < unitsRequired) {
      ensureCapacity(unitsRequired);
      unitsInUse = unitsRequired;
    }

    int startUnitIndex = unitIndex(fromIndex);
    long bitMask = 0;
    if (startUnitIndex == endUnitIndex) {
      // Case 1: One word
      bitMask = (1L << (toIndex & BIT_INDEX_MASK)) -
        (1L << (fromIndex & BIT_INDEX_MASK));
      bits[startUnitIndex] ^= bitMask;
      if (bits[unitsInUse - 1] == 0)
        recalculateUnitsInUse();
      return;
    }

    // Case 2: Multiple words
    // Handle first word
    bitMask = bitsLeftOf(fromIndex & BIT_INDEX_MASK);
    bits[startUnitIndex] ^= bitMask;

    // Handle intermediate words, if any
    if (endUnitIndex - startUnitIndex > 1) {
      for (int i = startUnitIndex + 1; i < endUnitIndex; i++)
        bits[i] ^= WORD_MASK;
    }

    // Handle last word
    bitMask = bitsRightOf(toIndex & BIT_INDEX_MASK);
    bits[endUnitIndex] ^= bitMask;

    // Check to see if we reduced size
    if (bits[unitsInUse - 1] == 0)
      recalculateUnitsInUse();
  }

  /**
   * Returns a long that has all bits that are less significant
   * than the specified index set to 1. All other bits are 0.
   */
  private static long bitsRightOf(int x) {
    return (x == 0 ? 0 : WORD_MASK >>> (64 - x));
  }

  /**
   * Returns a long that has all the bits that are more significant
   * than or equal to the specified index set to 1. All other bits are 0.
   */
  private static long bitsLeftOf(int x) {
    return WORD_MASK << x;
  }

  /**
   * Sets the bit at the specified index to <code>true</code>.
   *
   * @param bitIndex a bit index.
   * @return :IS: true if the bit was changed
   * @throws IndexOutOfBoundsException if the specified index is negative.
   * @since JDK1.0
   */
  public boolean set(int bitIndex) {
    if (bitIndex < 0)
      throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

    int unitIndex = unitIndex(bitIndex);
    int unitsRequired = unitIndex + 1;

    if (unitsInUse < unitsRequired) {
      ensureCapacity(unitsRequired);
      bits[unitIndex] |= bit(bitIndex);
      unitsInUse = unitsRequired;
      return true;
    } else {
      long bit = bit(bitIndex);
      boolean unset = (bits[unitIndex] & bit) == 0;
      bits[unitIndex] |= bit;
      return unset;
    }
  }

  /**
   * Sets the bit at the specified index to the specified value.
   *
   * @return :IS: true if the bit was changed
   * @param bitIndex a bit index.
   * @param value    a boolean value to set.
   * @throws IndexOutOfBoundsException if the specified index is negative.
   * @since 1.4
   */
  public boolean set(int bitIndex, boolean value) {
    if (value)
      return set(bitIndex);
    else
      return clear(bitIndex);
  }

  /**
   * Sets the bits from the specified fromIndex(inclusive) to the
   * specified toIndex(exclusive) to <code>true</code>.
   *
   * @param fromIndex index of the first bit to be set.
   * @param toIndex   index after the last bit to be set.
   * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
   *                                   or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
   *                                   larger than <tt>toIndex</tt>.
   * @since 1.4
   */
  public void set(int fromIndex, int toIndex) {
    if (fromIndex < 0)
      throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
    if (toIndex < 0)
      throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
    if (fromIndex > toIndex)
      throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
        " > toIndex: " + toIndex);

    // Increase capacity if necessary
    int endUnitIndex = unitIndex(toIndex);
    int unitsRequired = endUnitIndex + 1;

    if (unitsInUse < unitsRequired) {
      ensureCapacity(unitsRequired);
      unitsInUse = unitsRequired;
    }

    int startUnitIndex = unitIndex(fromIndex);
    long bitMask = 0;
    if (startUnitIndex == endUnitIndex) {
      // Case 1: One word
      bitMask = (1L << (toIndex & BIT_INDEX_MASK)) -
        (1L << (fromIndex & BIT_INDEX_MASK));
      bits[startUnitIndex] |= bitMask;
      return;
    }

    // Case 2: Multiple words
    // Handle first word
    bitMask = bitsLeftOf(fromIndex & BIT_INDEX_MASK);
    bits[startUnitIndex] |= bitMask;

    // Handle intermediate words, if any
    if (endUnitIndex - startUnitIndex > 1) {
      for (int i = startUnitIndex + 1; i < endUnitIndex; i++)
        bits[i] |= WORD_MASK;
    }

    // Handle last word
    bitMask = bitsRightOf(toIndex & BIT_INDEX_MASK);
    bits[endUnitIndex] |= bitMask;
  }

  /**
   * Sets the bits from the specified fromIndex(inclusive) to the
   * specified toIndex(exclusive) to the specified value.
   *
   * @param fromIndex index of the first bit to be set.
   * @param toIndex   index after the last bit to be set
   * @param value     value to set the selected bits to
   * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
   *                                   or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
   *                                   larger than <tt>toIndex</tt>.
   * @since 1.4
   */
  public void set(int fromIndex, int toIndex, boolean value) {
    if (value)
      set(fromIndex, toIndex);
    else
      clear(fromIndex, toIndex);
  }

  /**
   * Sets the bit specified by the index to <code>false</code>.
   *
   * @param bitIndex the index of the bit to be cleared.
   * @throws IndexOutOfBoundsException if the specified index is negative.
   * @since JDK1.0
   */
  public boolean clear(int bitIndex) {
    if (bitIndex < 0)
      throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

    int unitIndex = unitIndex(bitIndex);
    if (unitIndex >= unitsInUse)
      return false;

    long bit = bit(bitIndex);
    boolean set = (bits[unitIndex] & bit(bitIndex)) != 0;
    bits[unitIndex] &= ~bit;
    if (bits[unitsInUse - 1] == 0)
      recalculateUnitsInUse();
    return set;
  }

  /**
   * Sets the bits from the specified fromIndex(inclusive) to the
   * specified toIndex(exclusive) to <code>false</code>.
   *
   * @param fromIndex index of the first bit to be cleared.
   * @param toIndex   index after the last bit to be cleared.
   * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
   *                                   or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
   *                                   larger than <tt>toIndex</tt>.
   * @since 1.4
   */
  public void clear(int fromIndex, int toIndex) {
    if (fromIndex < 0)
      throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
    if (toIndex < 0)
      throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
    if (fromIndex > toIndex)
      throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
        " > toIndex: " + toIndex);

    int startUnitIndex = unitIndex(fromIndex);
    if (startUnitIndex >= unitsInUse)
      return;
    int endUnitIndex = unitIndex(toIndex);

    long bitMask = 0;
    if (startUnitIndex == endUnitIndex) {
      // Case 1: One word
      bitMask = (1L << (toIndex & BIT_INDEX_MASK)) -
        (1L << (fromIndex & BIT_INDEX_MASK));
      bits[startUnitIndex] &= ~bitMask;
      if (bits[unitsInUse - 1] == 0)
        recalculateUnitsInUse();
      return;
    }

    // Case 2: Multiple words
    // Handle first word
    bitMask = bitsLeftOf(fromIndex & BIT_INDEX_MASK);
    bits[startUnitIndex] &= ~bitMask;

    // Handle intermediate words, if any
    if (endUnitIndex - startUnitIndex > 1) {
      for (int i = startUnitIndex + 1; i < endUnitIndex; i++) {
        if (i < unitsInUse)
          bits[i] = 0;
      }
    }

    // Handle last word
    if (endUnitIndex < unitsInUse) {
      bitMask = bitsRightOf(toIndex & BIT_INDEX_MASK);
      bits[endUnitIndex] &= ~bitMask;
    }

    if (bits[unitsInUse - 1] == 0)
      recalculateUnitsInUse();
  }

  /**
   * Sets all of the bits in this BitSet2 to <code>false</code>.
   *
   * @since 1.4
   */
  public void clear() {
    while (unitsInUse > 0)
      bits[--unitsInUse] = 0;
  }

  /**
   * Returns the value of the bit with the specified index. The value
   * is <code>true</code> if the bit with the index <code>bitIndex</code>
   * is currently set in this <code>BitSet2</code>; otherwise, the result
   * is <code>false</code>.
   *
   * @param bitIndex the bit index.
   * @return the value of the bit with the specified index.
   * @throws IndexOutOfBoundsException if the specified index is negative.
   */
  public boolean get(int bitIndex) {
    if (bitIndex < 0)
      throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

    boolean result = false;
    int unitIndex = unitIndex(bitIndex);
    if (unitIndex < unitsInUse)
      result = ((bits[unitIndex] & bit(bitIndex)) != 0);

    return result;
  }

  /**
   * Returns a new <tt>BitSet2</tt> composed of bits from this <tt>BitSet2</tt>
   * from <tt>fromIndex</tt>(inclusive) to <tt>toIndex</tt>(exclusive).
   *
   * @param fromIndex index of the first bit to include.
   * @param toIndex   index after the last bit to include.
   * @return a new <tt>BitSet2</tt> from a range of this <tt>BitSet2</tt>.
   * @throws IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
   *                                   or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
   *                                   larger than <tt>toIndex</tt>.
   * @since 1.4
   */
  public BitSet2 get(int fromIndex, int toIndex) {
    if (fromIndex < 0)
      throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
    if (toIndex < 0)
      throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
    if (fromIndex > toIndex)
      throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
        " > toIndex: " + toIndex);

    // If no set bits in range return empty bitset
    if (length() <= fromIndex || fromIndex == toIndex)
      return new BitSet2(0);

    // An optimization
    if (length() < toIndex)
      toIndex = length();

    BitSet2 result = new BitSet2(toIndex - fromIndex);
    int startBitIndex = fromIndex & BIT_INDEX_MASK;
    int endBitIndex = toIndex & BIT_INDEX_MASK;
    int targetWords = (toIndex - fromIndex + 63) / 64;
    int sourceWords = unitIndex(toIndex) - unitIndex(fromIndex) + 1;
    int inverseIndex = 64 - startBitIndex;
    int targetIndex = 0;
    int sourceIndex = unitIndex(fromIndex);

    // Process all words but the last word
    while (targetIndex < targetWords - 1)
      result.bits[targetIndex++] =
        (bits[sourceIndex++] >>> startBitIndex) |
        ((inverseIndex == 64) ? 0 : bits[sourceIndex] << inverseIndex);

    // Process the last word
    result.bits[targetIndex] = (sourceWords == targetWords ?
      (bits[sourceIndex] & bitsRightOf(endBitIndex)) >>> startBitIndex :
      (bits[sourceIndex++] >>> startBitIndex) | ((inverseIndex == 64) ? 0 :
      (getBits(sourceIndex) & bitsRightOf(endBitIndex)) << inverseIndex));

    // Set unitsInUse correctly
    result.unitsInUse = targetWords;
    result.recalculateUnitsInUse();
    return result;
  }

  /**
   * Returns the unit of this bitset at index j as if this bitset had an
   * infinite amount of storage.
   */
  private long getBits(int j) {
    return (j < unitsInUse) ? bits[j] : 0;
  }

  /**
   * Returns the index of the first bit that is set to <code>true</code>
   * that occurs on or after the specified starting index. If no such
   * bit exists then -1 is returned.
   * <p/>
   * To iterate over the <code>true</code> bits in a <code>BitSet2</code>,
   * use the following loop:
   * <p/>
   * for(int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) {
   * // operate on index i here
   * }
   *
   * @param fromIndex the index to start checking from (inclusive).
   * @return the index of the next set bit.
   * @throws IndexOutOfBoundsException if the specified index is negative.
   * @since 1.4
   */
  public int nextSetBit(int fromIndex) {
    if (fromIndex < 0)
      throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
    int u = unitIndex(fromIndex);
    if (u >= unitsInUse)
      return -1;
    int testIndex = (fromIndex & BIT_INDEX_MASK);
    long unit = bits[u] >> testIndex;

    if (unit == 0)
      testIndex = 0;

    while ((unit == 0) && (u < unitsInUse - 1))
      unit = bits[++u];

    if (unit == 0)
      return -1;

    testIndex += trailingZeroCnt(unit);
    return ((u * BITS_PER_UNIT) + testIndex);
  }

  /**
   * Added by IS, similar to nextSetBit.
   */
  public int prevSetBit(int fromIndex) {
    if (fromIndex < 0)
      return -1;

    int u = unitIndex(fromIndex);

    long unit;
    if (u >= unitsInUse) {
      unit = 0;
      u = unitsInUse;
    } else {
      int testIndex = (fromIndex & BIT_INDEX_MASK);
      if (testIndex < BIT_INDEX_MASK)
        unit = bits[u] & ((1L << (testIndex + 1)) - 1);
      else
        unit = bits[u];
    }

    while ((unit == 0) && (u > 0))
      unit = bits[--u];

    if (unit == 0)
      return -1;

    int testIndex = BITS_PER_UNIT - leadingZeroCnt(unit) - 1;
    return ((u * BITS_PER_UNIT) + testIndex);
  }

  public int prevClearBit(int fromIndex) {
    if (fromIndex < 0)
      return -1;

    int u = unitIndex(fromIndex);

    long unit;
    if (u >= unitsInUse)
      return fromIndex;
    int testIndex = (fromIndex & BIT_INDEX_MASK);
    if (testIndex < BIT_INDEX_MASK)
      unit = ~bits[u] & ((1L << (testIndex + 1)) - 1);
    else
      unit = ~bits[u];

    while ((unit == 0) && (u > 0))
      unit = ~bits[--u];
    if (unit == 0)
      return -1;
    testIndex = BITS_PER_UNIT - leadingZeroCnt(unit) - 1;
    return ((u * BITS_PER_UNIT) + testIndex);
  }

  private static int leadingZeroCnt(long val) {
    // todo maybe just comparing would be more effective

    int byteVal = (int) (val >>> 56) & 0xff;
    if (byteVal != 0)
      return leadingZeroTable[byteVal];

    byteVal = (int) (val >>> 48) & 0xff;
    if (byteVal != 0)
      return leadingZeroTable[byteVal] + 8;

    byteVal = (int) (val >>> 40) & 0xff;
    if (byteVal != 0)
      return leadingZeroTable[byteVal] + 16;

    byteVal = (int) (val >>> 32) & 0xff;
    if (byteVal != 0)
      return leadingZeroTable[byteVal] + 24;

    byteVal = (int) (val >>> 24) & 0xff;
    if (byteVal != 0)
      return leadingZeroTable[byteVal] + 32;

    byteVal = (int) (val >>> 16) & 0xff;
    if (byteVal != 0)
      return leadingZeroTable[byteVal] + 40;

    byteVal = (int) (val >>> 8) & 0xff;
    if (byteVal != 0)
      return leadingZeroTable[byteVal] + 48;

    byteVal = (int) val & 0xff;
    return leadingZeroTable[byteVal] + 56;
  }

  /**
   * :IS:
   * leadingZeroTable[i] is the number of leading zero bits in the binary
   * representation of i.
   */
  private final static byte leadingZeroTable[] = {
    -25, 7, 6, 6, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4,
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};


  private static int trailingZeroCnt(long val) {
    // Loop unrolled for performance
    int byteVal = (int) val & 0xff;
    if (byteVal != 0)
      return trailingZeroTable[byteVal];

    byteVal = (int) (val >>> 8) & 0xff;
    if (byteVal != 0)
      return trailingZeroTable[byteVal] + 8;

    byteVal = (int) (val >>> 16) & 0xff;
    if (byteVal != 0)
      return trailingZeroTable[byteVal] + 16;

    byteVal = (int) (val >>> 24) & 0xff;
    if (byteVal != 0)
      return trailingZeroTable[byteVal] + 24;

    byteVal = (int) (val >>> 32) & 0xff;
    if (byteVal != 0)
      return trailingZeroTable[byteVal] + 32;

    byteVal = (int) (val >>> 40) & 0xff;
    if (byteVal != 0)
      return trailingZeroTable[byteVal] + 40;

    byteVal = (int) (val >>> 48) & 0xff;
    if (byteVal != 0)
      return trailingZeroTable[byteVal] + 48;

    byteVal = (int) (val >>> 56) & 0xff;
    return trailingZeroTable[byteVal] + 56;
  }

  /*
   * trailingZeroTable[i] is the number of trailing zero bits in the binary
   * representation of i.
   */
  private final static byte trailingZeroTable[] = {
    -25, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    7, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0};

  /**
   * Returns the index of the first bit that is set to <code>false</code>
   * that occurs on or after the specified starting index.
   *
   * @param fromIndex the index to start checking from (inclusive).
   * @return the index of the next clear bit.
   * @throws IndexOutOfBoundsException if the specified index is negative.
   * @since 1.4
   */
  public int nextClearBit(int fromIndex) {
    if (fromIndex < 0)
      throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

    int u = unitIndex(fromIndex);
    if (u >= unitsInUse)
      return fromIndex;
    int testIndex = (fromIndex & BIT_INDEX_MASK);
    long unit = bits[u] >> testIndex;

    if (unit == (WORD_MASK >> testIndex))
      testIndex = 0;

    while ((unit == WORD_MASK) && (u < unitsInUse - 1))
      unit = bits[++u];

    if (unit == WORD_MASK)
      return length();

    if (unit == 0)
      return u * BITS_PER_UNIT + testIndex;

    testIndex += trailingZeroCnt(~unit);
    return ((u * BITS_PER_UNIT) + testIndex);
  }

  /**
   * Returns the "logical size" of this <code>BitSet2</code>: the index of
   * the highest set bit in the <code>BitSet2</code> plus one. Returns zero
   * if the <code>BitSet2</code> contains no set bits.
   *
   * @return the logical size of this <code>BitSet2</code>.
   * @since 1.2
   */
  public int length() {
    if (unitsInUse == 0)
      return 0;

    long highestUnit = bits[unitsInUse - 1];
    int highPart = (int) (highestUnit >>> 32);
    return 64 * (unitsInUse - 1) +
      (highPart == 0 ? bitLen((int) highestUnit)
      : 32 + bitLen((int) highPart));
  }

  /**
   * bitLen(val) is the number of bits in val.
   */
  private static int bitLen(int w) {
    // Binary search - decision tree (5 tests, rarely 6)
    return
      (w < 1 << 15 ?
      (w < 1 << 7 ?
      (w < 1 << 3 ?
      (w < 1 << 1 ? (w < 1 << 0 ? (w < 0 ? 32 : 0) : 1) : (w < 1 << 2 ? 2 : 3)) :
      (w < 1 << 5 ? (w < 1 << 4 ? 4 : 5) : (w < 1 << 6 ? 6 : 7))) :
      (w < 1 << 11 ?
      (w < 1 << 9 ? (w < 1 << 8 ? 8 : 9) : (w < 1 << 10 ? 10 : 11)) :
      (w < 1 << 13 ? (w < 1 << 12 ? 12 : 13) : (w < 1 << 14 ? 14 : 15)))) :
      (w < 1 << 23 ?
      (w < 1 << 19 ?
      (w < 1 << 17 ? (w < 1 << 16 ? 16 : 17) : (w < 1 << 18 ? 18 : 19)) :
      (w < 1 << 21 ? (w < 1 << 20 ? 20 : 21) : (w < 1 << 22 ? 22 : 23))) :
      (w < 1 << 27 ?
      (w < 1 << 25 ? (w < 1 << 24 ? 24 : 25) : (w < 1 << 26 ? 26 : 27)) :
      (w < 1 << 29 ? (w < 1 << 28 ? 28 : 29) : (w < 1 << 30 ? 30 : 31)))));
  }

  /**
   * Returns true if this <code>BitSet2</code> contains no bits that are set
   * to <code>true</code>.
   *
   * @return boolean indicating whether this <code>BitSet2</code> is empty.
   * @since 1.4
   */
  public boolean isEmpty() {
    return (unitsInUse == 0);
  }

  /**
   * Returns true if the specified <code>BitSet2</code> has any bits set to
   * <code>true</code> that are also set to <code>true</code> in this
   * <code>BitSet2</code>.
   *
   * @return boolean indicating whether this <code>BitSet2</code> intersects
   *         the specified <code>BitSet2</code>.
   * @param	set <code>BitSet2</code> to intersect with
   * @since 1.4
   */
  public boolean intersects(BitSet2 set) {
    for (int i = Math.min(unitsInUse, set.unitsInUse) - 1; i >= 0; i--)
      if ((bits[i] & set.bits[i]) != 0)
        return true;
    return false;
  }

  /**
   * Returns the number of bits set to <tt>true</tt> in this
   * <code>BitSet2</code>.
   *
   * @return the number of bits set to <tt>true</tt> in this
   *         <code>BitSet2</code>.
   * @since 1.4
   */
  public int cardinality() {
    int sum = 0;
    for (int i = 0; i < unitsInUse; i++)
      sum += bitCount(bits[i]);
    return sum;
  }

  /**
   * Returns the number of bits set in val.
   * For a derivation of this algorithm, see
   * "Algorithms and data structures with applications to
   * graphics and geometry", by Jurg Nievergelt and Klaus Hinrichs,
   * Prentice Hall, 1993.
   */
  public static int jdkBitCount(long val) {
    val -= (val & 0xaaaaaaaaaaaaaaaaL) >>> 1;
    val = (val & 0x3333333333333333L) + ((val >>> 2) & 0x3333333333333333L);
    val = (val + (val >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
    val += val >>> 8;
    val += val >>> 16;
    return ((int) (val) + (int) (val >>> 32)) & 0xff;
  }


  private static int bitCount(long val) {
    if (WORD_BIT_COUNTS == null) {
      WORD_BIT_COUNTS = initBitCounts();
    }
    return
      WORD_BIT_COUNTS[((int)(val)) & 0xFFFF] +
      WORD_BIT_COUNTS[((int)(val >> 16)) & 0xFFFF] +
      WORD_BIT_COUNTS[((int)(val >> 32)) & 0xFFFF] +
      WORD_BIT_COUNTS[((int)(val >> 48)) & 0xFFFF];
  }

  private static byte[] initBitCounts() {
    byte[] bytes = new byte[0x10000];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) jdkBitCount(i);
    }
    return bytes;
  }

  /**
   * Performs a logical <b>AND</b> of this target bit set with the
   * argument bit set. This bit set is modified so that each bit in it
   * has the value <code>true</code> if and only if it both initially
   * had the value <code>true</code> and the corresponding bit in the
   * bit set argument also had the value <code>true</code>.
   *
   * @param set a bit set.
   */
  public BitSet2 and(BitSet2 set) {
    if (this == set)
      return this;

    // Perform logical AND on bits in common
    int oldUnitsInUse = unitsInUse;
    unitsInUse = Math.min(unitsInUse, set.unitsInUse);
    int i;
    for (i = 0; i < unitsInUse; i++)
      bits[i] &= set.bits[i];

    // Clear out units no longer used
    for (; i < oldUnitsInUse; i++)
      bits[i] = 0;

    // Recalculate units in use if necessary
    if (unitsInUse > 0 && bits[unitsInUse - 1] == 0)
      recalculateUnitsInUse();

    return this;
  }

  /**
   * Performs a logical <b>OR</b> of this bit set with the bit set
   * argument. This bit set is modified so that a bit in it has the
   * value <code>true</code> if and only if it either already had the
   * value <code>true</code> or the corresponding bit in the bit set
   * argument has the value <code>true</code>.
   *
   * @param set a bit set.
   */
  public BitSet2 or(BitSet2 set) {
    if (this == set)
      return this;

    ensureCapacity(set.unitsInUse);

    // Perform logical OR on bits in common
    int unitsInCommon = Math.min(unitsInUse, set.unitsInUse);
    int i;
    for (i = 0; i < unitsInCommon; i++)
      bits[i] |= set.bits[i];

    // Copy any remaining bits
    for (; i < set.unitsInUse; i++)
      bits[i] = set.bits[i];

    if (unitsInUse < set.unitsInUse)
      unitsInUse = set.unitsInUse;

    return this;
  }

  /**
   * Performs a logical <b>XOR</b> of this bit set with the bit set
   * argument. This bit set is modified so that a bit in it has the
   * value <code>true</code> if and only if one of the following
   * statements holds:
   * <ul>
   * <li>The bit initially has the value <code>true</code>, and the
   * corresponding bit in the argument has the value <code>false</code>.
   * <li>The bit initially has the value <code>false</code>, and the
   * corresponding bit in the argument has the value <code>true</code>.
   * </ul>
   *
   * @param set a bit set.
   */
  public void xor(BitSet2 set) {
    int unitsInCommon;

    if (unitsInUse >= set.unitsInUse) {
      unitsInCommon = set.unitsInUse;
    } else {
      unitsInCommon = unitsInUse;
      int newUnitsInUse = set.unitsInUse;
      ensureCapacity(newUnitsInUse);
      unitsInUse = newUnitsInUse;
    }

    // Perform logical XOR on bits in common
    int i;
    for (i = 0; i < unitsInCommon; i++)
      bits[i] ^= set.bits[i];

    // Copy any remaining bits
    for (; i < set.unitsInUse; i++)
      bits[i] = set.bits[i];

    recalculateUnitsInUse();
  }

  /**
   * Clears all of the bits in this <code>BitSet2</code> whose corresponding
   * bit is set in the specified <code>BitSet2</code>.
   *
   * @param set the <code>BitSet2</code> with which to mask this
   *            <code>BitSet2</code>.
   * @since JDK1.2
   */
  public void andNot(BitSet2 set) {
    int unitsInCommon = Math.min(unitsInUse, set.unitsInUse);

    // Perform logical (a & !b) on bits in common
    for (int i = 0; i < unitsInCommon; i++) {
      bits[i] &= ~set.bits[i];
    }

    recalculateUnitsInUse();
  }

  /**
   * Returns a hash code value for this bit set. The has code
   * depends only on which bits have been set within this
   * <code>BitSet2</code>. The algorithm used to compute it may
   * be described as follows.<p>
   * Suppose the bits in the <code>BitSet2</code> were to be stored
   * in an array of <code>long</code> integers called, say,
   * <code>bits</code>, in such a manner that bit <code>k</code> is
   * set in the <code>BitSet2</code> (for nonnegative values of
   * <code>k</code>) if and only if the expression
   * <pre>((k&gt;&gt;6) &lt; bits.length) && ((bits[k&gt;&gt;6] & (1L &lt;&lt; (bit & 0x3F))) != 0)</pre>
   * is true. Then the following definition of the <code>hashCode</code>
   * method would be a correct implementation of the actual algorithm:
   * <pre>
   * public int hashCode() {
   *      long h = 1234;
   *      for (int i = bits.length; --i &gt;= 0; ) {
   *           h ^= bits[i] * (i + 1);
   *      }
   *      return (int)((h &gt;&gt; 32) ^ h);
   * }</pre>
   * Note that the hash code values change if the set of bits is altered.
   * <p>Overrides the <code>hashCode</code> method of <code>Object</code>.
   *
   * @return a hash code value for this bit set.
   */
  public int hashCode() {
    long h = 1234;
    for (int i = bits.length; --i >= 0;)
      h ^= bits[i] * (i + 1);

    return (int) ((h >> 32) ^ h);
  }

  /**
   * Returns the number of bits of space actually in use by this
   * <code>BitSet2</code> to represent bit values.
   * The maximum element in the set is the size - 1st element.
   *
   * @return the number of bits currently in this bit set.
   */
  public int size() {
    return bits.length << ADDRESS_BITS_PER_UNIT;
  }

  /**
   * Compares this object against the specified object.
   * The result is <code>true</code> if and only if the argument is
   * not <code>null</code> and is a <code>Bitset</code> object that has
   * exactly the same set of bits set to <code>true</code> as this bit
   * set. That is, for every nonnegative <code>int</code> index <code>k</code>,
   * <pre>((BitSet2)obj).access(k) == this.access(k)</pre>
   * must be true. The current sizes of the two bit sets are not compared.
   * <p>Overrides the <code>equals</code> method of <code>Object</code>.
   *
   * @param obj the object to compare with.
   * @return <code>true</code> if the objects are the same;
   *         <code>false</code> otherwise.
   * @see BitSet2#size()
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof BitSet2))
      return false;
    if (this == obj)
      return true;

    BitSet2 set = (BitSet2) obj;
    int minUnitsInUse = Math.min(unitsInUse, set.unitsInUse);

    // Check units in use by both BitSets
    for (int i = 0; i < minUnitsInUse; i++)
      if (bits[i] != set.bits[i])
        return false;

    // Check any units in use by only one BitSet2 (must be 0 in other)
    if (unitsInUse > minUnitsInUse) {
      for (int i = minUnitsInUse; i < unitsInUse; i++)
        if (bits[i] != 0)
          return false;
    } else {
      for (int i = minUnitsInUse; i < set.unitsInUse; i++)
        if (set.bits[i] != 0)
          return false;
    }

    return true;
  }

  /**
   * Cloning this <code>BitSet2</code> produces a new <code>BitSet2</code>
   * that is equal to it.
   * The clone of the bit set is another bit set that has exactly the
   * same bits set to <code>true</code> as this bit set and the same
   * current size.
   * <p>Overrides the <code>clone</code> method of <code>Object</code>.
   *
   * @return a clone of this bit set.
   * @see BitSet2#size()
   */
  public BitSet2 clone() {
    BitSet2 result = null;
    try {
      result = (BitSet2) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
    result.bits = new long[bits.length];
    System.arraycopy(bits, 0, result.bits, 0, unitsInUse);
    return result;
  }

  /**
   * This override of readObject makes sure unitsInUse is set properly
   * when deserializing a bitset
   */
  private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    // Assume maximum length then find real length
    // because recalculateUnitsInUse assumes maintenance
    // or reduction in logical size
    unitsInUse = bits.length;
    recalculateUnitsInUse();
  }

  /**
   * Returns a string representation of this bit set. For every index
   * for which this <code>BitSet2</code> contains a bit in the set
   * state, the decimal representation of that index is included in
   * the result. Such indices are listed in order from lowest to
   * highest, separated by ",&nbsp;" (a comma and a space) and
   * surrounded by braces, resulting in the usual mathematical
   * notation for a set of integers.<p>
   * Overrides the <code>toString</code> method of <code>Object</code>.
   * <p>Example:
   * <pre>
   * BitSet2 drPepper = new BitSet2();</pre>
   * Now <code>drPepper.toString()</code> returns "<code>{}</code>".<p>
   * <pre>
   * drPepper.set(2);</pre>
   * Now <code>drPepper.toString()</code> returns "<code>{2}</code>".<p>
   * <pre>
   * drPepper.set(4);
   * drPepper.set(10);</pre>
   * Now <code>drPepper.toString()</code> returns "<code>{2, 4, 10}</code>".
   *
   * @return a string representation of this bit set.
   */
  public String toString() {
    int numBits = unitsInUse << ADDRESS_BITS_PER_UNIT;
    StringBuffer buffer = new StringBuffer(8 * numBits + 2);
    String separator = "";
    buffer.append('{');

    for (int i = 0; i < numBits; i++) {
      if (get(i)) {
        buffer.append(separator);
        separator = ", ";
        buffer.append(i);
      }
    }

    buffer.append('}');
    return buffer.toString();
  }

  public void writeTo(DataOutput out) throws IOException {
    int len = bits.length;
    CompactInt.writeInt(out, len);
    for (int i = 0; i < len; i++) {
      CompactInt.writeLong(out, bits[i]);
    }
  }

  public void readFrom(DataInput in) throws IOException {
    int length = CompactInt.readInt(in);
    if (length < 0)
      throw new IOException("L " + length);
    bits = new long[length];
    for (int i = 0; i < bits.length; i++) {
      bits[i] = CompactInt.readLong(in);
    }
    unitsInUse = bits.length;
    recalculateUnitsInUse();
  }

  // for debug
  long[] bits() {
    return (long[]) bits.clone();
  }

  public static int intersectionCardinality(BitSet2 set1, BitSet2 set2) {
    if (set1 == null || set2 == null)
      return 0;
    if (set1 == set2)
      return set1.cardinality();

    int sum = 0;

    // Perform logical AND on bits in common
    int units = Math.min(set1.unitsInUse, set2.unitsInUse);
    int i;
    for (i = 0; i < units; i++) {
      long bits = set1.bits[i] & set2.bits[i];
      sum += bitCount(bits);
    }

    return sum;
  }

  public static int intersectionCardinality(BitSet2 set1, BitSet2 set2, BitSet2 set3) {
    if (set1 == null || set2 == null || set3 == null)
      return 0;

    int sum = 0;

    // Perform logical AND on bits in common
    int units = Math.min(Math.min(set1.unitsInUse, set2.unitsInUse), set3.unitsInUse);
    int i;
    for (i = 0; i < units; i++) {
      long bits = set1.bits[i] & set2.bits[i] & set3.bits[i];
      sum += bitCount(bits);
    }
    return sum;
  }

  /**
   * Returns array of all indexes set to 1, in increasing order
   */
  public int[] getIndexesSet() {
    int cardinality = cardinality();
    if (cardinality == 0)
      return Const.EMPTY_INTS;
    int[] result = new int[cardinality];
    int i = 0;
    for (int index = nextSetBit(0); index >= 0; index = nextSetBit(index + 1)) {
      result[i++] = index;
    }
    assert i == cardinality;
    return result;
  }

  public BitSet2 unmodifiableCopy() {
    return new UnmodifiableBitSet2(this);
  }

  public BitSet2 modifiableCopy() {
    return new BitSet2(this);
  }

  /**
   * Returns this bit set if it is modifiable, or its modifiable copy.
   * @return
   */
  public BitSet2 modifiable() {
    return this;
  }

  public long getWord(int index) {
    return (bits != null && index >= 0 && index < bits.length) ? bits[index] : 0;
  }

  public void setWord(int index, long word) {
    if (index < 0)
      throw new ArrayIndexOutOfBoundsException();
    ensureCapacity(index + 1);
    bits[index] = word;
    if (unitsInUse <= index + 1) {
      unitsInUse = index + 1;
      recalculateUnitsInUse();
    }
  }

  public void shiftRight(int offset, int count) {
    // todo effectively
    if (count <= 0) return;
    for (int i = size() - 1; i >= offset; i--) {
      set(i + count, get(i));
    }
    for (int i = 0; i < count; i++) {
      clear(offset + i);
    }
  }

  public void shiftLeft(int offset, int count) {
    // todo effectively
    if (count <= 0)
      return;
    int sz = size();
    for (int i = offset; i < sz - count; i++) {
      set(i, get(i + count));
    }
    for (int i = sz - count; i < sz; i++) {
      clear(i);
    }
  }
}
