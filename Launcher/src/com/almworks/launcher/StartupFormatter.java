package com.almworks.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class StartupFormatter extends Formatter  {
  private static final SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  public String format(LogRecord record) {
    StringBuffer result = new StringBuffer(200);
    // Minimize memory allocations here.
    result.append("[*] ");
    result.append(myDateFormat.format(record.getMillis()));
    result.append(' ');
    result.append(record.getLevel().getName());
    result.append(' ');
    String message = formatMessage(record);
    result.append(message);
    result.append(LINE_SEPARATOR);
    Throwable thrown = record.getThrown();
    if (thrown != null) {
      StringWriter writer = new StringWriter();
      PrintWriter out = new PrintWriter(writer);
      thrown.printStackTrace(out);
      out.close();
      result.append(writer.toString());
    }
    return result.toString();
  }
}
