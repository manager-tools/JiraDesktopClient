package util.external;

import com.almworks.util.tests.BaseTestCase;

import java.io.*;

public class CompactIntTests extends BaseTestCase {
  public void testRegressionErrors() throws IOException {
    checkLong(54467607016767488L);
    checkLong(-31558217100705796L);
  }

  private void checkLong(long val) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    CompactInt.writeLong(out, val);
    out.close();
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    DataInputStream in = new DataInputStream(bais);
    long val2 = CompactInt.readLong(in);
    assertEquals(val, val2);
  }
}
