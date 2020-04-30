package com.almworks.util.io;

import com.almworks.util.tests.BaseTestCase;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import java.io.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StringTransferTests extends BaseTestCase {
  public static final String[] TEST_CHARSETS = {
    "ISO-8859-1", "UTF-8", "KOI8-R", "WINDOWS-1251", "UTF-16",
  };

  public String doTest(String string, String charset) throws IOException {
    InputStream in = new ByteArrayInputStream(string.getBytes(charset));
    String result = IOUtils.transferToString(in, charset);
    assertEquals(string, result);
    return result;
  }

  public void doTest(String string) throws IOException {
    for (int i = 0; i < TEST_CHARSETS.length; i++) {
      doTest(string, TEST_CHARSETS[i]);
    }
  }

  public void testSimple() throws IOException {
    doTest("");
    doTest("A");
    doTest("ABABABABABABABA");
    doTest(
      "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789");
  }

  public void testBigData() throws IOException {
    testData(IOUtils.BLOCK_SIZE * 4, 3);
    testData(IOUtils.BLOCK_SIZE * 4 + 1, 5);
    testData(IOUtils.BLOCK_SIZE * 4 - 1, 7);
  }

  private void testData(int count, int seed) throws IOException {
    byte[] data = createTestData(count, seed);
    String a = new String(data, "ISO-8859-1");
    String b = IOUtils.transferToString(new ByteArrayInputStream(data), "ISO-8859-1");
    assertEquals(a, b);
  }

  public void testInvalid() throws IOException {
    final String CHARSET = "ISO-8859-1";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write("A".getBytes(CHARSET));
    baos.write(3);
    baos.write("B".getBytes(CHARSET));
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    String result = IOUtils.transferToString(bais, CHARSET, XMLCharValidator.INSTANCE);
    assertEquals("A?B", result);
  }

  public void testBadXml() throws IOException, JDOMException {
    InputStream stream = getClass().getResourceAsStream("badxml.xml");
    assert stream != null;
    String xml = IOUtils.transferToString(stream, "ISO-8859-1", XMLCharValidator.INSTANCE);
    stream.close();

    SAXBuilder builder = new SAXBuilder();
    Document document = builder.build(new InputSource(new StringReader(xml)));
    assertNotNull(document);
  }
}
