package util.external;

import java.io.DataInput;
import java.io.IOException;

public class UnmodifiableBitSet2 extends BitSet2 {
  public UnmodifiableBitSet2(BitSet2 copyFrom) {
    super(copyFrom);
  }

  public void flip(int bitIndex) {
    throw new UnsupportedOperationException();
  }

  public void flip(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  public boolean set(int bitIndex) {
    throw new UnsupportedOperationException();
  }

  public boolean set(int bitIndex, boolean value) {
    throw new UnsupportedOperationException();
  }

  public void set(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  public void set(int fromIndex, int toIndex, boolean value) {
    throw new UnsupportedOperationException();
  }

  public boolean clear(int bitIndex) {
    throw new UnsupportedOperationException();
  }

  public void clear(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public BitSet2 and(BitSet2 set) {
    throw new UnsupportedOperationException();
  }

  public BitSet2 or(BitSet2 set) {
    throw new UnsupportedOperationException();
  }

  public void xor(BitSet2 set) {
    throw new UnsupportedOperationException();
  }

  public void andNot(BitSet2 set) {
    throw new UnsupportedOperationException();
  }

  public void readFrom(DataInput in) throws IOException {
    throw new UnsupportedOperationException();
  }

  public BitSet2 modifiable() {
    return modifiableCopy();
  }
}
