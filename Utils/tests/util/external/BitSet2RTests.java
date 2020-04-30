package util.external;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.io.*;
import java.util.List;

public class BitSet2RTests extends BaseTestCase {

  private BitSet2 set;

  protected void setUp() throws Exception {
    set = new BitSet2();
  }

  protected void tearDown() throws Exception {
    set = null;
  }

  public void testPrevIteratingBitPerUnit() {
    int[] bits = makeOneBitPerUnit();
    set(bits, true);
    int k = bits.length - 1;
    for (int i = set.prevSetBit(set.size()); i >= 0; i = set.prevSetBit(i - 1)) {
      assertEquals(bits[k], i);
      k--;
    }
  }

  public void testPrevIterating63BitsPerUnit() {
    int[] bits = makeOneBitPerUnit();
    int N = 65 * 64 + 2;
    set.set(0, N);
    set(bits, false);
    int k = bits.length - 1;
    int count = 0;
    for (int i = set.prevSetBit(set.size()); i >= 0; i = set.prevSetBit(i - 1)) {
      count++;
      while (k >= 0 && i <= bits[k]) {
        assertTrue("" + i, i != bits[k]);
        k--;
      }
    }
    assertEquals(N - bits.length, count);
    assertEquals(set.cardinality(), count);
  }

  public void testPrevIteratingAll() {
    int N = 8917;
    set.set(0, N);
    int count = 0;
    for (int i = set.prevSetBit(set.size()); i >= 0; i = set.prevSetBit(i - 1))
      count++;
    assertEquals(N, count);
    assertEquals(set.cardinality(), count);
  }


  private void set(int[] indexes, boolean value) {
    for (int i = 0; i < indexes.length; i++) {
      int index = indexes[i];
      set.set(index, value);
    }
  }


  private static int[] makeOneBitPerUnit() {
    int[] ints = new int[65];
    for (int i = 0; i < ints.length; i++)
      ints[i] = 65 * i;
    return ints;
  }


  public void testSerialization() throws IOException, ClassNotFoundException {
    InputStream stream = getClass().getClassLoader().getResourceAsStream("util/external/BitSet2.ser");
    assertNotNull(stream);
    BitSet2 bits = (BitSet2) new ObjectInputStream(stream).readObject();
    stream.close();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    bits.writeTo(out);
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    DataInputStream in = new DataInputStream(bais);
    BitSet2 bits2 = new BitSet2();
    bits2.readFrom(in);

    List<Long> u1 = getUnitList(bits);
    List<Long> u2 = getUnitList(bits2);
    new CollectionsCompare().order(u1, u2);

    List<Integer> list1 = getBitList(bits);
    List<Integer> list2 = getBitList(bits2);
    new CollectionsCompare().order(list1, list2);

    assertEquals(bits, bits2);
  }

  private List<Integer> getBitList(BitSet2 bits) {
    List<Integer> result = Collections15.arrayList();
    for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
      result.add(new Integer(i));
    }
    return result;
  }

  private List<Long> getUnitList(BitSet2 bits) {
    List<Long> result = Collections15.arrayList();
    long[] longs = bits.bits();
    for (int i = 0; i < longs.length; i++) {
      result.add(new Long(longs[i]));
    }
    return result;
  }
}
