package com.almworks.util.text;

import com.almworks.util.tests.BaseTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SmartDateParserTests extends BaseTestCase {
  public void testInvalidTimeZone() {
    // #582
    String format = "yyyy-MM-dd HH:mm";
    SmartDateParser parser = new SmartDateParser(format + " z").setIgnoreInvalidTimezones(true);
    Date date = parser.parse("2007-07-19 20:01 zona", null, TimeZone.getDefault());
    assertNotNull(date);
    assertEquals("2007-07-19 20:01", new SimpleDateFormat(format).format(date));
  }

  public void testAsianTime() throws ParseException {
    // #613
    String s = "Thu, 5 Jul 2007 07:32:41 +0800 (CST)";
    SmartDateParser parser = new SmartDateParser("EEE, d MMM yyyy HH:mm:ss Z (z)");
    Date date = parser.parse(s, null, TimeZone.getDefault());
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    assertEquals("2007-07-04 23:32:41", format.format(date));
  }

  public void testIsraeliTime() {
    // #788
    String s = "Tue, 1 Jan 2008 18:10:01 +0200 (IST)";
    SmartDateParser parser = new SmartDateParser("EEE, d MMM yyyy HH:mm:ss Z (z)");
    Date date = parser.parse(s, null, TimeZone.getDefault());
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    assertEquals("2008-01-01 16:10:01", format.format(date));
  }

//  public void testAustralianTime() {
//    // #788
//    String s = "Tue, 4 Dec 2007 17:33:58 +1100 (EST)";
//    SmartDateParser parser = new SmartDateParser("EEE, d MMM yyyy HH:mm:ss Z (z)");
//    Date date = parser.parse(s, null, TimeZone.getDefault());
//    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    format.setTimeZone(TimeZone.getTimeZone("GMT"));
//    assertEquals("2007-12-04 06:33:58", format.format(date));
//  }
}
