package com.almworks.util.files;

import com.almworks.util.io.BufferedDataInput;
import com.almworks.util.io.InputPump;
import com.almworks.util.tests.BaseTestCase;

import java.io.*;

public class PumpsTests extends BaseTestCase {
  private final static int BUFFER_SIZE = 20;

  private File tempFile;
  private FileInputStream myInputStream;

  protected void setUp() throws Exception {
    super.setUp();
    tempFile = File.createTempFile("pumps", "tests");
    myInputStream = null;
  }

  protected void tearDown() throws Exception {
    if (myInputStream != null) {
      try {
        myInputStream.close();
      } catch (IOException e) {
      }
      myInputStream = null;
    }
    tempFile.delete();
    tempFile.deleteOnExit();
    super.tearDown();
  }

  public void testInputPosition() throws IOException {
    BufferedDataInput input = getHelloPump(1000);
    assertEquals(0, input.getPosition());
    input.readByte();
    assertEquals(1, input.getPosition());
    input.readFully(new byte[10]);
    assertEquals(11, input.getPosition());
    int n = input.skipBytes(10);
    assertTrue(n > 0);
    assertEquals(11 + n, input.getPosition());
  }

  public void testInputPumping() throws IOException {
    BufferedDataInput input = getHelloPump(1000);
    byte[] b = new byte[BUFFER_SIZE];
    input.readFully(b);
    assertEquals("HELLOHELLOHELLOHELLO", new String(b));
    input.readFully(b);
    assertEquals("HELLOHELLOHELLOHELLO", new String(b));
    b = new byte[BUFFER_SIZE + 6];
    input.readFully(b);
    assertEquals("HELLOHELLOHELLOHELLOHELLOH", new String(b));
    input.readInt(); // whatever to skip 4 bytes
    myInputStream.close();
  }

  public void testDiscarding() throws IOException {
    BufferedDataInput input = getHelloPump(1000);
    assertEquals(BUFFER_SIZE, input.getBuffer().capacity());
    assertEquals(0, input.getBuffer().remaining());
    input.readLong();
    assertEquals(8, input.getBuffer().remaining());
    input.readFully(new byte[BUFFER_SIZE]);
    assertEquals(8 + BUFFER_SIZE, input.getBuffer().remaining());
    input.readFully(new byte[BUFFER_SIZE]);
    assertEquals(8 + 2 * BUFFER_SIZE, input.getBuffer().remaining());
    input.discard();
    assertEquals(0, input.getBuffer().remaining());
  }

  public void testInputEOF() throws IOException {
    BufferedDataInput input = getHelloPump(2);
    byte[] b = new byte[BUFFER_SIZE];
    try {
      input.readFully(b);
      fail("successfully read inexistent data");
    } catch (EOFException e) {
      // normal
    }
    //assertEquals("HELLOHELLO", new String(b));
    input = createInputPump();
    int i = 0;
    b = new byte[20];
    try {
      while (true) {
        b[i] = input.readByte();
        i++;
      }
    } catch (EOFException e) {
    }
    assertEquals("HELLOHELLO", new String(b, 0, i));

    input = createInputPump();
    input.readFully(b, 0, 10);
    assertEquals("HELLOHELLO", new String(b, 0, i));

    try {
      input.readByte();
      fail("successfully read byte behind eof");
    } catch (EOFException e) {
      // normal
    }
  }


  private BufferedDataInput getHelloPump(int count) throws IOException {
    createHello(count);
    return createInputPump();
  }

  private BufferedDataInput createInputPump() throws FileNotFoundException {
    myInputStream = new FileInputStream(tempFile);
    InputPump pump = new InputPump(myInputStream.getChannel(), BUFFER_SIZE, 0);
    BufferedDataInput input = pump;
    return input;
  }

  private void createHello(int count) throws IOException {
    FileOutputStream fos = new FileOutputStream(tempFile);
    PrintStream printStream = new PrintStream(fos);
    for (int i = 0; i < count; i++) {
      printStream.print("HELLO");
    }
    fos.close();
  }
}
