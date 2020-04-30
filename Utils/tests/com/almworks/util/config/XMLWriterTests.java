package com.almworks.util.config;

import com.almworks.util.BadFormatException;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Failure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author dyoma
 */
public class XMLWriterTests extends BaseTestCase {
  public static final String RUS; //russian ABV

  static {
    try {
      RUS = new String(new byte[]{-48, -80, -48, -79, -48, -78}, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new Failure(e);
    }
  }

  private final ByteArrayOutputStream myStream = new ByteArrayOutputStream();
  private final XMLWriter myWriter = new XMLWriter(myStream);

  public void testOpenCloseTag() throws IOException {
    myWriter.openTag("tagName");
    myWriter.newLine();
    checkContentAndReset("<tagName>\n");
    myWriter.writeTag("x", myWriter.encodeString("a=\"2\""));
    checkContentAndReset("  <x a=\"2\"/>\n");
    myWriter.openTag("x");
    myWriter.closeTag();
    checkContentAndReset("  <x></x>\n");
    myWriter.openTag("x");
    myWriter.newLine();
    myWriter.closeTag();
    checkContentAndReset("  <x>\n  </x>\n");
    myWriter.closeTag();
    checkContentAndReset("</tagName>\n");
  }

  public void testTagNameSymbols() throws IOException {
    myWriter.openTag("1");
    myWriter.closeTag();
    checkContentAndReset("<_1></_1>\n");
    myWriter.openTag("!");
    myWriter.closeTag();
    checkContentAndReset("<__21></__21>\n");
    myWriter.openTag("_");
    myWriter.closeTag();
    checkContentAndReset("<__5f></__5f>\n");
  }

  public void testTextNationalSymbols() throws IOException, ReadonlyConfiguration.NoSettingException, BadFormatException {
    myWriter.appendText("<>&");
    checkContentAndReset("&lt;&gt;&amp;");
    myWriter.appendText(RUS);
    checkXMLTextAndReset(RUS);
    myWriter.appendText("");
    checkContentAndReset("");
  }

  public void testTextNationalSymbolsAndXMLSpecialSymbols() throws IOException, ReadonlyConfiguration.NoSettingException,
    BadFormatException
  {
    myWriter.appendText("<" + RUS + "&" + RUS + ">");
    checkXMLTextAndReset("<" + RUS + "&" + RUS + ">");
    myWriter.appendText("<" + RUS);
    checkXMLTextAndReset("<" + RUS);
  }

  public void testNameForebiddenUpperUnicodeSymbol() throws IOException {
    myWriter.openTag("\u2011");
    checkContentAndReset("<__L2011>");
  }

  private void checkContentAndReset(String expectedContent) throws UnsupportedEncodingException {
    assertEquals(expectedContent, new String(myStream.toByteArray(), "UTF-8"));
    myStream.reset();
  }

  private void checkXMLTextAndReset(String expectedText)
    throws IOException, ReadonlyConfiguration.NoSettingException, BadFormatException
  {
    String str = new String(myStream.toByteArray(), "UTF-8");
    myStream.reset();
    String parsed = JDOMConfigurator.parse("<r><v>" + str + "</v></r>").getMandatorySetting("v");
    assertEquals(expectedText, parsed);
  }
}
